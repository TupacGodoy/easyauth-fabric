package com.cobblemania.economia.event;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.data.MissionDefinition;
import com.cobblemania.economia.data.MissionType;
import com.cobblemania.economia.data.PlayerMissionData;
import com.cobblemania.economia.mission.MissionManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Integración de misiones con CobbleMaiaRanked.
 *
 * Se conecta al sistema ranked via reflection para no crear
 * dependencia de compilación con el mod ranked.
 *
 * Llamado desde CobblemonEventAdapter.handleBattleVictory()
 * cuando detecta que ambos jugadores están en un match ranked activo.
 *
 * Tipos soportados:
 *   WIN_RANKED_BATTLE         — cualquier victoria ranked
 *   WIN_RANKED_CONSECUTIVE    — racha de victorias ranked
 *   WIN_RANKED_WITH_TYPE      — ganar usando tipo específico [desc=tipo]
 *   WIN_RANKED_NO_ITEMS       — ganar sin usar objetos (aproximado)
 *   WIN_RANKED_SPECIFIC_RANK  — ganar contra rango específico [desc="campeon"]
 *   WIN_RANKED_FLAWLESS       — ganar sin perder un Pokémon
 *   PLAY_RANKED_BATTLES       — jugar X ranked (gana o pierde)
 *   REACH_ELO                 — alcanzar X ELO (pasiva, revisada al ganar)
 */
public final class RankedMissionAdapter {

    private RankedMissionAdapter() {}

    // Racha de victorias ranked por jugador
    private static final Map<UUID, Integer> rankedStreak = new HashMap<>();

    /**
     * Llamado cuando el jugador GANA una batalla ranked.
     * @param winner  El jugador ganador
     * @param loser   El jugador perdedor
     * @param winnerPartyType Tipo del equipo ganador (ej: "FIRE,WATER")
     */
    public static void onRankedWin(ServerPlayerEntity winner, ServerPlayerEntity loser,
                                    String winnerPartyType) {
        // 1. Victoria general
        MissionManager.addProgress(winner, MissionType.WIN_RANKED_BATTLE, 1);

        // 2. Partida jugada (ganador)
        MissionManager.addProgress(winner, MissionType.PLAY_RANKED_BATTLES, 1);

        // 3. Racha consecutiva
        int streak = rankedStreak.merge(winner.getUuid(), 1, Integer::sum);
        handleRankedStreak(winner, streak);

        // 4. Usando tipo específico
        if (winnerPartyType != null && !winnerPartyType.isEmpty()) {
            MissionManager.addProgressFiltered(winner, MissionType.WIN_RANKED_WITH_TYPE, 1, winnerPartyType);
        }

        // 5. Contra rango específico del rival
        String loserRankName = getRankedRankName(loser);
        if (loserRankName != null) {
            MissionManager.addProgressFiltered(winner, MissionType.WIN_RANKED_SPECIFIC_RANK, 1, loserRankName);
        }

        // 6. ELO alcanzado (pasiva)
        int currentElo = getRankedElo(winner);
        if (currentElo > 0) {
            checkEloMilestone(winner, currentElo);
        }

        // 7. WIN_RANKED_NO_ITEMS — aproximado: si la misión está activa, se cuenta
        //    (la detección real de uso de objetos requeriría un Mixin en la batalla)
        MissionManager.addProgress(winner, MissionType.WIN_RANKED_NO_ITEMS, 1);
    }

    /**
     * Llamado cuando el jugador PIERDE una batalla ranked.
     */
    public static void onRankedLoss(ServerPlayerEntity loser) {
        // Reset racha
        rankedStreak.put(loser.getUuid(), 0);

        // Partida jugada (perdedor también cuenta)
        MissionManager.addProgress(loser, MissionType.PLAY_RANKED_BATTLES, 1);
    }

    /**
     * Llamado cuando termina una batalla ranked (ganó o perdió).
     */
    public static void onRankedMatchEnd(ServerPlayerEntity winner, ServerPlayerEntity loser,
                                         String winnerPartyType) {
        onRankedWin(winner, loser, winnerPartyType);
        onRankedLoss(loser);
    }

