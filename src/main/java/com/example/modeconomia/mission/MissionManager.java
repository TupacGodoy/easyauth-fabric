package com.cobblemania.economia.mission;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.data.MissionDefinition;
import com.cobblemania.economia.data.MissionType;
import com.cobblemania.economia.data.PlayerMissionData;
import com.cobblemania.economia.rank.RankManager;
import com.cobblemania.economia.data.rank.Rank;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sistema central de misiones de CobbleMania.
 *
 * Flujo de una misión:
 *   1. Admin crea la misión (tipo, nombre, descripción, objetivo, recompensa, duración)
 *   2. Jugador visita el NPC Misionero y hace click en la misión para seleccionarla
 *   3. Las acciones del jugador llaman a addProgress() o addProgressAuto()
 *   4. Al alcanzar requiredAmount → completetion automática + recompensa
 *
 * Tipos de progreso:
 *   addProgress()     — misiones MANUALES (requieren selección previa del jugador)
 *   addProgressAuto() — misiones PASIVAS (siempre cuentan, sin selección)
 *   addProgressFiltered() — misiones con filtro en 'description'
 *   handleVanillaEvent() — entrada unificada para eventos vanilla
 */
public final class MissionManager {

    private MissionManager() {}

    // ── Tracking state ──
    private static final Map<UUID, Vec3d>   LAST_POS         = new HashMap<>();
    private static final Map<UUID, Integer> PLAYTIME_SECONDS = new HashMap<>();
    private static final Map<UUID, Boolean> WAS_DAMAGED      = new HashMap<>(); // para CATCH_WITHOUT_DAMAGE
    private static final Set<String>        notifiedExpired  = new HashSet<>();

    // ════════════════════════════════════════════════════
    // LIFECYCLE
    // ════════════════════════════════════════════════════

    public static void onPlayerJoin(ServerPlayerEntity player) {
        EconomyStorage.getMissionData(player.getUuid());
        if (EconomyStorage.shouldResetMissions()) EconomyStorage.resetMissionsForAll();
        // JOIN_SERVER es pasiva — cuenta automáticamente al entrar
        addProgressAuto(player, MissionType.JOIN_SERVER, 1);
    }