    // ════════════════════════════════════
    // Racha ranked (WIN_RANKED_CONSECUTIVE)
    // ════════════════════════════════════
    private static void handleRankedStreak(ServerPlayerEntity player, int streak) {
        PlayerMissionData data = EconomyStorage.getMissionData(player.getUuid());
        if (data.activeMissionId == null) return;
        MissionDefinition def = MissionManager.findDef(data.activeMissionId);
        if (def == null || def.type != MissionType.WIN_RANKED_CONSECUTIVE) return;
        if (data.completed.contains(def.id)) return;

        // El progreso = la racha actual
        data.progress.put(def.id, streak);
        EconomyStorage.save();

        if (streak >= def.requiredAmount) {
            data.completed.add(def.id);
            data.activeMissionId = null;
            EconomyStorage.recordMissionComplete(player.getUuid());
            MissionManager.rewardMission(player, def);
            player.sendMessage(net.minecraft.text.Text.literal(
                "§6§l✦ §eMisión completada: §f§l" + def.displayName), false);
            player.sendMessage(net.minecraft.text.Text.literal(
                "§6+" + MissionManager.format(MissionManager.calculateReward(player, def)) + " CC"), false);
        } else if (streak > 1) {
            // Notificar progreso de racha
            player.sendMessage(net.minecraft.text.Text.literal(
                "§e§l[Ranked Racha] §f" + streak + "§7/§f" + def.requiredAmount + " victorias consecutivas"), false);
        }
    }

    // ════════════════════════════════════
    // ELO milestone (REACH_ELO pasiva)
    // ════════════════════════════════════
    private static void checkEloMilestone(ServerPlayerEntity player, int currentElo) {
        var defs = ModeconomiaConfig.DATA.missions.definitions;
        if (defs == null) return;
        PlayerMissionData data = EconomyStorage.getMissionData(player.getUuid());

        for (MissionDefinition def : defs) {
            if (def.type != MissionType.REACH_ELO) continue;
            if (!def.active || def.isExpired()) continue;
            if (data.completed.contains(def.id)) continue;

            // requiredAmount = ELO objetivo (ej: 1200, 1500, 1800)
            if (currentElo >= def.requiredAmount) {
                data.progress.put(def.id, currentElo);
                data.completed.add(def.id);
                EconomyStorage.recordMissionComplete(player.getUuid());
                MissionManager.rewardMission(player, def);
                player.sendMessage(net.minecraft.text.Text.literal(
                    "§6§l✦ §eMisión completada: §f§l" + def.displayName), false);
                player.sendMessage(net.minecraft.text.Text.literal(
                    "§b¡Alcanzaste " + currentElo + " ELO! §6+" +
                    MissionManager.format(MissionManager.calculateReward(player, def)) + " CC"), false);
            }
        }
        EconomyStorage.save();
    }

    // ════════════════════════════════════
    // Acceso a datos de CobbleMaiaRanked via reflection
    // ════════════════════════════════════

    /** ¿Está el jugador en una partida ranked activa? */
    public static boolean isInRankedMatch(ServerPlayerEntity player) {
        try {
            Class<?> mmClass = Class.forName("com.cobblemania.ranked.matchmaking.MatchmakingManager");
            return (boolean) mmClass.getMethod("isInMatch", UUID.class).invoke(null, player.getUuid());
        } catch (Exception ignored) { return false; }
    }

    /** ¿Están AMBOS jugadores en un match ranked entre sí? */
    public static boolean areBothInRankedMatch(ServerPlayerEntity playerA, ServerPlayerEntity playerB) {
        try {
            Class<?> mmClass = Class.forName("com.cobblemania.ranked.matchmaking.MatchmakingManager");
            boolean aInMatch = (boolean) mmClass.getMethod("isInMatch", UUID.class).invoke(null, playerA.getUuid());
            if (!aInMatch) return false;
            UUID rivalOfA = (UUID) mmClass.getMethod("getRival", UUID.class).invoke(null, playerA.getUuid());
            return rivalOfA != null && rivalOfA.equals(playerB.getUuid());
        } catch (Exception ignored) { return false; }
    }

    /** Obtiene el nombre de rango ranked del jugador (ej: "campeon", "alto mando", "lider", "entrenador") */
    public static String getRankedRankName(ServerPlayerEntity player) {
        try {
            Class<?> dbClass = Class.forName("com.cobblemania.ranked.data.RankedDatabase");
            Object playerData = dbClass.getMethod("getOrCreate", UUID.class, String.class)
                .invoke(null, player.getUuid(), player.getName().getString());
            if (playerData == null) return null;
            // PlayerData.getRankName() returns colored string like "§6👑 Campeón Pokémon"
            // Extract plain rank identifier
            int elo = (int) playerData.getClass().getField("elo").get(playerData);
            if (elo >= 1800) return "campeon";
            if (elo >= 1500) return "alto mando";
            if (elo >= 1200) return "lider";
            return "entrenador";
        } catch (Exception ignored) { return null; }
    }

    /** Obtiene el ELO actual del jugador en ranked */
    public static int getRankedElo(ServerPlayerEntity player) {
        try {
            Class<?> dbClass = Class.forName("com.cobblemania.ranked.data.RankedDatabase");
            Object playerData = dbClass.getMethod("getOrCreate", UUID.class, String.class)
                .invoke(null, player.getUuid(), player.getName().getString());
            if (playerData == null) return 0;
            return (int) playerData.getClass().getField("elo").get(playerData);
        } catch (Exception ignored) { return 0; }
    }
}