    public static void tick(MinecraftServer server) {
        if (EconomyStorage.shouldResetMissions()) EconomyStorage.resetMissionsForAll();
        if (EconomyStorage.shouldResetWeekly())   EconomyStorage.resetWeeklyForAll();
        if (EconomyStorage.shouldResetMonthly())  EconomyStorage.resetMonthlyForAll();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            tickWalkMission(player);
            tickPlaytimeMission(player);
        }
        checkExpiredMissions(server);
    }

    private static void checkExpiredMissions(MinecraftServer server) {
        List<MissionDefinition> defs = ModeconomiaConfig.DATA.missions.definitions;
        if (defs == null) return;
        boolean anyExpired = false;
        for (MissionDefinition def : defs) {
            if (def.active && def.isExpired() && !notifiedExpired.contains(def.id)) {
                notifiedExpired.add(def.id);
                def.active = false;
                anyExpired = true;
                // Notificar a jugadores que tenían esta misión activa
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    PlayerMissionData pdata = EconomyStorage.getMissionData(p.getUuid());
                    if (def.id.equals(pdata.activeMissionId)) {
                        pdata.activeMissionId = null;
                        p.sendMessage(Text.literal("§c§l⚠ §cTu misión §f" + def.displayName + " §cha expirado."), false);
                    }
                }
            }
        }
        if (anyExpired) {
            ModeconomiaConfig.save();
            EconomyStorage.save();
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (p.hasPermissionLevel(4)) {
                    p.sendMessage(Text.literal(
                        "§6§l✦ §e[Admin] §7Misiones expiradas — usa §e/ccoins owner §7→ Misiones."), false);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════
    // SELECCIÓN DE MISIÓN
    // ════════════════════════════════════════════════════

    /**
     * Jugador selecciona una misión en el NPC.
     * Retorna true si fue seleccionada, false si hay error.
     */
    public static boolean selectMission(ServerPlayerEntity player, String missionId) {
        PlayerMissionData data = EconomyStorage.getMissionData(player.getUuid());
        MissionDefinition target = findDef(missionId);

        if (target == null || !target.active || target.isExpired()) {
            player.sendMessage(Text.literal("§c✘ Esa misión ya no está disponible."), false);
            return false;
        }
        if (data.completed.contains(missionId)) {
            player.sendMessage(Text.literal("§c✘ Ya completaste esa misión hoy."), false);
            return false;
        }
        if (isPassive(target.type)) {
            player.sendMessage(Text.literal(
                "§e⚠ Misión §f" + target.displayName + " §ese completa automáticamente — no necesitas seleccionarla."), false);
            return false;
        }
        // Deseleccionar la anterior si la hay
        if (data.activeMissionId != null && !data.activeMissionId.equals(missionId)) {
            MissionDefinition prev = findDef(data.activeMissionId);
            String prevName = prev != null ? prev.displayName : "anterior";
            player.sendMessage(Text.literal("§7(Misión §f" + prevName + " §7pausada — puedes volver a seleccionarla.)"), false);
        }

        data.activeMissionId = missionId;
        EconomyStorage.save();

        int current = data.progress.getOrDefault(missionId, 0);
        player.sendMessage(Text.literal("§a§l✔ §aMisión seleccionada: §f§l" + target.displayName), false);
        player.sendMessage(Text.literal(
            "§7Objetivo: §e" + current + "§7/§f" + target.requiredAmount +
            " §8| Recompensa: §6+" + formatReward(player, target) + " CC"), false);
        if (!target.description.isEmpty())
            player.sendMessage(Text.literal("§8" + target.description), false);
        return true;
    }

    // ════════════════════════════════════════════════════
    // ADDPROGRESS — MANUAL (requiere misión activa del tipo)
    // ════════════════════════════════════════════════════

    /**
     * Añade progreso a la misión ACTIVA del jugador si coincide el tipo.
     * Llamado por eventos de juego (Cobblemon, bloque roto, mob muerto, etc.)
     */
    public static void addProgress(ServerPlayerEntity player, MissionType type, int amount) {
        PlayerMissionData data = EconomyStorage.getMissionData(player.getUuid());
        if (data.activeMissionId == null) return;

        MissionDefinition def = findDef(data.activeMissionId);
        if (def == null || def.type != type) return;

        applyProgress(player, data, def, amount);
    }

    /**
     * Versión con filtro por descripción. El filtro se compara contra
     * el 'description' de la MissionDefinition.
     * Útil para tipos como CAPTURE_SPECIFIC_SPECIES, CAPTURE_SPECIFIC_TYPE, etc.
     *
     * @param filterValue El valor actual del evento (ej: "pikachu", "FIRE", "fast_ball")
     */
    public static void addProgressFiltered(ServerPlayerEntity player, MissionType type,
                                            int amount, String filterValue) {
        PlayerMissionData data = EconomyStorage.getMissionData(player.getUuid());
        if (data.activeMissionId == null) return;

        MissionDefinition def = findDef(data.activeMissionId);
        if (def == null || def.type != type) return;

        // filter1 = filtro interno guardado por el admin (ej: "FIRE", "fast_ball")
        // Si filter1 está vacío, intentar con description para retrocompatibilidad
        String required = (def.filter1 != null && !def.filter1.trim().isEmpty())
            ? def.filter1.trim()
            : def.description.trim();

        if (!required.isEmpty() && !matchesFilter(filterValue, required)) return;

        applyProgress(player, data, def, amount);
    }

    /**
     * Versión con filtro doble (tipo:ball).
     * Usa filter1 y filter2, con retrocompatibilidad a description="TIPO:BALL".
     */
    public static void addProgressDoubleFilter(ServerPlayerEntity player, MissionType type,
                                                int amount, String value1, String value2) {
        PlayerMissionData data = EconomyStorage.getMissionData(player.getUuid());
        if (data.activeMissionId == null) return;

        MissionDefinition def = findDef(data.activeMissionId);
        if (def == null || def.type != type) return;

        // Usar filter1 y filter2 separados; retrocompat con description="TIPO:BALL"
        String req1, req2;
        if (def.filter1 != null && !def.filter1.trim().isEmpty()) {
            req1 = def.filter1.trim().toLowerCase();
            req2 = (def.filter2 != null) ? def.filter2.trim().toLowerCase() : "";
        } else {
            String desc = def.description.trim();
            String[] parts = desc.split(":");
            req1 = parts.length > 0 ? parts[0].trim().toLowerCase() : "";
            req2 = parts.length > 1 ? parts[1].trim().toLowerCase() : "";
        }
        // Comparación robusta: normalizar ambos lados
        if (!req1.isEmpty() && !matchesFilter(value1, req1)) return;
        if (!req2.isEmpty() && !matchesFilter(value2, req2)) return;

        applyProgress(player, data, def, amount);
    }

    /**
     * Versión rango numérico (nivel MIN:MAX).
     * Usa filter1="MIN:MAX"; retrocompat con description="MIN:MAX".
     */
    public static void addProgressRangeFilter(ServerPlayerEntity player, MissionType type,
                                               int amount, int value) {
        PlayerMissionData data = EconomyStorage.getMissionData(player.getUuid());
        if (data.activeMissionId == null) return;

        MissionDefinition def = findDef(data.activeMissionId);
        if (def == null || def.type != type) return;

        String range = (def.filter1 != null && !def.filter1.trim().isEmpty())
            ? def.filter1.trim()
            : def.description.trim();
        if (!range.isEmpty()) {
            try {
                String[] parts = range.split(":");
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                if (value < min || value > max) return;
            } catch (Exception ignored) {}
        }

        applyProgress(player, data, def, amount);
    }

    // ════════════════════════════════════════════════════
    // ADDPROGRESS — PASIVO (cuenta siempre, sin selección)
    // ════════════════════════════════════════════════════

    /**
     * Para misiones pasivas: WALK_DISTANCE, PLAYTIME_MINUTES, JOIN_SERVER,
     * COLLECT_POKEDEX_ENTRIES.
     * Aplica a TODAS las misiones activas del tipo, no solo la seleccionada.
     */
    public static void addProgressAuto(ServerPlayerEntity player, MissionType type, int amount) {
        PlayerMissionData data = EconomyStorage.getMissionData(player.getUuid());
        List<MissionDefinition> defs = ModeconomiaConfig.DATA.missions.definitions;
        if (defs == null) return;

        for (MissionDefinition def : defs) {
            if (def.type != type) continue;
            if (!def.active || def.isExpired()) continue;
            if (data.completed.contains(def.id)) continue;
            applyProgressSilent(player, data, def, amount);
        }
        EconomyStorage.save();
    }

    // ════════════════════════════════════════════════════
    // TRACKING VANILLA
    // ════════════════════════════════════════════════════

    /** Llamado por ModeconomiaEvents cuando el jugador rompe un bloque */
    public static void onBlockBreak(ServerPlayerEntity player, String blockId) {
        addProgress(player, MissionType.BREAK_BLOCKS, 1);
        addProgressFiltered(player, MissionType.BREAK_SPECIFIC_BLOCK, 1, blockId);
    }

    /** Llamado cuando el jugador coloca un bloque */
    public static void onBlockPlace(ServerPlayerEntity player) {
        addProgress(player, MissionType.PLACE_BLOCKS, 1);
    }

    /** Llamado cuando el jugador mata un mob */
    public static void onKillMob(ServerPlayerEntity player, String entityId) {
        addProgress(player, MissionType.KILL_MOBS, 1);
        addProgressFiltered(player, MissionType.KILL_SPECIFIC_MOB, 1, entityId);
    }

    /** Llamado cuando el jugador craftea */
    public static void onCraft(ServerPlayerEntity player) {
        addProgress(player, MissionType.CRAFT_ITEMS, 1);
    }

    /** Llamado cuando el jugador pesca */
    public static void onFish(ServerPlayerEntity player) {
        addProgress(player, MissionType.FISH_ITEMS, 1);
    }

    /** Marca que el jugador recibió daño (para CATCH_WITHOUT_DAMAGE) */
    public static void onPlayerDamaged(ServerPlayerEntity player) {
        WAS_DAMAGED.put(player.getUuid(), true);
    }

    /** Llamado al inicio de una batalla — resetea el flag de daño */
    public static void onBattleStart(ServerPlayerEntity player) {
        WAS_DAMAGED.put(player.getUuid(), false);
    }

    /** ¿El jugador recibió daño desde el último onBattleStart? */
    public static boolean wasDamagedInBattle(ServerPlayerEntity player) {
        return WAS_DAMAGED.getOrDefault(player.getUuid(), false);
    }

    // ════════════════════════════════════════════════════
    // INTERNO — aplicar progreso
    // ════════════════════════════════════════════════════

    private static void applyProgress(ServerPlayerEntity player, PlayerMissionData data,
                                       MissionDefinition def, int amount) {
        if (!def.active || def.isExpired()) {
            data.activeMissionId = null;
            EconomyStorage.save();
            player.sendMessage(Text.literal(
                "§c§l⚠ §cTu misión §f" + def.displayName + " §cha expirado. Selecciona otra."), false);
            return;
        }
        if (data.completed.contains(def.id)) return;

        int current = data.progress.getOrDefault(def.id, 0);
        int next    = Math.min(def.requiredAmount, current + amount);
        data.progress.put(def.id, next);

        // ── Notificaciones de progreso ──
        sendProgressNotification(player, def, current, next);

        // ── Completado ──
        if (next >= def.requiredAmount) {
            completeMission(player, data, def);
        }
        EconomyStorage.save();
    }

    /** Versión silenciosa para pasivas (no notifica progreso intermedio) */
    private static void applyProgressSilent(ServerPlayerEntity player, PlayerMissionData data,
                                             MissionDefinition def, int amount) {
        if (!def.active || def.isExpired()) return;
        if (data.completed.contains(def.id)) return;

        int current = data.progress.getOrDefault(def.id, 0);
        int next    = Math.min(def.requiredAmount, current + amount);
        data.progress.put(def.id, next);

        if (next >= def.requiredAmount) completeMission(player, data, def);
    }

    private static void sendProgressNotification(ServerPlayerEntity player,
                                                  MissionDefinition def, int current, int next) {
        if (def.requiredAmount <= 1) return; // Para objetivos de 1 no hay progreso intermedio
        int pct     = (next * 100) / def.requiredAmount;
        int prevPct = (current * 100) / def.requiredAmount;

        // Notifica en cada incremento de 25% y también en el último paso antes de completar
        boolean milestoneHit = (pct != prevPct) && (pct == 25 || pct == 50 || pct == 75);
        boolean nearlyDone   = (next == def.requiredAmount - 1) && current < def.requiredAmount - 1;

        if (milestoneHit) {
            player.sendMessage(Text.literal(
                "§e§l[Misión " + pct + "%] §f" + def.displayName +
                " §7— §e" + next + "§7/§f" + def.requiredAmount), false);
        } else if (nearlyDone) {
            player.sendMessage(Text.literal(
                "§6§l[Misión] §f" + def.displayName + " §6— ¡Casi! §e" +
                next + "§7/§f" + def.requiredAmount), false);
        }
    }

    private static void completeMission(ServerPlayerEntity player, PlayerMissionData data,
                                         MissionDefinition def) {
        data.completed.add(def.id);
        data.activeMissionId = null;
        EconomyStorage.recordMissionComplete(player.getUuid());
        rewardMission(player, def);

        // Animación / celebración en chat
        player.sendMessage(Text.literal("§6§l━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§a§l  ✔ MISIÓN COMPLETADA: §f§l" + def.displayName), false);
        double reward = calculateReward(player, def);
        player.sendMessage(Text.literal("§6  +" + format(reward) + " CobbleCoins§e recibidos."), false);
        player.sendMessage(Text.literal("§6§l━━━━━━━━━━━━━━━━━━━━━━━━"), false);
    }

    public static void rewardMission(ServerPlayerEntity player, MissionDefinition def) {
        double reward = calculateReward(player, def);

        // Solo dar CobbleCoins físicas — el jugador las canjea con /ccoins
        // NO agregar al balance directamente para evitar duplicado
        int coinAmount = (int) Math.round(reward);
        if (coinAmount > 0) {
            com.cobblemania.economia.item.CoinRewardHelper.giveCoins(player, coinAmount);
        }
    }

    /** Recompensa base * multiplicador de rango */
    public static double calculateReward(ServerPlayerEntity player, MissionDefinition def) {
        double base = def.reward > 0 ? def.reward : ModeconomiaConfig.DATA.missions.baseReward;
        Rank rank = RankManager.getRank(player);
        Double mult = ModeconomiaConfig.DATA.missions.multipliers.get(rank);
        return mult != null ? base * mult : base;
    }

    // ════════════════════════════════════════════════════
    // TICK PASIVAS
    // ════════════════════════════════════════════════════

    private static void tickWalkMission(ServerPlayerEntity player) {
        Vec3d cur  = player.getPos();
        Vec3d last = LAST_POS.put(player.getUuid(), cur);
        if (last == null) return;
        double dist = Math.sqrt(
            Math.pow(cur.x - last.x, 2) + Math.pow(cur.z - last.z, 2));
        if (dist < 0.05) return;
        addProgressAuto(player, MissionType.WALK_DISTANCE, (int) Math.round(dist));
    }

    private static void tickPlaytimeMission(ServerPlayerEntity player) {
        int secs = PLAYTIME_SECONDS.getOrDefault(player.getUuid(), 0) + 1;
        if (secs >= 60) {
            addProgressAuto(player, MissionType.PLAYTIME_MINUTES, secs / 60);
            secs %= 60;
        }
        PLAYTIME_SECONDS.put(player.getUuid(), secs);
    }

    // ════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════

    public static MissionDefinition findDef(String id) {
        if (id == null) return null;
        List<MissionDefinition> defs = ModeconomiaConfig.DATA.missions.definitions;
        if (defs == null) return null;
        for (MissionDefinition d : defs) if (d.id.equals(id)) return d;
        return null;
    }

    /** ¿Es una misión pasiva (no requiere selección)? */
    public static boolean isPassive(MissionType type) {
        return switch (type) {
            case JOIN_SERVER, WALK_DISTANCE, PLAYTIME_MINUTES,
                 COLLECT_POKEDEX_ENTRIES, REACH_ELO -> true;
            default -> false;
        };
    }

    /** Recompensa formateada con multiplicador */
    public static String formatReward(ServerPlayerEntity player, MissionDefinition def) {
        return format(calculateReward(player, def));
    }

    public static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    /**
     * Compara un valor del evento contra el filtro guardado por el admin.
     *
     * Normalización aplicada a ambos lados:
     *   - minúsculas
     *   - sin namespace (quita "cobblemon:", "minecraft:", etc.)
     *   - sin guiones bajos ni espacios (para comparar "fastball" == "fast_ball")
     *
     * Soporte para múltiples tipos (ej: "fire flying" debe coincidir con filtro "fire").
     *
     * @param eventValue  Valor del evento (ej: "fire", "fire flying", "fast_ball")
     * @param filterValue Filtro del admin (ej: "FIRE", "fast_ball", "FAST_BALL")
     */
    public static boolean matchesFilter(String eventValue, String filterValue) {
        if (eventValue == null || filterValue == null) return false;

        // Normalizar: minúsculas, quitar namespace, quitar guiones/espacios
        String ev  = normalize(eventValue);
        String fv  = normalize(filterValue);

        if (fv.isEmpty()) return true;   // sin filtro = coincide siempre
        if (ev.isEmpty())  return false;

        // Coincidencia exacta normalizada
        if (ev.equals(fv)) return true;

        // El evento puede tener múltiples tipos separados por espacio (ej: "fire flying")
        // → dividir y verificar si alguno coincide con el filtro
        for (String part : ev.split("\\s+")) {
            if (normalize(part).equals(fv)) return true;
        }

        // Contiene (para casos como biomas "minecraft:forest" vs filtro "forest")
        if (ev.contains(fv)) return true;

        return false;
    }

    /** Normaliza un string de filtro: minúsculas, sin namespace, sin guiones/espacios */
    private static String normalize(String s) {
        if (s == null) return "";
        s = s.toLowerCase().trim();
        // Quitar namespace (ej: "cobblemon:fire" → "fire", "minecraft:stone" → "stone")
        if (s.contains(":")) s = s.substring(s.lastIndexOf(':') + 1);
        // Quitar guiones bajos y espacios para comparación flexible
        // "fast_ball" == "fastball", "FIRE TYPE" == "firetype"
        s = s.replace("_", "").replace(" ", "").replace("-", "");
        return s;
    }
}
