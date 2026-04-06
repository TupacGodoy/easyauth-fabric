package com.cobblemania.economia.gui;

import com.cobblemania.economia.afk.AfkManager;
import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.data.MissionDefinition;
import com.cobblemania.economia.item.ModItems;
import com.cobblemania.economia.data.PlayerMissionData;
import com.cobblemania.economia.data.ShopItem;
import com.cobblemania.economia.data.rank.Rank;
import com.cobblemania.economia.mission.MissionManager;
import com.cobblemania.economia.rank.RankManager;
import com.cobblemania.economia.shop.ShopManager;
import com.cobblemania.economia.util.ItemStackUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomiaScreenHandler extends GenericContainerScreenHandler {

    private final String guiType;

    public static final Map<UUID, double[]>  amountCtx     = new HashMap<>();
    public static final Map<UUID, UUID>      targetCtx     = new HashMap<>();
    public static final Map<UUID, Integer>   shopPriceCtx  = new HashMap<>();
    public static final Map<UUID, String>    detailCtx     = new HashMap<>();
    public static final Map<UUID, String>    awaitingInput = new HashMap<>();

    public EconomiaScreenHandler(int syncId, PlayerInventory playerInv,
                                  SimpleInventory inv, String guiType) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInv, inv, 3);
        this.guiType = guiType;
    }

    public EconomiaScreenHandler(int syncId, PlayerInventory playerInv,
                                  SimpleInventory inv, String guiType, int rows) {
        super(rows == 6 ? ScreenHandlerType.GENERIC_9X6
                        : rows == 4 ? ScreenHandlerType.GENERIC_9X4
                        : ScreenHandlerType.GENERIC_9X3,
              syncId, playerInv, inv, rows);
        this.guiType = guiType;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        if (slotIndex < 0) return;
        int maxSlot = switch (guiType) {
            case "player_menu", "admin_main", "shop_view", "shop_config",
                 "shop_server", "shop_players", "shop_price_edit",
                 "player_list", "player_detail", "ranks_main", "rank_members",
                 "economy_player", "missions_list", "missions_view",
                 "mission_type_picker", "custom_cats" -> 54;
            default -> 27;
        };
        if (slotIndex >= maxSlot) return;
        if (actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) return;

        sp.closeHandledScreen();
        sp.getServer().execute(() -> handleClick(sp, slotIndex, button));
    }

    private void handleClick(ServerPlayerEntity sp, int slot, int button) {
        switch (guiType) {
            case "player_menu"     -> handlePlayerMenu(sp, slot);
            case "admin_main"      -> handleAdminMain(sp, slot);
            case "economy_action"  -> handleEconomyAction(sp, slot);
            case "economy_player"  -> handleEconomyPlayer(sp, slot);
            case "economy_amount"  -> handleEconomyAmount(sp, slot, button);
            case "missions_config" -> handleMissionsConfig(sp, slot, button);
            case "missions_list"   -> handleMissionsList(sp, slot, button);
            case "afk_config"      -> handleAfkConfig(sp, slot, button);
            case "shop_view"       -> handleShopView(sp, slot);
            case "shop_server"     -> handleShopServer(sp, slot);
            case "shop_players"    -> handleShopPlayers(sp, slot, button);
            case "shop_sell"       -> handleShopSell(sp, slot, button);
            case "shop_price_edit" -> handleShopPriceEdit(sp, slot, button);
            case "shop_config"     -> handleShopConfig(sp, slot, button);
            case "mission_type_picker" -> handleMissionTypePicker(sp, slot);
            case "ranks_main"      -> handleRanksMain(sp, slot);
            case "rank_members"    -> handleRankMembers(sp, slot);
            case "npc_config"      -> handleNpcConfig(sp, slot);
            case "shop_npc_config" -> handleShopNpcConfig(sp, slot);
            case "player_list"     -> handlePlayerList(sp, slot);
            case "player_detail"   -> handlePlayerDetail(sp, slot);
            case "mission_edit"    -> handleMissionEdit(sp, slot);
            case "missions_view"   -> handleMissionsView(sp, slot);
            case "custom_cats"     -> handleCustomCategoriesAdmin(sp, slot);
        }
    }

    // ══════════════════════════════════════
    // ABRIR GUIS
    // ══════════════════════════════════════

    public static void openPlayerMenu(ServerPlayerEntity sp) {
        SimpleInventory inv = new SimpleInventory(54);
        UUID uuid = sp.getUuid();
        double balance = EconomyStorage.getBalance(uuid);
        PlayerMissionData data = EconomyStorage.getMissionData(uuid);
        Rank rank = RankManager.getRank(sp);
        String rankName = rankDisplay(rank);
        List<MissionDefinition> defs = ModeconomiaConfig.DATA.missions.definitions;
        int completedToday = data.completed.size();
        int totalToday = defs != null ? (int) defs.stream().filter(d -> d.active && !d.isExpired()).count() : 0;
        double afkMult = ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(rank, 1.0);
        double misMult = ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(rank, 1.0);

        fill(inv, Items.ORANGE_STAINED_GLASS_PANE, "§6 ");

        // Título
        item(inv, 4, Items.NETHER_STAR,
            "§6§l✦ CobbleMania — Economía §6§l✦",
            "§7Tu panel personal de economía.");

        // ── Slot 10: Perfil — quién eres y tu rango ──
        item(inv, 10, Items.PLAYER_HEAD,
            "§e§l✦ " + sp.getName().getString(),
            "§7Rango: " + rankName,
            "§8Multiplicador misiones: §a×" + MissionManager.format(misMult),
            "§8Multiplicador AFK:      §a×" + MissionManager.format(afkMult),
            "§7Tiempo AFK total: §e" + formatAfk(data.totalAfkMinutes));

        // ── Slot 13: Balance ACTUAL — lo más importante ──
        item(inv, 13, Items.GOLD_BLOCK,
            "§6§l💰 Balance: §f§l" + MissionManager.format(balance) + " §6§lCC",
            "§7Este es tu saldo actual disponible.",
            "§8Para enviar CC: §f/pay <jugador> <monto>",
            "§8Gana CC con misiones y en la Zona AFK.");

        // ── Slot 16: Estadísticas — historial ──
        item(inv, 16, Items.BOOK,
            "§b§lEstadísticas",
            "§7Misiones hoy:  §a" + completedToday + "§7/§f" + totalToday,
            "§7Semana:         §b" + data.weeklyCompleted + " misiones",
            "§7Mes:            §d" + data.monthlyCompleted + " misiones",
            "§7Total hist.:    §f" + data.totalDailyCompleted + " misiones",
            "§7CC ganados (total histórico): §6" + MissionManager.format(data.totalEarned) + " CC");

        // ── Fila 3: Acciones ──
        item(inv, 28, completedToday >= totalToday && totalToday > 0
                ? Items.LIME_CONCRETE : Items.ORANGE_CONCRETE,
            "§b§lMisiones del día §7[" + completedToday + "/" + totalToday + "]",
            completedToday >= totalToday && totalToday > 0
                ? "§a§l✔ ¡Todas las misiones completadas hoy!"
                : "§7Pendientes: §e" + (totalToday - completedToday) + " §7misiones.",
            "§8Habla con el §eNPC Misiones §8para ver el detalle.",
            "§7Semana: §b" + data.weeklyCompleted + " §8| Mes: §d" + data.monthlyCompleted);

        boolean hasTienda = ModeconomiaConfig.DATA.shopNpcUuid != null;
        item(inv, 31, Items.CHEST,
            "§d§l🏪 Tienda",
            "§7Compra items del servidor o vende los tuyos.",
            hasTienda
                ? "§e✦ §7Habla con el §b§lTienda §7para acceder."
                : "§8Tienda del Servidor: items del owner.",
            "§8Tienda de Jugadores: items de la comunidad.",
            "§7► Click para abrir la tienda.");

        item(inv, 34, Items.CLOCK,
            "§e§lZona AFK",
            "§7Usa §f/afk §7para teleportarte a la zona AFK.",
            "§8Cada §f" + ModeconomiaConfig.DATA.afk.intervalMinutes + " min §8recibes §6+"
                + MissionManager.format(ModeconomiaConfig.DATA.afk.baseReward * afkMult) + " CC",
            "§7Tu multiplicador de rango: §6×" + MissionManager.format(afkMult));

        // ── CobbleCoin físico: canjear monedas del inventario ──
        int physicalCoins = com.cobblemania.economia.item.CoinRewardHelper.countCoins(sp);
        item(inv, 37, com.cobblemania.economia.item.ModItems.COBBLE_COIN,
            "§6§l✦ CobbleCoins Físicas",
            "§7Tienes: §e" + physicalCoins + " §7moneda(s) en inventario.",
            physicalCoins > 0
                ? "§a► Click para canjear §e" + physicalCoins + " CC §aal balance."
                : "§8No tienes monedas físicas.",
            "§8Las recibes como recompensa de misiones.",
            "§81 moneda física = 1.00 CC de balance.");

        item(inv, 40, Items.BARRIER, "§c§l✘ Cerrar", "§7Cierra este menú.");

        open(sp, inv, "player_menu", 6, "§6§l✦ CobbleMania — Economía");
    }

    public static void openAdminMain(ServerPlayerEntity sp) {
        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, Items.BLACK_STAINED_GLASS_PANE, "§8 ");
        border(inv, Items.RED_STAINED_GLASS_PANE);
        item(inv, 4, Items.NETHER_STAR,
            "§4§l👑 ADMIN PANEL — CobbleMania Economía",
            "§7Solo accesible para OP nivel 4.");
        item(inv, 10, Items.EMERALD,
            "§a§l⬤ Economía",
            "§7Dar, quitar, asignar o ver balance de jugadores.",
            "§7► Click para gestionar CobbleCoins.");
        item(inv, 12, Items.WRITABLE_BOOK,
            "§b§l⬤ Misiones",
            "§7Configurar misiones diarias, recompensas y NPC.",
            "§7► Click para configurar misiones.");
        item(inv, 14, Items.CLOCK,
            "§e§l⬤ Zona AFK",
            "§7Configurar zona AFK, coordenadas y pagos.",
            "§7► Click para configurar zona AFK.");
        item(inv, 16, Items.CHEST,
            "§d§l⬤ Tienda",
            "§7Configurar items y precios de la tienda del servidor.",
            "§7► Click para configurar la tienda.");
        item(inv, 18, Items.BOOKSHELF,
            "§5§l⬤ Categorías Custom",
            "§7Crear y gestionar categorías propias en la tienda.",
            "§7► Click para gestionar categorías.");
        item(inv, 20, Items.VILLAGER_SPAWN_EGG,
            "§6§l⬤ NPC Misiones",
            "§7Spawnear o remover el aldeano de misiones.",
            "§7► Click para gestionar el NPC.");
        item(inv, 21, Items.CHEST,
            "§b§l⬤ NPC Tienda",
            "§7Spawnear o remover el aldeano de la tienda.",
            "§7► Click para gestionar el NPC.");
        item(inv, 22, Items.NAME_TAG,
            "§c§l⬤ Rangos",
            "§7Gestionar rangos de jugadores (Trainer+, Elite...)",
            "§7Afecta multiplicadores de misiones y AFK.",
            "§7► Click para gestionar rangos.");
        item(inv, 24, Items.PLAYER_HEAD,
            "§f§l⬤ Jugadores",
            "§7Ver lista de todos los jugadores.",
            "§8Balance, misiones, AFK y estadísticas.",
            "§7► Click para ver la lista.");
        item(inv, 26, Items.BARRIER, "§c§l✘ Cerrar", "§7Cierra el panel de administración.");

        open(sp, inv, "admin_main", 6, "§4§l👑 Admin Panel — CobbleMania");
    }

    public static void openEconomyAction(ServerPlayerEntity sp) {
        SimpleInventory inv = new SimpleInventory(27);
        fill(inv, Items.GREEN_STAINED_GLASS_PANE, "§2 ");
        item(inv, 4, Items.EMERALD, "§a§l✦ Economía — Acciones",
            "§7Selecciona una acción para realizar.",
            "§8Luego elegirás jugador y monto.");
        item(inv, 10, Items.LIME_CONCRETE,
            "§a§l[+] Dar CobbleCoins",
            "§7Agrega CobbleCoins al balance de un jugador.",
            "§7► Click para continuar.");
        item(inv, 12, Items.RED_CONCRETE,
            "§c§l[-] Quitar CobbleCoins",
            "§7Resta CobbleCoins del balance de un jugador.",
            "§7► Click para continuar.");
        item(inv, 14, Items.GOLD_BLOCK,
            "§e§l[=] Asignar Balance",
            "§7Define el balance exacto del jugador.",
            "§8Sobreescribe el valor anterior.",
            "§7► Click para continuar.");
        item(inv, 16, Items.PAPER,
            "§f§l[?] Ver Balance",
            "§7Muestra el balance actual de un jugador.",
            "§8Solo lectura — no modifica nada.");
        item(inv, 22, Items.ARROW, "§7« Volver al panel", "§7Regresa al menú principal de admin.");
        open(sp, inv, "economy_action", 3, "§a✦ Economía — Acciones");
    }

    public static void openEconomyPlayer(ServerPlayerEntity sp, int action) {
        targetCtx.put(sp.getUuid(), null);
        amountCtx.put(sp.getUuid(), new double[]{0, action});
        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, Items.GRAY_STAINED_GLASS_PANE, "§8 ");
        String actionName = switch (action) {
            case 0 -> "§a[+] Dar CC";
            case 1 -> "§c[-] Quitar CC";
            case 2 -> "§e[=] Asignar CC";
            default -> "§f[?] Ver Balance";
        };
        item(inv, 4, Items.PLAYER_HEAD, "§7Selecciona un jugador — " + actionName,
            "§7Haz click en un jugador para seleccionarlo.");
        int slot = 9;
        for (ServerPlayerEntity target : sp.getServer().getPlayerManager().getPlayerList()) {
            if (slot >= 45) break;
            double bal = EconomyStorage.getBalance(target.getUuid());
            item(inv, slot, Items.PLAYER_HEAD,
                "§e" + target.getName().getString(),
                "§7Balance: §6" + MissionManager.format(bal) + " CC",
                "§7► Click para seleccionar.");
            slot++;
        }
        item(inv, 49, Items.BARRIER, "§c§l✘ Volver", "§7Regresa a las acciones.");
        open(sp, inv, "economy_player", 6, "§7Selecciona jugador");
    }

    public static void openEconomyAmount(ServerPlayerEntity sp) {
        double[] ctx = amountCtx.getOrDefault(sp.getUuid(), new double[]{0, 0});
        SimpleInventory inv = new SimpleInventory(27);
        fill(inv, Items.GRAY_STAINED_GLASS_PANE, "§8 ");
        item(inv, 10, Items.RED_STAINED_GLASS_PANE,   "§c§l-100", "§7Resta §c100 §7al monto.");
        item(inv, 11, Items.RED_STAINED_GLASS_PANE,   "§c§l-10",  "§7Resta §c10 §7al monto.");
        item(inv, 12, Items.RED_STAINED_GLASS_PANE,   "§c§l-1",   "§7Resta §c1 §7al monto.");
        item(inv, 13, Items.RED_STAINED_GLASS_PANE,   "§c§l-0.1", "§7Resta §c0.1 §7al monto.");
        item(inv, 15, Items.LIME_STAINED_GLASS_PANE,  "§a§l+0.1", "§7Suma §a0.1 §7al monto.");
        item(inv, 16, Items.LIME_STAINED_GLASS_PANE,  "§a§l+1",   "§7Suma §a1 §7al monto.");
        item(inv, 17, Items.LIME_STAINED_GLASS_PANE,  "§a§l+10",  "§7Suma §a10 §7al monto.");
        item(inv, 18, Items.LIME_STAINED_GLASS_PANE,  "§a§l+100", "§7Suma §a100 §7al monto.");
        item(inv, 22, Items.EMERALD,
            "§a§l✔ Confirmar: §6" + MissionManager.format(ctx[0]) + " CC",
            "§7Aplica la acción al jugador seleccionado.",
            "§8Monto: §6" + MissionManager.format(ctx[0]) + " CobbleCoins");
        item(inv, 26, Items.BARRIER, "§c§l✘ Cancelar", "§7Vuelve sin aplicar cambios.");
        open(sp, inv, "economy_amount", 3, "§7Ajustar monto");
    }

    public static void openMissionsConfig(ServerPlayerEntity sp) {
        SimpleInventory inv = new SimpleInventory(27);
        fill(inv, Items.BLUE_STAINED_GLASS_PANE, "§1 ");
        item(inv, 4, Items.WRITABLE_BOOK, "§b§l✦ Config Misiones",
            "§7Ajusta recompensas y multiplicadores.",
            "§8Click izq = aumentar | Click der = reducir");
        item(inv, 10, Items.EMERALD,
            "§a§lRecompensa base: §6" + MissionManager.format(ModeconomiaConfig.DATA.missions.baseReward) + " CC",
            "§7CobbleCoins por misión completada.",
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 12, Items.PAPER,
            "§e§lMisiones diarias: §f" + ModeconomiaConfig.DATA.missions.dailyCount,
            "§7Cantidad de misiones disponibles por día.",
            "§8Click §aizq §8= +1 | Click §cder §8= -1");
        item(inv, 14, Items.BOOK,
            "§f§l✎ Editar misiones",
            "§7Ver, crear, activar o desactivar misiones.",
            "§8Ej: romper 64 bloques, matar 10 mobs.");
        item(inv, 16, Items.VILLAGER_SPAWN_EGG,
            "§6§l⚑ Asignar NPC",
            "§7Mira a un aldeano y haz click para asignarlo.",
            "§8El NPC abrirá el menú de misiones al interactuar.");
        item(inv, 20, Items.LIME_DYE,
            "§a§lTrainer+ ×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.TRAINER_PLUS, 1.5),
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 21, Items.CYAN_DYE,
            "§b§lElite ×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.ELITE, 1.9),
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 22, Items.PURPLE_DYE,
            "§d§lLegendary ×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.LEGENDARY, 2.4),
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 23, Items.ORANGE_DYE,
            "§6§lMythical ×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.MYTHICAL, 2.9),
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 26, Items.ARROW, "§7« Volver", "§7Regresa al panel de administración.");
        open(sp, inv, "missions_config", 3, "§b✦ Config Misiones");
    }

    // FIX 4+5: Misiones — no hay más tipos predeterminados forzados.
    // Cada misión se crea manualmente (nombre, desc, objetivo, recompensa, duración).
    // La lista muestra las existentes + botón de agregar (NUEVA) con scroll si > 18.
    public static void openMissionsList(ServerPlayerEntity sp) {
        openMissionsList(sp, 0);
    }

    public static void openMissionsList(ServerPlayerEntity sp, int page) {
        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, Items.CYAN_STAINED_GLASS_PANE, "§3 ");
        List<MissionDefinition> defs = ModeconomiaConfig.DATA.missions.definitions;
        if (defs == null) defs = new ArrayList<>();

        int pageSize = 18;
        int totalPages = Math.max(1, (int) Math.ceil(defs.size() / (double) pageSize));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int start = page * pageSize;

        item(inv, 4, Items.NETHER_STAR, "§b§l✦ Gestión de Misiones §8(pág " + (page+1) + "/" + totalPages + ")",
            "§7Click izq en misión = gestionar/reactivar.",
            "§7Click der en misión = ELIMINAR.",
            "§8Slot 49 = Agregar NUEVA misión.");

        // Mostrar misiones de esta página
        for (int i = 0; i < pageSize && (start + i) < defs.size(); i++) {
            MissionDefinition def = defs.get(start + i);
            boolean expired = def.isExpired() || !def.active;
            item(inv, i + 9,
                expired ? Items.RED_CONCRETE : Items.LIME_CONCRETE,
                (expired ? "§c§l✗ " : "§a§l✔ ") + def.displayName,
                "§7" + (def.description.isEmpty() ? "Sin descripción" : def.description),
                "§7Tipo: §b" + def.type.name() + " §8| Objetivo: §f" + def.requiredAmount,
                "§7Recompensa: §6" + MissionManager.format(def.reward > 0 ? def.reward : ModeconomiaConfig.DATA.missions.baseReward) + " CC",
                "§7Tiempo restante: " + def.timeRemaining(),
                expired ? "§c► Click izq = reactivar | Click der = eliminar"
                        : "§7► Click izq = gestionar | Click der = finalizar");
        }

        // Navegación
        if (page > 0)
            item(inv, 45, Items.ARROW, "§7« Página anterior", "§7Página " + page + "/" + totalPages);
        if (page < totalPages - 1)
            item(inv, 53, Items.ARROW, "§7Página siguiente »", "§7Página " + (page+2) + "/" + totalPages);

        // FIX 5: Botón añadir siempre visible en slot 49
        item(inv, 49, Items.NETHER_STAR, "§a§l+ Agregar nueva misión",
            "§7Crea una misión con nombre, descripción,",
            "§7objetivo, recompensa y duración personalizados.",
            "§7► Click para crear.");

        // Volver
        item(inv, 47, Items.BARRIER, "§c§l✘ Volver", "§7Regresa a config de misiones.");

        // Store page
        detailCtx.put(sp.getUuid(), "misspage:" + page);
        open(sp, inv, "missions_list", 6, "§b✎ Gestión de Misiones");
    }

    /** GUI para seleccionar el tipo de misión al crear una nueva — reemplaza el chat */
    public static void openMissionTypePicker(ServerPlayerEntity sp) {
        openMissionTypePicker(sp, 0);
    }

    public static void openMissionTypePicker(ServerPlayerEntity sp, int page) {
        SimpleInventory inv = new SimpleInventory(54);
        if (page == 0) {
            fill(inv, Items.BLUE_STAINED_GLASS_PANE, "§1 ");
            item(inv, 4, Items.NETHER_STAR, "§b§l✦ Tipo de Misión §81/4",
                "§7Click en el tipo para crear la misión.", "§7Página 1: Vanilla + Captura.");

            // ── Vanilla ──
            item(inv, 10, Items.IRON_PICKAXE,    "§f§lRomper Bloques",       "§7Romper X bloques de cualquier tipo.");
            item(inv, 11, Items.BONE,             "§c§lMatar Mobs",           "§7Matar X mobs (cualquiera).");
            item(inv, 12, Items.LEATHER_BOOTS,    "§e§lCaminar Distancia",    "§7Caminar X bloques de distancia.");
            item(inv, 13, Items.CLOCK,            "§b§lTiempo Conectado",     "§7Estar X minutos en el servidor.");
            item(inv, 14, Items.OAK_DOOR,         "§6§lConectarse Hoy",       "§7Conectarse al servidor (1 por día).");

            // ── Captura básica ──
            item(inv, 19, Items.PURPLE_STAINED_GLASS_PANE, "§d§lCapturar Pokémon",   "§7Capturar X Pokémon cualquiera.");
            item(inv, 20, Items.GOLD_NUGGET,      "§6§lCapturar Shiny",       "§7Capturar X Pokémon shiny.");
            item(inv, 21, Items.NETHER_STAR,      "§c§lCapturar Legendario",  "§7Capturar X Pokémon legendario/mítico.");
            item(inv, 22, Items.NAME_TAG,         "§a§lCapturar Especie",     "§7Capturar X de especie específica.", "§8Desc: pikachu");
            item(inv, 23, Items.LIME_DYE,         "§a§lCapturar por Tipo",    "§7Capturar X de un tipo.", "§8Desc: FIRE");

            // ── Captura avanzada ──
            item(inv, 28, Items.PAPER,            "§7§lCapturar con Ball",    "§7Capturar X con ball específica.", "§8Desc: FAST_BALL");
            item(inv, 29, Items.RED_DYE,          "§c§lTipo + Ball",          "§7Capturar tipo X con ball Y.", "§8Desc: FIRE:FAST_BALL");
            item(inv, 30, Items.GRASS_BLOCK,      "§a§lCapturar en Bioma",    "§7Capturar X en un bioma específico.", "§8Desc: forest");
            item(inv, 31, Items.EXPERIENCE_BOTTLE,"§b§lCapturar por Nivel",   "§7Capturar X en rango de nivel.", "§8Desc: 20:40");
            item(inv, 32, Items.GOLD_INGOT,       "§6§lShiny por Tipo",        "§7Capturar X shiny de tipo específico.", "§8Desc: GHOST");
            item(inv, 33, Items.DIAMOND,          "§b§lShiny con Ball",        "§7Capturar X shiny con ball específica.", "§8Desc: MASTER_BALL");

            // ── Condiciones ──
            item(inv, 37, Items.CLOCK,            "§e§lCapturar de Noche",    "§7Capturar X Pokémon de noche.");
            item(inv, 38, Items.WATER_BUCKET,     "§9§lCapturar bajo Lluvia", "§7Capturar X Pokémon mientras llueve.");
            item(inv, 39, Items.SHIELD,           "§7§lCapturar sin Daño",    "§7Capturar X Pokémon sin recibir daño en la batalla.");

            item(inv, 53, Items.ARROW, "§7Siguiente página »", "§7Página 2: Combate, Crianza y más.");
            item(inv, 45, Items.BARRIER, "§c§l✘ Cancelar", "§7Vuelve a la lista de misiones.");

        } else if (page == 1) {
            fill(inv, Items.PURPLE_STAINED_GLASS_PANE, "§5 ");
            item(inv, 4, Items.NETHER_STAR, "§d§l✦ Tipo de Misión §82/4",
                "§7Click en el tipo para crear la misión.", "§7Página 2: Combate, Crianza y Especiales.");

            // ── Combate ──
            item(inv, 10, Items.IRON_SWORD,       "§c§lDerrotar Pokémon",     "§7Derrotar X Pokémon en batalla.");
            item(inv, 11, Items.GRASS_BLOCK,      "§a§lDerrotar Salvaje",      "§7Derrotar X Pokémon salvajes.");
            item(inv, 12, Items.DIAMOND_SWORD,    "§b§lDerrotar Especie",      "§7Derrotar X de especie específica.", "§8Desc: rattata");
            item(inv, 13, Items.FIRE_CHARGE,      "§6§lDerrotar por Tipo",     "§7Derrotar X de tipo específico.", "§8Desc: WATER");
            item(inv, 14, Items.PLAYER_HEAD,      "§e§lGanar vs Jugador",      "§7Ganar X batallas contra jugadores.");
            item(inv, 15, Items.SHIELD,           "§d§lGanar con Tipo",        "§7Ganar X batallas usando Pokémon del tipo.", "§8Desc: FIRE");
            item(inv, 16, Items.TOTEM_OF_UNDYING, "§6§lRacha de Victorias",    "§7Ganar X batallas consecutivas.", "§8Se resetea al perder.");

            // ── Crianza y evolución ──
            item(inv, 19, Items.BLAZE_POWDER,     "§6§lEvolucionar Pokémon",   "§7Evolucionar X Pokémon cualquiera.");
            item(inv, 20, Items.BLAZE_ROD,        "§c§lEvolucionar Especie",   "§7Evolucionar especie específica.", "§8Desc: eevee");
            item(inv, 21, Items.EGG,              "§f§lHacer Eclosionar Huevo","§7Hacer eclosionar X huevos.");
            item(inv, 22, Items.BONE_BLOCK,       "§7§lRevivir Fósil",          "§7Revivir X fósiles de Pokémon.");

            // ── Interacción ──
            item(inv, 28, Items.BOOK,             "§b§lRegistrar en Pokédex",  "§7Registrar X entradas nuevas. — AUTO");
            item(inv, 29, Items.GOLD_BLOCK,       "§6§lIntercambiar Pokémon",  "§7Intercambiar X Pokémon con otros jugadores.");
            item(inv, 30, Items.FEATHER,          "§7§lLiberar Pokémon",        "§7Liberar X Pokémon.");
            item(inv, 31, Items.NAME_TAG,         "§a§lPonerle Apodo",          "§7Darle apodo a X Pokémon.");
            item(inv, 32, Items.WRITABLE_BOOK,    "§f§lCraftear Items",         "§7Craftear X items en cualquier mesa.");
            item(inv, 33, Items.COD,              "§b§lPescar",                "§7Pescar X veces con la caña.");

            item(inv, 45, Items.ARROW, "§7« Página anterior", "§7Vuelve a la página 1.");
            item(inv, 53, Items.ARROW, "§7Siguiente página »", "§7Página 3: Nuevas misiones Cobbleverse.");

        } else if (page == 2) {
            // PAGE 3 — NUEVOS TIPOS COBBLEVERSE
            fill(inv, Items.GREEN_STAINED_GLASS_PANE, "§2 ");
            item(inv, 4, Items.EMERALD, "§a§l✦ Tipo de Misión §83/4",
                "§7Click en el tipo para crear la misión.",
                "§7Página 3: Nuevas misiones Cobbleverse 🌿");

            item(inv, 10, Items.AMETHYST_SHARD,   "§5§lCapturar Paradoja",     "§7Capturar X Pokémon paradoja.");
            item(inv, 11, Items.DIAMOND_SWORD,    "§5§lDerrotar Paradoja",     "§7Derrotar X Pokémon paradoja en batalla.");
            item(inv, 12, Items.GOLD_NUGGET,      "§6§lShiny Full Odds",       "§7Capturar X shiny sin items de suerte.");
            item(inv, 13, Items.COD,              "§f§lSolo con Poké Ball",    "§7Capturar X Pokémon usando solo Poké Ball.");
            item(inv, 14, Items.SNOWBALL,         "§f§lCapturar Nivel 1",      "§7Capturar X Pokémon exactamente nivel 1.");
            item(inv, 15, Items.WATER_BUCKET,     "§9§lCapturar en Tormenta",  "§7Capturar X Pokémon durante tormenta.");
            item(inv, 16, Items.PINK_DYE,         "§d§lCapturar Radiante/Tipo","§7Capturar X radiante de tipo específico.", "§8Desc: dragon");

            item(inv, 19, Items.BLAZE_POWDER,     "§e§lEvolucionar con Piedra","§7Evolucionar X Pokémon usando piedra evolutiva.");
            item(inv, 20, Items.EGG,              "§f§lEclosionar Especie",    "§7Eclosionar huevo de especie específica.", "§8Desc: magikarp");
            item(inv, 21, Items.NAME_TAG,         "§a§lApodo Especie",         "§7Poner apodo a especie específica.", "§8Desc: pikachu");
            item(inv, 22, Items.FEATHER,          "§6§lLiberar Shiny",         "§7Liberar X Pokémon shiny.");
            item(inv, 23, Items.SHIELD,           "§b§lGanar sin Legendarios", "§7Ganar X batallas sin usar legendarios en el equipo.");

            item(inv, 45, Items.ARROW, "§7« Página anterior", "§7Vuelve a la página 2.");
            item(inv, 53, Items.ARROW, "§7Siguiente página »", "§7Página 4: Misiones Ranked ⚔");

        } else {
            // PAGE 4 — RANKED
            fill(inv, Items.ORANGE_STAINED_GLASS_PANE, "§6 ");
            item(inv, 4, Items.GOLDEN_SWORD, "§6§l✦ Tipo de Misión §84/4",
                "§7Click en el tipo para crear la misión.",
                "§7Página 4: Misiones Ranked ⚔",
                "§8Requiere el mod CobbleMaiaRanked instalado.");

            item(inv, 10, Items.GOLDEN_SWORD,      "§6§l⚔ Ganar Ranked",         "§7Ganar X batallas ranked.", "§8Cuenta cualquier victoria.");
            item(inv, 11, Items.TOTEM_OF_UNDYING,  "§e§l⚔ Racha Ranked",         "§7Ganar X batallas ranked consecutivas.", "§8Se resetea al perder.");
            item(inv, 12, Items.PLAYER_HEAD,       "§b§l⚔ Jugar Ranked",          "§7Jugar X batallas ranked.", "§8Cuenta ganes o pierdas.");
            item(inv, 19, Items.FIRE_CHARGE,       "§c§l⚔ Ranked con Tipo",       "§7Ganar ranked usando equipo del tipo.", "§8Desc: FIRE");
            item(inv, 20, Items.NETHER_STAR,       "§d§l⚔ Ranked vs Rango",       "§7Ganar ranked contra jugador del rango.", "§8Desc: campeon / alto mando / lider / entrenador");
            item(inv, 21, Items.DIAMOND_SWORD,     "§a§l⚔ Ranked Impecable",      "§7Ganar ranked sin perder ningún Pokémon.");
            item(inv, 22, Items.BARRIER,           "§7§l⚔ Ranked sin Objetos",    "§7Ganar ranked sin usar objetos.");
            item(inv, 28, Items.EXPERIENCE_BOTTLE, "§b§l⚔ Alcanzar ELO",          "§7Alcanzar X puntos de ELO ranked.", "§8Desc: ELO objetivo (ej: 1200)", "§8AUTO.");

            item(inv, 45, Items.ARROW, "§7« Página anterior", "§7Vuelve a la página 3.");
            item(inv, 53, Items.BARRIER, "§c§l✘ Cancelar", "§7Vuelve a la lista de misiones.");
        }
        detailCtx.put(sp.getUuid(), "typepicker:" + page);
        open(sp, inv, "mission_type_picker", 6, "§b✦ Tipo de Misión [" + (page+1) + "/4]");
    }

    private void handleMissionTypePicker(ServerPlayerEntity sp, int slot) {
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "typepicker:0");
        int page = 0;
        try { if (ctx.startsWith("typepicker:")) page = Integer.parseInt(ctx.substring(11)); } catch (Exception ignored) {}

        // Navigation
        if (page == 0) {
            if (slot == 45) { openMissionsList(sp); return; }
            if (slot == 53) { openMissionTypePicker(sp, 1); return; }
        } else if (page == 1) {
            if (slot == 45) { openMissionTypePicker(sp, 0); return; }
            if (slot == 53) { openMissionTypePicker(sp, 2); return; }
        } else if (page == 2) {
            if (slot == 45) { openMissionTypePicker(sp, 1); return; }
            if (slot == 53) { openMissionTypePicker(sp, 3); return; }
        } else {
            if (slot == 45) { openMissionTypePicker(sp, 2); return; }
            if (slot == 53) { openMissionsList(sp); return; }
        }

        com.cobblemania.economia.data.MissionType type = switch (page) {
            case 0 -> switch (slot) {
                case 10 -> com.cobblemania.economia.data.MissionType.BREAK_BLOCKS;
                case 11 -> com.cobblemania.economia.data.MissionType.WALK_DISTANCE;
                case 13 -> com.cobblemania.economia.data.MissionType.PLAYTIME_MINUTES;
                case 14 -> com.cobblemania.economia.data.MissionType.JOIN_SERVER;
                case 19 -> com.cobblemania.economia.data.MissionType.CAPTURE_COBBLEMON;
                case 20 -> com.cobblemania.economia.data.MissionType.CAPTURE_SHINY_COBBLEMON;
                case 21 -> com.cobblemania.economia.data.MissionType.CAPTURE_LEGENDARY_COBBLEMON;
                case 22 -> com.cobblemania.economia.data.MissionType.CAPTURE_SPECIFIC_SPECIES;
                case 23 -> com.cobblemania.economia.data.MissionType.CAPTURE_SPECIFIC_TYPE;
                case 28 -> com.cobblemania.economia.data.MissionType.CAPTURE_WITH_BALL;
                case 29 -> com.cobblemania.economia.data.MissionType.CAPTURE_TYPE_WITH_BALL;
                case 30 -> com.cobblemania.economia.data.MissionType.CAPTURE_IN_BIOME;
                case 31 -> com.cobblemania.economia.data.MissionType.CAPTURE_LEVEL_RANGE;
                case 32 -> com.cobblemania.economia.data.MissionType.CAPTURE_SHINY_SPECIFIC_TYPE;
                case 33 -> com.cobblemania.economia.data.MissionType.CAPTURE_SHINY_WITH_BALL;
                case 37 -> com.cobblemania.economia.data.MissionType.CATCH_AT_NIGHT;
                case 38 -> com.cobblemania.economia.data.MissionType.CATCH_DURING_RAIN;
                case 39 -> com.cobblemania.economia.data.MissionType.CATCH_WITHOUT_DAMAGE;
                default -> null;
            };
            case 1 -> switch (slot) {
                case 10 -> com.cobblemania.economia.data.MissionType.DEFEAT_COBBLEMON;
                case 11 -> com.cobblemania.economia.data.MissionType.DEFEAT_WILD_COBBLEMON;
                case 12 -> com.cobblemania.economia.data.MissionType.DEFEAT_SPECIFIC_SPECIES;
                case 13 -> com.cobblemania.economia.data.MissionType.DEFEAT_SPECIFIC_TYPE;
                case 14 -> com.cobblemania.economia.data.MissionType.WIN_TRAINER_BATTLE;
                case 15 -> com.cobblemania.economia.data.MissionType.WIN_BATTLE_WITH_TYPE;
                case 16 -> com.cobblemania.economia.data.MissionType.WIN_STREAK;
                case 19 -> com.cobblemania.economia.data.MissionType.EVOLVE_COBBLEMON;
                case 20 -> com.cobblemania.economia.data.MissionType.EVOLVE_SPECIFIC_SPECIES;
                case 21 -> com.cobblemania.economia.data.MissionType.HATCH_EGG;
                case 22 -> com.cobblemania.economia.data.MissionType.REVIVE_FOSSIL;
                case 28 -> com.cobblemania.economia.data.MissionType.COLLECT_POKEDEX_ENTRIES;
                case 29 -> com.cobblemania.economia.data.MissionType.TRADE_POKEMON;
                case 30 -> com.cobblemania.economia.data.MissionType.RELEASE_POKEMON;
                case 31 -> com.cobblemania.economia.data.MissionType.NICKNAME_POKEMON;
                case 32 -> com.cobblemania.economia.data.MissionType.CRAFT_ITEMS;
                case 33 -> com.cobblemania.economia.data.MissionType.FISH_ITEMS;
                default -> null;
            };
            case 2 -> switch (slot) { // Nuevos tipos Cobbleverse
                case 10 -> com.cobblemania.economia.data.MissionType.CAPTURE_PARADOX_COBBLEMON;
                case 11 -> com.cobblemania.economia.data.MissionType.DEFEAT_PARADOX_COBBLEMON;
                case 12 -> com.cobblemania.economia.data.MissionType.CAPTURE_FULL_ODDS_SHINY;
                case 13 -> com.cobblemania.economia.data.MissionType.CATCH_USING_ONLY_POKEBALL;
                case 14 -> com.cobblemania.economia.data.MissionType.CAPTURE_COBBLEMON_LEVEL_1;
                case 15 -> com.cobblemania.economia.data.MissionType.CAPTURE_IN_WEATHER;
                case 16 -> com.cobblemania.economia.data.MissionType.CATCH_RADIANT_SPECIFIC_TYPE;
                case 19 -> com.cobblemania.economia.data.MissionType.EVOLVE_USING_STONE;
                case 20 -> com.cobblemania.economia.data.MissionType.HATCH_SPECIFIC_SPECIES;
                case 21 -> com.cobblemania.economia.data.MissionType.NICKNAME_SPECIFIC_POKEMON;
                case 22 -> com.cobblemania.economia.data.MissionType.RELEASE_SHINY;
                case 23 -> com.cobblemania.economia.data.MissionType.WIN_BATTLE_NO_LEGENDARY;
                default -> null;
            };
            default -> switch (slot) { // page 3 = Ranked
                case 10 -> com.cobblemania.economia.data.MissionType.WIN_RANKED_BATTLE;
                case 11 -> com.cobblemania.economia.data.MissionType.WIN_RANKED_CONSECUTIVE;
                case 12 -> com.cobblemania.economia.data.MissionType.PLAY_RANKED_BATTLES;
                case 19 -> com.cobblemania.economia.data.MissionType.WIN_RANKED_WITH_TYPE;
                case 20 -> com.cobblemania.economia.data.MissionType.WIN_RANKED_SPECIFIC_RANK;
                case 21 -> com.cobblemania.economia.data.MissionType.WIN_RANKED_FLAWLESS;
                case 22 -> com.cobblemania.economia.data.MissionType.WIN_RANKED_NO_ITEMS;
                case 28 -> com.cobblemania.economia.data.MissionType.REACH_ELO;
                default -> null;
            };
        };

        if (type == null) return;
        String typeName = type.name();

        // Hint contextual según el tipo
        String hint = switch (type) {
            case CAPTURE_SPECIFIC_SPECIES, DEFEAT_SPECIFIC_SPECIES, EVOLVE_SPECIFIC_SPECIES ->
                "§8Descripción: nombre de especie (ej: §fpikachu§8).";
            case CAPTURE_SPECIFIC_TYPE, DEFEAT_SPECIFIC_TYPE, CAPTURE_SHINY_SPECIFIC_TYPE,
                 WIN_BATTLE_WITH_TYPE, WIN_RANKED_WITH_TYPE ->
                "§8Tipos disponibles:\n§fNORMAL §8· §fFIRE §8· §fWATER §8· §fGRASS §8· §fELECTRIC §8· §fICE\n§fFIGHTING §8· §fPOISON §8· §fGROUND §8· §fFLYING §8· §fPSYCHIC §8· §fBUG\n§fROCK §8· §fGHOST §8· §fDRAGON §8· §fDARK §8· §fSTEEL §8· §fFAIRY";
            case CAPTURE_WITH_BALL, CAPTURE_SHINY_WITH_BALL ->
                "§8Descripción: nombre de ball (ej: §fFAST_BALL§8, §fMASTER_BALL§8).";
            case CAPTURE_TYPE_WITH_BALL ->
                "§8Descripción: §fTIPO:BALL §8(ej: §fFIRE:FAST_BALL§8).";
            case CAPTURE_IN_BIOME ->
                "§8Descripción: bioma (ej: §fforest§8, §fdesert§8, §focean§8).";
            case CAPTURE_LEVEL_RANGE ->
                "§8Descripción: §fMIN:MAX §8(ej: §f20:40 §8para niveles 20 a 40).";
            case WIN_RANKED_SPECIFIC_RANK ->
                "§8Descripción: §fcampeon §8/ §falto mando §8/ §flider §8/ §fentrenador";
            case REACH_ELO ->
                "§8Descripción: ELO objetivo (ej: §f1200§8, §f1500§8, §f1800§8).";
            case HATCH_SPECIFIC_SPECIES, NICKNAME_SPECIFIC_POKEMON ->
                "§8Descripción: nombre de especie (ej: §fmagikarp§8, §fpikachu§8).";
            case CATCH_RADIANT_SPECIFIC_TYPE ->
                "§8Descripción: tipo del radiante (ej: §fdragon§8, §ffire§8, §fwater§8).";
            default -> "";
        };

        sp.sendMessage(Text.literal("§a§l✔ §aTipo seleccionado: §f" + typeName), false);
        if (!hint.isEmpty()) sp.sendMessage(Text.literal(hint), false);
        sp.sendMessage(Text.literal("§7Escribe el §enombre §7de la misión:"), false);
        sp.sendMessage(Text.literal("§7(escribe §fcancel §7para cancelar)"), false);
        awaitingInput.put(sp.getUuid(), "mission_name:" + typeName);
    }

    // FIX 1: missions_view now shows selection UI — player clicks a mission to make it active.
    // Only ONE mission can be active at a time. Passive missions (walk/playtime/join) auto-track.
    public static void openMissionsView(ServerPlayerEntity sp) { openMissionsView(sp, 0); }
    public static void openMissionsView(ServerPlayerEntity sp, int page) {
        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, Items.CYAN_STAINED_GLASS_PANE, "§3 ");
        PlayerMissionData data = EconomyStorage.getMissionData(sp.getUuid());
        List<MissionDefinition> defs = ModeconomiaConfig.DATA.missions.definitions;
        Rank rank = RankManager.getRank(sp);
        double mult = ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(rank, 1.0);
        List<MissionDefinition> active = new ArrayList<>();
        if (defs != null) {
            for (MissionDefinition d : defs) if (d.active && !d.isExpired()) active.add(d);
        }
        long completedCount = active.stream().filter(d -> data.completed.contains(d.id)).count();
        String currentActive = data.activeMissionId;

        item(inv, 4, Items.NETHER_STAR,
            "§b§l✦ Misiones §7[" + completedCount + "/" + active.size() + "]",
            "§7Haz §eclick izquierdo §7en una misión para seleccionarla.",
            "§8Solo puedes hacer §eUNA §8misión a la vez.",
            currentActive != null
                ? "§7En curso: §e" + getMissionName(defs, currentActive)
                : "§7No tienes ninguna misión seleccionada.",
            "§7Completadas hoy: §a" + completedCount + "§7/§f" + active.size());

        int pageSize = 28; // slots 9-36 = 28 misiones por página
        int totalPages = Math.max(1, (int) Math.ceil(active.size() / (double) pageSize));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        int startIdx = page * pageSize;

        for (int i = 0; i < pageSize && (startIdx + i) < active.size(); i++) {
            MissionDefinition def = active.get(startIdx + i);
            int current = data.progress.getOrDefault(def.id, 0);
            boolean done = data.completed.contains(def.id);
            boolean isSelected = def.id.equals(currentActive);
            boolean isPassive  = MissionManager.isPassive(def.type);
            double reward = (def.reward > 0 ? def.reward : ModeconomiaConfig.DATA.missions.baseReward) * mult;

            Item icon;
            String status;
            if (done) {
                icon = Items.LIME_CONCRETE;
                status = "§a§l✔ COMPLETADA";
            } else if (isSelected) {
                icon = Items.YELLOW_CONCRETE;
                status = "§e§l▶ EN CURSO";
            } else if (isPassive) {
                icon = Items.BLUE_CONCRETE;
                status = "§b§lAUTO";
            } else {
                icon = Items.ORANGE_CONCRETE;
                status = "§7Disponible";
            }

            // f1/f2 for typeLabel — use filter1/filter2 if present, else fallback to description
            String f1raw = (def.filter1 != null && !def.filter1.isEmpty()) ? def.filter1 : def.description;
            String f2raw = (def.filter2 != null && !def.filter2.isEmpty()) ? def.filter2 : "";
            // Translate type/ball/rank to Spanish: "FIRE (Fuego)", "FAST_BALL (Veloz Ball)", etc.
            String f1 = com.cobblemania.economia.util.PokemonTranslations.translate(f1raw);
            String f2 = com.cobblemania.economia.util.PokemonTranslations.translate(f2raw);
            String typeLabel = switch (def.type) {
                // Vanilla
                case BREAK_BLOCKS            -> "§8Romper bloques";
                case BREAK_SPECIFIC_BLOCK    -> "§8Bloque: §f" + f1;
                case WALK_DISTANCE           -> "§8Caminar (bloques) §7— AUTO";
                case PLAYTIME_MINUTES        -> "§8Tiempo conectado (min) §7— AUTO";
                case JOIN_SERVER             -> "§8Conectarse hoy §7— AUTO";
                case PLACE_BLOCKS            -> "§8Colocar bloques";
                case CRAFT_ITEMS             -> "§8Craftear items";
                case FISH_ITEMS              -> "§8Pescar";
                // Captura básica
                case CAPTURE_COBBLEMON             -> "§8Capturar Pokémon";
                case CAPTURE_SHINY_COBBLEMON       -> "§8Capturar §6Shiny";
                case CAPTURE_LEGENDARY_COBBLEMON   -> "§8Capturar §cLegendario/Mítico";
                case CAPTURE_RADIANT_COBBLEMON     -> "§8Capturar §dRadiante";
                case CAPTURE_PARADOX_COBBLEMON     -> "§8Capturar §5Paradoja";
                // Captura con filtro
                case CAPTURE_SPECIFIC_SPECIES      -> "§8Especie: §f" + f1;
                case CAPTURE_SPECIFIC_TYPE         -> "§8Tipo: §f" + f1;
                case CAPTURE_WITH_BALL             -> "§8Ball: §f" + f1;
                case CAPTURE_TYPE_WITH_BALL        -> "§8Tipo: §f" + f1 + (f2.isEmpty() ? "" : " §8| Ball: §f" + f2);
                case CAPTURE_SHINY_SPECIFIC_TYPE   -> "§8Shiny tipo: §f" + f1;
                case CAPTURE_SHINY_WITH_BALL       -> "§8Shiny ball: §f" + f1;
                case CAPTURE_LEVEL_RANGE           -> "§8Nivel: §f" + f1;
                case CAPTURE_IN_BIOME              -> "§8Bioma: §f" + f1;
                // Condiciones especiales
                case CATCH_AT_NIGHT                -> "§8De noche";
                case CATCH_DURING_RAIN             -> "§8Con lluvia";
                case CATCH_WITHOUT_DAMAGE          -> "§8Sin recibir daño";
                // Combate
                case DEFEAT_COBBLEMON              -> "§8Derrotar Pokémon";
                case DEFEAT_WILD_COBBLEMON         -> "§8Derrotar salvaje";
                case DEFEAT_SPECIFIC_SPECIES       -> "§8Especie: §f" + f1;
                case DEFEAT_SPECIFIC_TYPE          -> "§8Tipo: §f" + f1;
                case WIN_TRAINER_BATTLE            -> "§8Ganar vs jugador (PvP)";
                case WIN_BATTLE_WITH_TYPE          -> "§8Ganar con tipo: §f" + f1;
                case WIN_STREAK                    -> "§8Racha victorias normales";
                // Crianza
                case EVOLVE_COBBLEMON              -> "§8Evolucionar";
                case EVOLVE_SPECIFIC_SPECIES       -> "§8Evolucionar: §f" + f1;
                case HATCH_EGG                     -> "§8Hacer eclosionar huevo";
                // Interacción
                case REVIVE_FOSSIL                 -> "§8Revivir fósil";
                case TRADE_POKEMON                 -> "§8Intercambiar Pokémon";
                case RELEASE_POKEMON               -> "§8Liberar Pokémon";
                case NICKNAME_POKEMON              -> "§8Ponerle apodo";
                // Colección
                case COLLECT_POKEDEX_ENTRIES       -> "§8Pokédex §7— AUTO";
                // Nuevos tipos Cobbleverse
                case DEFEAT_PARADOX_COBBLEMON      -> "§8Derrotar §5Paradoja";
                case CAPTURE_FULL_ODDS_SHINY       -> "§8Capturar §6Shiny §8full odds";
                case WIN_BATTLE_NO_LEGENDARY       -> "§8Ganar sin legendarios";
                case HATCH_SPECIFIC_SPECIES        -> "§8Eclosionar: §f" + f1;
                case CATCH_USING_ONLY_POKEBALL     -> "§8Solo con Poké Ball";
                case DEFEAT_COBBLEMON_TYPE_COMBO   -> "§8Derrotar doble tipo: §f" + f1;
                case CAPTURE_COBBLEMON_LEVEL_1     -> "§8Capturar nivel 1";
                case CAPTURE_IN_WEATHER            -> "§8Capturar en tormenta";
                case WIN_BATTLE_UNDERLEVELED       -> "§8Ganar con nivel inferior";
                case EVOLVE_USING_STONE            -> "§8Evolucionar con piedra";
                case NICKNAME_SPECIFIC_POKEMON     -> "§8Apodo a: §f" + f1;
                case RELEASE_SHINY                 -> "§8Liberar §6Shiny";
                case CATCH_RADIANT_SPECIFIC_TYPE   -> "§8Capturar §dRadiante tipo: §f" + f1;
                // Ranked
                case WIN_RANKED_BATTLE             -> "§6⚔ §8Ganar Ranked";
                case WIN_RANKED_CONSECUTIVE        -> "§6⚔ §8Racha Ranked";
                case WIN_RANKED_WITH_TYPE          -> "§6⚔ §8Ranked tipo: §f" + f1;
                case WIN_RANKED_NO_ITEMS           -> "§6⚔ §8Ranked sin objetos";
                case WIN_RANKED_SPECIFIC_RANK      -> "§6⚔ §8Ranked vs rango: §f" + f1;
                case WIN_RANKED_FLAWLESS           -> "§6⚔ §8Ranked sin perder Pokémon";
                case PLAY_RANKED_BATTLES           -> "§6⚔ §8Jugar Ranked";
                case REACH_ELO                     -> "§6⚔ §8Alcanzar §f" + f1 + " §8ELO §7— AUTO";
                default                            -> "§8" + def.type.name().toLowerCase().replace('_', ' ');
            };

            // description is now always the public visible text for the player
            // filter1/filter2 are the internal filters (shown in typeLabel, not here)
            String descLine = def.description.isEmpty() ? "" : "§7" + def.description;

            // Líneas del tooltip — siempre se muestran todas
            String progressLine = "§7Progreso: " + (done ? "§a§lCOMPLETADA" : "§e" + current + "§7/§f" + def.requiredAmount);
            String rewardLine   = "§7Recompensa: §6+" + MissionManager.format(reward) + " CC";
            String timeLine     = def.expiresAt > 0 ? "§7Tiempo restante: " + def.timeRemaining() : "";
            String actionLine   = done       ? "§8Ya recibiste la recompensa." :
                                  isPassive  ? "§8Esta misión se completa automáticamente." :
                                  isSelected ? "§e► ¡Estás haciendo esta misión!" :
                                               "§a► Click para seleccionar y comenzar.";

            if (!descLine.isEmpty() && !timeLine.isEmpty()) {
                item(inv, i + 9, icon,
                    (done ? "§a§l✔ " : isSelected ? "§e§l▶ " : "§7") + def.displayName,
                    descLine,
                    "§7Tipo: " + typeLabel,
                    progressLine,
                    "§7Estado: " + status,
                    rewardLine,
                    timeLine,
                    actionLine);
            } else if (!descLine.isEmpty()) {
                item(inv, i + 9, icon,
                    (done ? "§a§l✔ " : isSelected ? "§e§l▶ " : "§7") + def.displayName,
                    descLine,
                    "§7Tipo: " + typeLabel,
                    progressLine,
                    "§7Estado: " + status,
                    rewardLine,
                    actionLine);
            } else if (!timeLine.isEmpty()) {
                item(inv, i + 9, icon,
                    (done ? "§a§l✔ " : isSelected ? "§e§l▶ " : "§7") + def.displayName,
                    "§7Tipo: " + typeLabel,
                    progressLine,
                    "§7Estado: " + status,
                    rewardLine,
                    timeLine,
                    actionLine);
            } else {
                item(inv, i + 9, icon,
                    (done ? "§a§l✔ " : isSelected ? "§e§l▶ " : "§7") + def.displayName,
                    "§7Tipo: " + typeLabel,
                    progressLine,
                    "§7Estado: " + status,
                    rewardLine,
                    actionLine);
            }
        }
        // Navegación de páginas
        if (page > 0)
            item(inv, 45, Items.ARROW, "§7« Anterior", "§7Página " + page + "/" + totalPages);
        item(inv, 49, Items.BARRIER, "§c§l✘ Cerrar", "§7Cierra este menú.");
        if (page < totalPages - 1)
            item(inv, 53, Items.ARROW, "§7Siguiente »", "§7Página " + (page + 2) + "/" + totalPages);
        else
            item(inv, 53, Items.CYAN_STAINED_GLASS_PANE, "§3 ", "");
        // Guardar página actual para el click handler
        detailCtx.put(sp.getUuid(), "mvpage:" + page);
        open(sp, inv, "missions_view", 6, "§b✦ Misiones §8(" + (page+1) + "/" + totalPages + ")");
    }

    private static String getMissionName(List<MissionDefinition> defs, String id) {
        if (defs == null || id == null) return "ninguna";
        for (MissionDefinition d : defs) if (d.id.equals(id)) return d.displayName;
        return "ninguna";
    }

    public static void openAfkConfig(ServerPlayerEntity sp) {
        SimpleInventory inv = new SimpleInventory(27);
        fill(inv, Items.YELLOW_STAINED_GLASS_PANE, "§e ");
        String pos1 = "§fX=" + ModeconomiaConfig.DATA.afk.pos1X
                    + " Y=" + ModeconomiaConfig.DATA.afk.pos1Y
                    + " Z=" + ModeconomiaConfig.DATA.afk.pos1Z;
        String pos2 = "§fX=" + ModeconomiaConfig.DATA.afk.pos2X
                    + " Y=" + ModeconomiaConfig.DATA.afk.pos2Y
                    + " Z=" + ModeconomiaConfig.DATA.afk.pos2Z;
        String worldStr = ModeconomiaConfig.DATA.afk.world != null ? ModeconomiaConfig.DATA.afk.world : "§cNo configurado";
        item(inv, 4, Items.CLOCK, "§e§l✦ Config Zona AFK",
            "§7Configura la zona donde los jugadores ganan CC.",
            "§8Mundo: §f" + worldStr);
        item(inv, 10, Items.LIME_CONCRETE,
            "§a§l▶ Set Pos1 §8(esquina 1)",
            "§7Guarda tu posición actual como esquina 1.",
            "§8Guardada: " + pos1,
            "§7► Párate en la esquina y haz click.");
        item(inv, 12, Items.LIME_CONCRETE,
            "§a§l▶ Set Pos2 §8(esquina 2)",
            "§7Guarda tu posición actual como esquina 2.",
            "§8Guardada: " + pos2,
            "§7► Párate en la esquina opuesta y haz click.");
        item(inv, 14, Items.EMERALD,
            "§a§lRecompensa: §6" + MissionManager.format(ModeconomiaConfig.DATA.afk.baseReward) + " CC",
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 16, Items.CLOCK,
            "§e§lIntervalo: §f" + ModeconomiaConfig.DATA.afk.intervalMinutes + " min",
            "§8Click §aizq §8= +1 | Click §cder §8= -1");
        item(inv, 20, Items.LIME_DYE,
            "§a§lTrainer+ ×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.TRAINER_PLUS, 1.3),
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 21, Items.CYAN_DYE,
            "§b§lElite ×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.ELITE, 1.8),
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 22, Items.PURPLE_DYE,
            "§d§lLegendary ×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.LEGENDARY, 2.0),
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 23, Items.ORANGE_DYE,
            "§6§lMythical ×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.MYTHICAL, 2.7),
            "§8Click §aizq §8= +0.1 | Click §cder §8= -0.1");
        item(inv, 26, Items.ARROW, "§7« Volver", "§7Regresa al panel de administración.");
        open(sp, inv, "afk_config", 3, "§e✦ Config Zona AFK");
    }

    // FIX 6+7: Ranks — muestra jugadores de todos (online y offline), permite asignar/remover.
    // FIX 7: Player detail muestra el rango y permite cambiarlo.
    public static void openRanksMain(ServerPlayerEntity sp) {
        SimpleInventory inv = new SimpleInventory(27);
        fill(inv, Items.MAGENTA_STAINED_GLASS_PANE, "§d ");
        item(inv, 4, Items.NETHER_STAR, "§d§l✦ Gestión de Rangos",
            "§7Selecciona un rango para asignar miembros.",
            "§8§lTrainer §8= rango base de todos (sin multiplicador).",
            "§8Los rangos afectan multiplicadores de CC.");
        // Trainer (base - informativo, no asignable)
        item(inv, 8, Items.GRAY_DYE,
            "§7§lTrainer §8(base)",
            "§7Rango que tienen todos los jugadores por defecto.",
            "§8Sin multiplicador — ×1.0 en misiones y AFK.",
            "§8No se puede asignar ni quitar.");
        item(inv, 10, Items.LIME_DYE,
            "§a§l▶ Trainer+",
            "§7Asignar rango §aTrainer+§7.",
            "§8Misiones: §a×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.TRAINER_PLUS, 1.5),
            "§8AFK: §a×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.TRAINER_PLUS, 1.3),
            "§7► Click para ver miembros.");
        item(inv, 12, Items.CYAN_DYE,
            "§b§l▶ Elite",
            "§7Asignar rango §bElite§7.",
            "§8Misiones: §b×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.ELITE, 1.9),
            "§8AFK: §b×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.ELITE, 1.8),
            "§7► Click para ver miembros.");
        item(inv, 14, Items.PURPLE_DYE,
            "§d§l▶ Legendary",
            "§7Asignar rango §dLegendary§7.",
            "§8Misiones: §d×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.LEGENDARY, 2.4),
            "§8AFK: §d×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.LEGENDARY, 2.0),
            "§7► Click para ver miembros.");
        item(inv, 16, Items.ORANGE_DYE,
            "§6§l▶ Mythical",
            "§7Asignar rango §6Mythical§7.",
            "§8Misiones: §6×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.MYTHICAL, 2.9),
            "§8AFK: §6×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.MYTHICAL, 2.7),
            "§7► Click para ver miembros.");
        item(inv, 26, Items.ARROW, "§7« Volver", "§7Regresa al panel de administración.");
        open(sp, inv, "ranks_main", 3, "§d✦ Gestión de Rangos");
    }

    // FIX 6: Show ALL tracked players (online + offline) in rank members
    public static void openRankMembers(ServerPlayerEntity sp, Rank rank) {
        detailCtx.put(sp.getUuid(), rank.name());
        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, Items.GRAY_STAINED_GLASS_PANE, "§8 ");
        String rankName = rankDisplay(rank);
        item(inv, 4, Items.NAME_TAG, rankName + " §7— Miembros",
            "§7Click en jugador conectado para agregar/quitar del rango.",
            "§8Los jugadores offline se listan pero no pueden gestionarse aquí.");

        // Online players first
        int slot = 9;
        for (ServerPlayerEntity target : sp.getServer().getPlayerManager().getPlayerList()) {
            if (slot >= 45) break;
            boolean inRank = RankManager.isInRank(rank, target.getUuid());
            Rank currentRank = RankManager.getRank(target);
            item(inv, slot, Items.PLAYER_HEAD,
                (inRank ? "§a§l[" + rankDisplay(rank) + "] " : "§7[Trainer base] ") + "§f" + target.getName().getString(),
                "§7Balance: §6" + MissionManager.format(EconomyStorage.getBalance(target.getUuid())) + " CC",
                "§7Rango actual: " + rankDisplay(currentRank),
                inRank ? "§c► Click para §cREMOVER §c(vuelve a Trainer base)." : "§a► Click para §aAGREGAR §aal rango.");
            slot++;
        }
        item(inv, 49, Items.BARRIER, "§c§l✘ Volver", "§7Regresa a la gestión de rangos.");
        open(sp, inv, "rank_members", 6, rankName + " — Miembros");
    }

    public static void openNpcConfig(ServerPlayerEntity sp) {
        SimpleInventory inv = new SimpleInventory(27);
        fill(inv, Items.ORANGE_STAINED_GLASS_PANE, "§6 ");
        String status = ModeconomiaConfig.DATA.questNpcUuid != null
            ? "§aActivo §7(§f" + ModeconomiaConfig.DATA.questNpcUuid.substring(0,8) + "...§7)"
            : "§cNo spawneado";
        item(inv, 4, Items.VILLAGER_SPAWN_EGG, "§6§lNPC Misiones", "§7Estado: " + status);
        item(inv, 11, Items.LIME_CONCRETE,
            "§a§l▶ Spawnear NPC",
            "§7Spawnea el NPC Misiones en tu posición actual.",
            "§8Sin IA, invulnerable, sin trades.",
            "§7► Click para spawnear.");
        item(inv, 13, Items.PAPER, "§e§lℹ Estado del NPC",
            "§7Estado: " + status,
            "§8UUID: §7" + (ModeconomiaConfig.DATA.questNpcUuid != null
                ? ModeconomiaConfig.DATA.questNpcUuid : "ninguno"));
        item(inv, 15, Items.RED_CONCRETE,
            "§c§l✘ Remover NPC",
            "§7Elimina el NPC Misiones del mundo.",
            "§7► Click para remover.");
        item(inv, 22, Items.ARROW, "§7« Volver", "§7Regresa al panel de administración.");
        open(sp, inv, "npc_config", 3, "§6✦ NPC Misiones");
    }

    // ══════════════════════════════════════════════════════════════
    // PANEL ADMIN — CATEGORÍAS CUSTOM DE TIENDA
    // ══════════════════════════════════════════════════════════════

    public static void openCustomCategoriesAdmin(ServerPlayerEntity sp) {
        openCustomCategoriesAdmin(sp, 0);
    }

    public static void openCustomCategoriesAdmin(ServerPlayerEntity sp, int page) {
        SimpleInventory inv = new SimpleInventory(54);
        for (int i = 0; i < 9;  i++) item(inv, i,    Items.PURPLE_STAINED_GLASS_PANE, "§5 ");
        for (int i = 45; i < 54; i++) item(inv, i,    Items.PURPLE_STAINED_GLASS_PANE, "§5 ");
        for (int row = 1; row < 5; row++) {
            item(inv, row * 9,      Items.BLACK_STAINED_GLASS_PANE, "§8 ");
            item(inv, row * 9 + 8,  Items.BLACK_STAINED_GLASS_PANE, "§8 ");
        }

        List<ModeconomiaConfig.CustomCategory> cats =
            ModeconomiaConfig.DATA.shop.customCategories != null
            ? ModeconomiaConfig.DATA.shop.customCategories : new java.util.ArrayList<>();

        // Slots interiores: 10-16, 19-25, 28-34, 37-43 → 28 slots por página
        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        int pageSize = slots.length; // 28 por página
        int totalPages = Math.max(1, (int) Math.ceil(cats.size() / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages - 1));
        int start = page * pageSize;

        item(inv, 4, Items.BOOKSHELF,
            "§5§l✦ Categorías Custom de Tienda §8(pág " + (page+1) + "/" + totalPages + ")",
            "§7Crea categorías nuevas que aparecen en la tienda.",
            "§8Máx. 14 categorías custom.",
            "§7El §eícono §7de cada categoría es el item que tenías en la §emano §7al crearla.");

        for (int i = 0; i < pageSize && (start + i) < cats.size(); i++) {
            ModeconomiaConfig.CustomCategory cc = cats.get(start + i);
            String color = cc.color != null ? cc.color : "§f";
            String desc  = (cc.description != null && !cc.description.isEmpty()) ? cc.description : "§8Sin descripción.";

            // Use saved icon NBT if available, otherwise fallback to BOOKSHELF
            ItemStack icon;
            if (cc.iconNbt != null && !cc.iconNbt.isEmpty()) {
                icon = ItemStackUtil.fromNbtString(cc.iconNbt, sp.getServer().getRegistryManager());
                if (icon.isEmpty()) icon = new ItemStack(Items.BOOKSHELF);
            } else {
                icon = new ItemStack(Items.BOOKSHELF);
            }
            icon.set(DataComponentTypes.CUSTOM_NAME, Text.literal(color + "§l" + cc.label));
            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal("§7" + desc));
            lore.add(Text.literal("§8ID: §7" + cc.id));
            lore.add(Text.literal("§8Color: §7" + cc.color));
            lore.add(Text.literal("§c§l✘ Click para eliminar esta categoría."));
            icon.set(DataComponentTypes.LORE, new LoreComponent(lore));
            inv.setStack(slots[i], icon);
        }

        // Navegación
        if (page > 0)
            item(inv, 46, Items.ARROW, "§7« Página anterior", "§7Página " + page + "/" + totalPages);
        if (page < totalPages - 1)
            item(inv, 52, Items.ARROW, "§7Página siguiente »", "§7Página " + (page+2) + "/" + totalPages);

        // Botón crear nueva categoría
        if (cats.size() < 14) {
            item(inv, 49, Items.LIME_DYE,
                "§a§l+ Crear categoría nueva",
                "§7Sostén el §eitem que quieres como ícono §7en la mano.",
                "§8Luego escribe el nombre, descripción y color.",
                "§8Se usará como ID y nombre visible.");
        } else {
            item(inv, 49, Items.BARRIER, "§c§lMáximo de categorías alcanzado", "§8(14/14)");
        }

        item(inv, 45, Items.ARROW, "§7« Volver al panel admin", "");
        detailCtx.put(sp.getUuid(), "custom_cats:" + page);
        open(sp, inv, "custom_cats", 6, "§5✦ Categorías Custom");
    }

    private void handleCustomCategoriesAdmin(ServerPlayerEntity sp, int slot) {
        List<ModeconomiaConfig.CustomCategory> cats = ModeconomiaConfig.DATA.shop.customCategories;
        if (cats == null) {
            cats = new java.util.ArrayList<>();
            ModeconomiaConfig.DATA.shop.customCategories = cats;
        }

        // Recover page from context
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "custom_cats:0");
        int page = 0;
        try {
            if (ctx.startsWith("custom_cats:")) page = Integer.parseInt(ctx.substring(12));
        } catch (Exception ignored) {}

        if (slot == 45) { openAdminMain(sp); return; }

        // Navegación
        if (slot == 46 && page > 0) { openCustomCategoriesAdmin(sp, page - 1); return; }
        if (slot == 52) {
            int[] innerSlots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
            int totalPages = Math.max(1, (int) Math.ceil(cats.size() / (double) innerSlots.length));
            if (page < totalPages - 1) { openCustomCategoriesAdmin(sp, page + 1); return; }
        }

        // Botón crear nueva categoría
        if (slot == 49) {
            if (cats.size() >= 14) { openCustomCategoriesAdmin(sp, page); return; }
            // Capture item in hand as icon
            ItemStack hand = sp.getMainHandStack();
            String iconNbt = hand.isEmpty() ? "" : ItemStackUtil.toNbtString(hand.copy(), sp.getServer().getRegistryManager());
            if (!hand.isEmpty()) {
                sp.sendMessage(Text.literal("§a✔ Ícono guardado: §f" + hand.getName().getString()), false);
            } else {
                sp.sendMessage(Text.literal("§e⚠ No tienes ningún item en la mano. Se usará un ícono por defecto."), false);
            }
            sp.sendMessage(Text.literal("§5§l✦ §dCategoría nueva: §7Escribe el §bnombre §7de la categoría en el chat."), false);
            sp.sendMessage(Text.literal("§8(Escribe §fcancel §8para cancelar)"), false);
            awaitingInput.put(sp.getUuid(), "new_custom_cat_name§§" + iconNbt);
            return;
        }

        // Click en categoría existente → eliminar
        int[] innerSlots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        int start = page * innerSlots.length;
        for (int i = 0; i < innerSlots.length; i++) {
            if (innerSlots[i] == slot) {
                int realIdx = start + i;
                if (realIdx < cats.size()) {
                    ModeconomiaConfig.CustomCategory removed = cats.remove(realIdx);
                    ModeconomiaConfig.save();
                    sp.sendMessage(Text.literal("§a✔ Categoría §f" + removed.label + " §aeliminada."), false);
                    openCustomCategoriesAdmin(sp, page);
                }
                return;
            }
        }
    }

    public static void openShopNpcConfig(ServerPlayerEntity sp) {
        SimpleInventory inv = new SimpleInventory(27);
        fill(inv, Items.CYAN_STAINED_GLASS_PANE, "§3 ");
        String status = ModeconomiaConfig.DATA.shopNpcUuid != null
            ? "§aActivo §7(§f" + ModeconomiaConfig.DATA.shopNpcUuid.substring(0,8) + "...§7)"
            : "§cNo spawneado";
        item(inv, 4, Items.CHEST, "§b§lNPC Tienda", "§7Estado: " + status);
        item(inv, 11, Items.LIME_CONCRETE,
            "§a§l▶ Spawnear NPC",
            "§7Spawnea el NPC Tienda en tu posición actual.",
            "§8Sin IA, invulnerable, sin trades.",
            "§7► Click para spawnear.");
        item(inv, 13, Items.PAPER, "§e§lℹ Estado del NPC",
            "§7Estado: " + status,
            "§8UUID: §7" + (ModeconomiaConfig.DATA.shopNpcUuid != null
                ? ModeconomiaConfig.DATA.shopNpcUuid : "ninguno"));
        item(inv, 15, Items.RED_CONCRETE,
            "§c§l✘ Remover NPC",
            "§7Elimina el NPC Tienda del mundo.",
            "§7► Click para remover.");
        item(inv, 22, Items.ARROW, "§7« Volver", "§7Regresa al panel de administración.");
        open(sp, inv, "shop_npc_config", 3, "§b✦ NPC Tienda");
    }

    public static void openPlayerList(ServerPlayerEntity sp, int page) {
        List<String> uuids = EconomyStorage.getAllPlayerUuids();
        uuids.sort((a, b) -> Double.compare(
            EconomyStorage.getBalance(toUUID(b)),
            EconomyStorage.getBalance(toUUID(a))));
        int pageSize = 28;
        int total = uuids.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int start = page * pageSize;
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};

        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, Items.GRAY_STAINED_GLASS_PANE, "§8 ");
        item(inv, 4, Items.PLAYER_HEAD,
            "§f§l⬤ Jugadores §7(" + total + " registrados)",
            "§7Página §f" + (page+1) + "§7/§f" + totalPages,
            "§8Ordenados por balance descendente.",
            "§7► Click en un jugador para ver detalles.");
        for (int i = 0; i < slots.length; i++) {
            int idx = start + i;
            if (idx >= total) break;
            try {
                UUID uuid = toUUID(uuids.get(idx));
                PlayerMissionData data = EconomyStorage.getMissionData(uuid);
                double balance = EconomyStorage.getBalance(uuid);
                String name = data.playerName != null && !data.playerName.isEmpty()
                    ? data.playerName : uuids.get(idx).substring(0,8);
                String col = balance >= 100 ? "§6" : balance >= 10 ? "§e" : "§f";
                // FIX 6: show rank in player list
                Rank r = getRankByUuid(sp, uuid);
                item(inv, slots[i], Items.PLAYER_HEAD,
                    col + "§l" + name,
                    "§7Balance: §6" + MissionManager.format(balance) + " CC",
                    "§7Rango: " + rankDisplay(r),
                    "§7Misiones hoy: §a" + data.completed.size(),
                    "§7Semana: §b" + data.weeklyCompleted + " §8| Mes: §d" + data.monthlyCompleted,
                    "§8Último acceso: §7" + (data.lastSeenDate.isEmpty() ? "desconocido" : data.lastSeenDate),
                    "§7► Click para gestionar.");
            } catch (Exception ignored) {}
        }
        if (page > 0)
            item(inv, 45, Items.ARROW, "§7« Anterior", "§7Página " + page + "/" + totalPages);
        item(inv, 49, Items.PAPER, "§ePágina §f" + (page+1) + "§e/§f" + totalPages,
            "§7Total: §f" + total + " §7jugadores registrados.");
        if (page < totalPages - 1)
            item(inv, 53, Items.ARROW, "§7Siguiente »", "§7Página " + (page+2) + "/" + totalPages);
        item(inv, 47, Items.BARRIER, "§c§l✘ Volver", "§7Regresa al panel de administración.");
        detailCtx.put(sp.getUuid(), "page:" + page);
        open(sp, inv, "player_list", 6, "§f⬤ Lista de Jugadores");
    }

    // FIX 3+7: Player detail — allow editing name, desc, reward; show rank; allow rank change
    public static void openPlayerDetail(ServerPlayerEntity sp, String uuidStr) {
        detailCtx.put(sp.getUuid(), "detail:" + uuidStr);
        try {
            UUID uuid = toUUID(uuidStr);
            PlayerMissionData data = EconomyStorage.getMissionData(uuid);
            double balance = EconomyStorage.getBalance(uuid);
            String name = data.playerName != null && !data.playerName.isEmpty()
                ? data.playerName : uuidStr.substring(0,8);
            Rank rank = getRankByUuid(sp, uuid);

            SimpleInventory inv = new SimpleInventory(54);
            fill(inv, Items.GRAY_STAINED_GLASS_PANE, "§8 ");
            item(inv, 4, Items.PLAYER_HEAD, "§6§l" + name,
                "§7Gestionando al jugador §e" + name,
                "§7Rango: " + rankDisplay(rank));
            item(inv, 10, Items.GOLD_INGOT,
                "§6§lBalance: §f" + MissionManager.format(balance) + " CC",
                "§7Balance actual del jugador.",
                "§8Usa los botones de la derecha para modificar.");
            item(inv, 19, Items.WRITABLE_BOOK,
                "§b§lMisiones",
                "§7Hoy: §a" + data.completed.size(),
                "§7Semana: §b" + data.weeklyCompleted,
                "§7Mes: §d" + data.monthlyCompleted,
                "§7Total histórico: §f" + data.totalDailyCompleted);
            item(inv, 28, Items.CLOCK,
                "§e§lTiempo AFK: §f" + formatAfk(data.totalAfkMinutes),
                "§7Total CC ganados: §6" + MissionManager.format(data.totalEarned));
            item(inv, 37, Items.NETHER_STAR,
                "§d§lEstadísticas",
                "§7Último acceso: §f" + (data.lastSeenDate.isEmpty() ? "desconocido" : data.lastSeenDate),
                "§7UUID: §8" + uuidStr.substring(0,13) + "...");
            // Economy actions
            item(inv, 14, Items.LIME_CONCRETE,
                "§a§l[+] Dar CobbleCoins",
                "§7Agrega CC a §e" + name + "§7.",
                "§7► Click para continuar.");
            item(inv, 23, Items.RED_CONCRETE,
                "§c§l[-] Quitar CobbleCoins",
                "§7Resta CC a §e" + name + "§7.",
                "§7► Click para continuar.");
            item(inv, 32, Items.GOLD_BLOCK,
                "§e§l[=] Asignar Balance",
                "§7Define el balance exacto de §e" + name + "§7.",
                "§7► Click para continuar.");
            item(inv, 41, Items.BARRIER,
                "§c§l✘ Resetear Misiones",
                "§7Reinicia las misiones diarias de §e" + name + "§7.",
                "§8Podrá completarlas de nuevo hoy.");
            // FIX 7: Rank management in player detail
            item(inv, 16, Items.NAME_TAG,
                "§d§lCambiar Rango",
                "§7Rango actual: " + rankDisplay(rank),
                "§7► Click para cambiar rango (cicla entre rangos).",
                "§8Orden: Sin rango → Trainer+ → Elite → Legendary → Mythical → Sin rango");
            item(inv, 25, Items.BARRIER,
                "§c§lRemover Rango",
                "§7Quita el rango actual de §e" + name + "§7.",
                "§8Solo si ya tiene un rango asignado.",
                "§7► Click para remover.");
            item(inv, 49, Items.ARROW, "§7« Volver a la lista", "§7Regresa a la lista de jugadores.");
            open(sp, inv, "player_detail", 6, "§6✦ Detalle: " + name);
        } catch (Exception e) {
            openPlayerList(sp, 0);
        }
    }

    // ── Shop ──
    private static final Map<UUID, ItemStack> sellItemCtx  = new HashMap<>();
    private static final Map<UUID, Double>    sellPriceCtx = new HashMap<>();
    private static final Map<UUID, Integer>   sellDurCtx   = new HashMap<>();

    public static void openShopSell(ServerPlayerEntity sp, ItemStack stack) {
        sellItemCtx.put(sp.getUuid(), stack);
        sellPriceCtx.putIfAbsent(sp.getUuid(), 1.0);
        sellDurCtx.putIfAbsent(sp.getUuid(), 60);
        double price = sellPriceCtx.get(sp.getUuid());
        int dur = sellDurCtx.get(sp.getUuid());
        SimpleInventory inv = new SimpleInventory(27);
        fill(inv, Items.GRAY_STAINED_GLASS_PANE, "§8 ");
        ItemStack display = stack.copy();
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("§7Este es el item que venderás."));
        display.set(DataComponentTypes.LORE, new LoreComponent(lore));
        inv.setStack(13, display);
        item(inv, 10, Items.GOLD_INGOT,
            "§6§lPrecio: §f" + MissionManager.format(price) + " CC",
            "§8Click §aizq §8= +1 | Click §cder §8= -1",
            "§8Shift: +/-10");
        item(inv, 12, Items.CLOCK,
            "§e§lDuración: §f" + dur + " min",
            "§8Click §aizq §8= +15 min | Click §cder §8= -15 min");
        item(inv, 15, Items.LIME_CONCRETE,
            "§a§l✔ Listar item",
            "§7Precio: §6" + MissionManager.format(price) + " CC",
            "§7Duración: §f" + dur + " min",
            "§7► Click para publicar en la tienda.");
        item(inv, 17, Items.BARRIER, "§c§l✘ Cancelar", "§7Vuelve a la tienda.");
        open(sp, inv, "shop_sell", 3, "§6✦ Vender item");
    }

    public static void openShopPriceEdit(ServerPlayerEntity sp, int slot) {
        openShopPriceEdit(sp, slot, 0);
    }

    public static void openShopPriceEdit(ServerPlayerEntity sp, int shopSlotNum, int catPage) {
        ShopItem existing = ModeconomiaConfig.DATA.shop.items.stream()
            .filter(i -> i.slot == shopSlotNum).findFirst().orElse(null);
        if (existing == null) { openShopConfig(sp); return; }
        detailCtx.put(sp.getUuid(), "shopslot:" + shopSlotNum + ":p" + catPage);
        double price = existing.price;
        String cat = existing.category != null ? existing.category : "general";

        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, Items.YELLOW_STAINED_GLASS_PANE, "§e ");
        for (int i = 27; i < 54; i++) item(inv, i, Items.BLACK_STAINED_GLASS_PANE, "§8 ");

        item(inv, 4, Items.GOLD_BLOCK,
            "§6§lEditar item — §f" + MissionManager.format(price) + " CC",
            "§7Categoría actual: §e" + cat,
            "§8Ajusta precio y categoría. §8[Pág cat: " + (catPage+1) + "]");

        // Price controls
        item(inv, 10, Items.RED_STAINED_GLASS_PANE,  "§c§l-100", "§7Resta §c100.");
        item(inv, 11, Items.RED_STAINED_GLASS_PANE,  "§c§l-10",  "§7Resta §c10.");
        item(inv, 12, Items.RED_STAINED_GLASS_PANE,  "§c§l-1",   "§7Resta §c1.");
        item(inv, 13, Items.RED_STAINED_GLASS_PANE,  "§c§l-0.1", "§7Resta §c0.1.");
        item(inv, 14, Items.LIME_STAINED_GLASS_PANE, "§a§l+0.1", "§7Suma §a0.1.");
        item(inv, 15, Items.LIME_STAINED_GLASS_PANE, "§a§l+1",   "§7Suma §a1.");
        item(inv, 16, Items.LIME_STAINED_GLASS_PANE, "§a§l+10",  "§7Suma §a10.");
        item(inv, 17, Items.LIME_STAINED_GLASS_PANE, "§a§l+100", "§7Suma §a100.");

        List<ModeconomiaConfig.CustomCategory> customCats =
            ModeconomiaConfig.DATA.shop.customCategories != null
            ? ModeconomiaConfig.DATA.shop.customCategories : java.util.Collections.emptyList();

        if (catPage == 0) {
            // Página 1 — categorías predeterminadas
            item(inv, 28, Items.PURPLE_DYE,    "§d§lPokébolas",   cat.equals("pokeballs")   ? "§a✔" : "§7Click.");
            item(inv, 29, Items.SWEET_BERRIES, "§a§lBayas",       cat.equals("bayas")       ? "§a✔" : "§7Click.");
            item(inv, 30, Items.BLAZE_POWDER,  "§e§lVitaminas",   cat.equals("vitaminas")   ? "§a✔" : "§7Click.");
            item(inv, 31, Items.CYAN_DYE,      "§b§lPP",          cat.equals("pp")          ? "§a✔" : "§7Click.");
            item(inv, 32, Items.BROWN_DYE,     "§6§lBonguris",    cat.equals("bonguris")    ? "§a✔" : "§7Click.");
            item(inv, 33, Items.DIAMOND,       "§9§lEvoluciones", cat.equals("evoluciones") ? "§a✔" : "§7Click.");
            item(inv, 34, Items.CHEST,         "§f§lGeneral",     cat.equals("general")     ? "§a✔" : "§7Click.");
            item(inv, 37, Items.PINK_DYE,      "§d§lCaramelos",   cat.equals("caramelos")   ? "§a✔" : "§7Click.");
            item(inv, 38, Items.GREEN_DYE,     "§a§lMentas",      cat.equals("mentas")      ? "§a✔" : "§7Click.");
            item(inv, 39, Items.MAGENTA_DYE,   "§5§lMTs/TMs",     cat.equals("tms")         ? "§a✔" : "§7Click.");
            item(inv, 40, Items.IRON_SWORD,    "§c§lBatalla",     cat.equals("batalla")     ? "§a✔" : "§7Click.");
            item(inv, 41, Items.AMETHYST_SHARD,"§5§lGemas",       cat.equals("gemas")       ? "§a✔" : "§7Click.");
            item(inv, 42, Items.NETHER_STAR,   "§e§lInvocadores", cat.equals("invocadores") ? "§a✔" : "§7Click.");
            item(inv, 43, Items.ORANGE_DYE,    "§6§lCurrys",      cat.equals("currys")      ? "§a✔" : "§7Click.");

            // Botón para ir a página de custom cats si existen
            if (!customCats.isEmpty()) {
                item(inv, 44, Items.BOOKSHELF,
                    "§5§l★ Categorías Custom »",
                    "§7Ver las " + customCats.size() + " categoría(s) custom.",
                    "§7► Click para ver página de custom.");
            }
        } else {
            // Página 2+ — categorías custom, 14 por página en slots 28-43
            int[] catSlots = {28,29,30,31,32,33,34,37,38,39,40,41,42,43};
            int catPageSize = catSlots.length;
            int catTotal = customCats.size();
            int catTotalPages = Math.max(1, (int) Math.ceil(catTotal / (double) catPageSize));
            int catStart = (catPage - 1) * catPageSize;

            for (int i = 0; i < catPageSize && (catStart + i) < catTotal; i++) {
                ModeconomiaConfig.CustomCategory cc = customCats.get(catStart + i);
                String color = cc.color != null ? cc.color : "§f";
                boolean active = cc.id.equals(cat);
                String desc = (cc.description != null && !cc.description.isEmpty()) ? "§7" + cc.description : "";

                ItemStack icon;
                if (cc.iconNbt != null && !cc.iconNbt.isEmpty()) {
                    icon = ItemStackUtil.fromNbtString(cc.iconNbt, sp.getServer().getRegistryManager());
                    if (icon.isEmpty()) icon = new ItemStack(Items.BOOKSHELF);
                } else {
                    icon = new ItemStack(Items.BOOKSHELF);
                }
                icon.set(DataComponentTypes.CUSTOM_NAME, Text.literal(color + "§l" + cc.label + (active ? " §a✔" : "")));
                List<Text> lore = new ArrayList<>();
                if (!desc.isEmpty()) lore.add(Text.literal(desc));
                lore.add(Text.literal("§8ID: §7" + cc.id));
                lore.add(Text.literal(active ? "§a► Categoría activa" : "§7► Click para asignar."));
                icon.set(DataComponentTypes.LORE, new LoreComponent(lore));
                inv.setStack(catSlots[i], icon);
            }

            // Navegación de páginas custom
            if (catPage > 1)
                item(inv, 27, Items.ARROW, "§7« Anterior", "§7Página " + catPage + "/" + catTotalPages);
            if (catPage < catTotalPages)
                item(inv, 35, Items.ARROW, "§7Siguiente »", "§7Página " + (catPage+2) + "/" + catTotalPages);

            // Volver a predeterminadas
            item(inv, 44, Items.ARROW, "§7« Categorías predeterminadas", "§7Vuelve a la página 1.");
        }

        item(inv, 49, Items.EMERALD,
            "§a§l✔ Guardar: §6" + MissionManager.format(price) + " CC §8[" + cat + "]",
            "§7Guarda el precio y la categoría.");
        item(inv, 53, Items.BARRIER, "§c§l✘ Volver", "§7Vuelve a la config de tienda.");
        open(sp, inv, "shop_price_edit", 6, "§6✦ Editar item de tienda");
    }

    // FIX 8+9: Shop config — owner can add items by holding in hand + clicking empty slot,
    // and the view updates live.
    public static void openShopConfig(ServerPlayerEntity sp) {
        openShopConfig(sp, 0);
    }

    public static void openShopConfig(ServerPlayerEntity sp, int page) {
        List<ShopItem> allItems = new ArrayList<>(ModeconomiaConfig.DATA.shop.items);
        // Sort by slot for consistent display
        allItems.sort(java.util.Comparator.comparingInt(i -> i.slot));

        int pageSize   = 45;
        int total      = allItems.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages - 1));
        int start = page * pageSize;

        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, Items.BLACK_STAINED_GLASS_PANE, "§8 ");

        // Show items for this page in display slots 0-44
        for (int i = 0; i < pageSize && (start + i) < total; i++) {
            ShopItem shopItem = allItems.get(start + i);
            ItemStack stack = ItemStackUtil.fromNbtString(shopItem.itemNbt,
                sp.getServer().getRegistryManager());
            if (stack.isEmpty()) continue;
            String priceLabel = MissionManager.format(shopItem.price);
            String cat = shopItem.category != null ? shopItem.category : "general";
            String configDisplayName = stack.getName().getString();
            stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§f§l" + configDisplayName + " §7- §6" + priceLabel + " CC"));
            stack.remove(DataComponentTypes.LORE);
            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal("§7Precio: §6" + priceLabel + " CobbleCoins"));
            lore.add(Text.literal("§8Categoría: §7" + cat));
            lore.add(Text.literal("§8Click §aizq §8= editar precio/categoría."));
            lore.add(Text.literal("§8Click §cder §8= eliminar."));
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
            inv.setStack(i, stack);
        }

        // Bottom bar
        if (page > 0)
            item(inv, 45, Items.ARROW, "§7« Anterior", "§7Página " + page + "/" + totalPages);
        item(inv, 46, Items.LIME_CONCRETE,
            "§a§l+ Agregar item",
            "§7Sostén el item en la mano y haz click.",
            "§7► Se agrega al final de la lista.");
        item(inv, 47, Items.HOPPER,
            "§b§l↺ Repoblar Cobblemon",
            "§7Agrega los items de Cobblemon que falten.",
            "§a► Click para repoblar.");
        item(inv, 48, Items.TNT,
            "§c§l🗑 Limpiar tienda",
            "§7Elimina TODOS los items de la tienda.",
            "§c► Click para limpiar.");
        item(inv, 49, Items.CYAN_STAINED_GLASS_PANE,
            "§b§lConfig Tienda §8[" + (page+1) + "/" + totalPages + "]",
            "§7" + total + " items en total.");
        item(inv, 53, Items.BARRIER, "§c§l✘ Volver", "§7Regresa al panel de administración.");
        if (page < totalPages - 1)
            item(inv, 53, Items.ARROW, "§7Siguiente »", "§7Página " + (page+2) + "/" + totalPages);

        detailCtx.put(sp.getUuid(), "shopconfig:" + page);
        open(sp, inv, "shop_config", 6, "§d✦ Config Tienda §8[" + (page+1) + "/" + totalPages + "]");
    }

    // ══════════════════════════════════════
    // HANDLERS DE CLICK
    // ══════════════════════════════════════

    private void handlePlayerMenu(ServerPlayerEntity sp, int slot) {
        if (slot == 31) {
            // Si hay NPC Tienda configurado → no abre nada, solo informar
            if (ModeconomiaConfig.DATA.shopNpcUuid != null) {
                sp.closeHandledScreen();
                sp.sendMessage(net.minecraft.text.Text.literal(
                    "§e✦ §7Habla con el §b§lTienda §7para acceder a la tienda."), false);
            } else {
                // Sin NPC Tienda → abre directamente (fallback para cuando no hay NPC)
                openShopSelector(sp);
            }
        }
        else if (slot == 37) {
            // Canjear CobbleCoins físicas → balance
            sp.closeHandledScreen();
            int redeemed = com.cobblemania.economia.item.CoinRewardHelper.redeemCoins(sp);
            if (redeemed == 0) {
                sp.sendMessage(net.minecraft.text.Text.literal(
                    "§e⚠ No tienes §6CobbleCoins §efísicas en el inventario."), false);
            }
        }
        else if (slot == 40) sp.closeHandledScreen();
    }

    // FIX 1: Handle mission selection from NPC view
    private void handleMissionsView(ServerPlayerEntity sp, int slot) {
        // Recuperar página actual
        String pageCtx = detailCtx.getOrDefault(sp.getUuid(), "mvpage:0");
        int page = 0;
        try { page = Integer.parseInt(pageCtx.replace("mvpage:", "")); } catch (Exception ignored) {}

        if (slot == 49) { sp.closeHandledScreen(); return; } // Cerrar
        if (slot == 45) { openMissionsView(sp, Math.max(0, page - 1)); return; } // Anterior
        if (slot == 53) { // Siguiente
            List<MissionDefinition> defsNav = ModeconomiaConfig.DATA.missions.definitions;
            List<MissionDefinition> activeNav = new ArrayList<>();
            if (defsNav != null) for (MissionDefinition d : defsNav) if (d.active && !d.isExpired()) activeNav.add(d);
            int totalPagesNav = Math.max(1, (int) Math.ceil(activeNav.size() / 28.0));
            openMissionsView(sp, Math.min(totalPagesNav - 1, page + 1));
            return;
        }

        // Misiones en slots 9-36 (28 por página)
        int idx = slot - 9;
        if (idx < 0 || idx >= 28) return;
        int realIdx = page * 28 + idx;

        List<MissionDefinition> defs = ModeconomiaConfig.DATA.missions.definitions;
        if (defs == null) return;
        List<MissionDefinition> active = new ArrayList<>();
        for (MissionDefinition d : defs) if (d.active && !d.isExpired()) active.add(d);
        if (realIdx >= active.size()) return;
        MissionDefinition chosen = active.get(realIdx);
        PlayerMissionData data = EconomyStorage.getMissionData(sp.getUuid());
        if (data.completed.contains(chosen.id)) {
            sp.sendMessage(Text.literal("§c✘ Ya completaste esta misión hoy."), false);
            openMissionsView(sp, page);
            return;
        }
        if (chosen.id.equals(data.activeMissionId)) {
            sp.sendMessage(Text.literal("§e⚠ Ya tienes esta misión en curso."), false);
        } else {
            MissionManager.selectMission(sp, chosen.id);
        }
        openMissionsView(sp, page);
    }

    private void handleAdminMain(ServerPlayerEntity sp, int slot) {
        switch (slot) {
            case 10 -> openEconomyAction(sp);
            case 12 -> openMissionsConfig(sp);
            case 14 -> openAfkConfig(sp);
            case 16 -> openShopConfig(sp);
            case 18 -> openCustomCategoriesAdmin(sp);
            case 20 -> openNpcConfig(sp);
            case 21 -> openShopNpcConfig(sp);
            case 22 -> openRanksMain(sp);
            case 24 -> openPlayerList(sp, 0);
            case 26 -> sp.closeHandledScreen();
        }
    }

    private void handleEconomyAction(ServerPlayerEntity sp, int slot) {
        switch (slot) {
            case 10 -> { amountCtx.put(sp.getUuid(), new double[]{0, 0}); openEconomyPlayer(sp, 0); }
            case 12 -> { amountCtx.put(sp.getUuid(), new double[]{0, 1}); openEconomyPlayer(sp, 1); }
            case 14 -> { amountCtx.put(sp.getUuid(), new double[]{0, 2}); openEconomyPlayer(sp, 2); }
            case 16 -> { amountCtx.put(sp.getUuid(), new double[]{0, 3}); openEconomyPlayer(sp, 3); }
            case 22 -> openAdminMain(sp);
        }
    }

    private void handleEconomyPlayer(ServerPlayerEntity sp, int slot) {
        if (slot == 49) { openEconomyAction(sp); return; }
        List<ServerPlayerEntity> players = sp.getServer().getPlayerManager().getPlayerList();
        int idx = slot - 9;
        if (idx >= 0 && idx < players.size()) {
            ServerPlayerEntity target = players.get(idx);
            double[] ctx = amountCtx.getOrDefault(sp.getUuid(), new double[]{0, 0});
            int action = (int) ctx[1];
            if (action == 3) {
                double bal = EconomyStorage.getBalance(target.getUuid());
                sp.sendMessage(Text.literal("§e✦ Balance de §f" + target.getName().getString()
                    + "§e: §6" + MissionManager.format(bal) + " CC"), false);
                openEconomyAction(sp);
            } else {
                targetCtx.put(sp.getUuid(), target.getUuid());
                amountCtx.put(sp.getUuid(), new double[]{0, action});
                openEconomyAmount(sp);
            }
        }
    }

    private void handleEconomyAmount(ServerPlayerEntity sp, int slot, int button) {
        double[] ctx = amountCtx.getOrDefault(sp.getUuid(), new double[]{0, 0});
        double delta = switch (slot) {
            case 10 -> -100; case 11 -> -10; case 12 -> -1; case 13 -> -0.1;
            case 15 -> 0.1;  case 16 -> 1;  case 17 -> 10;  case 18 -> 100;
            default -> 0;
        };
        if (delta != 0) {
            ctx[0] = Math.max(0, ctx[0] + delta);
            amountCtx.put(sp.getUuid(), ctx);
            openEconomyAmount(sp);
            return;
        }
        if (slot == 22) {
            UUID target = targetCtx.get(sp.getUuid());
            if (target != null) {
                switch ((int) ctx[1]) {
                    case 0 -> EconomyStorage.addBalance(target, ctx[0]);
                    case 1 -> EconomyStorage.takeBalance(target, ctx[0]);
                    case 2 -> EconomyStorage.setBalance(target, ctx[0]);
                }
                sp.sendMessage(Text.literal("§a✔ Acción aplicada correctamente."), false);
            }
            String dCtx = detailCtx.getOrDefault(sp.getUuid(), "");
            if (dCtx.startsWith("detail:")) openPlayerDetail(sp, dCtx.substring(7));
            else openEconomyAction(sp);
        } else if (slot == 26) {
            String dCtx = detailCtx.getOrDefault(sp.getUuid(), "");
            if (dCtx.startsWith("detail:")) openPlayerDetail(sp, dCtx.substring(7));
            else openEconomyAction(sp);
        }
    }

    private void handleMissionsConfig(ServerPlayerEntity sp, int slot, int button) {
        switch (slot) {
            case 10 -> { double d = button == 0 ? 0.1 : -0.1;
                ModeconomiaConfig.DATA.missions.baseReward = Math.max(0, ModeconomiaConfig.DATA.missions.baseReward + d);
                ModeconomiaConfig.save(); openMissionsConfig(sp); }
            case 12 -> { int d = button == 0 ? 1 : -1;
                ModeconomiaConfig.DATA.missions.dailyCount = Math.max(1, ModeconomiaConfig.DATA.missions.dailyCount + d);
                ModeconomiaConfig.save(); openMissionsConfig(sp); }
            case 14 -> openMissionsList(sp);
            case 16 -> {
                if (setNpcFromLook(sp)) sp.sendMessage(Text.literal("§a✔ NPC de misiones actualizado."), false);
                else sp.sendMessage(Text.literal("§c✘ Mira a un aldeano para asignarlo."), false);
                openMissionsConfig(sp); }
            case 20, 21, 22, 23 -> {
                Rank r = slot == 20 ? Rank.TRAINER_PLUS : slot == 21 ? Rank.ELITE : slot == 22 ? Rank.LEGENDARY : Rank.MYTHICAL;
                double d = button == 0 ? 0.1 : -0.1;
                double cur = ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(r, 1.0);
                ModeconomiaConfig.DATA.missions.multipliers.put(r, Math.max(1.0, cur + d));
                ModeconomiaConfig.save(); openMissionsConfig(sp); }
            case 26 -> openAdminMain(sp);
        }
    }

    // FIX 4+5: No predefined types forced. Add button always visible.
    // Left click on mission → if expired: reactivate; else: show edit submenu
    // Right click → delete
    private void handleMissionsList(ServerPlayerEntity sp, int slot, int button) {
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "misspage:0");
        int page = 0;
        try { if (ctx.startsWith("misspage:")) page = Integer.parseInt(ctx.substring(9)); } catch (Exception ignored) {}

        List<MissionDefinition> defs = ModeconomiaConfig.DATA.missions.definitions;
        if (defs == null) defs = new ArrayList<>();

        // Navigation
        if (slot == 45 && page > 0) { openMissionsList(sp, page - 1); return; }
        if (slot == 53) {
            int totalPages = Math.max(1, (int)Math.ceil(defs.size() / 18.0));
            if (page < totalPages - 1) { openMissionsList(sp, page + 1); return; }
        }
        if (slot == 47) { openMissionsConfig(sp); return; }

        // FIX 5: Add new mission button
        if (slot == 49) {
            sp.sendMessage(Text.literal("§6§l✦ §eCREAR NUEVA MISIÓN"), false);
            sp.sendMessage(Text.literal("§7Paso 1/5 — Escribe el §enombre §7de la misión:"), false);
            sp.sendMessage(Text.literal("§7(escribe §fcancel §7para cancelar)"), false);
            awaitingInput.remove(sp.getUuid()); // type is now picked via GUI
            openMissionTypePicker(sp);
            return;
        }

        // Mission slots 9-26
        int pageSize = 18;
        int start = page * pageSize;
        int idx = slot - 9;
        if (idx >= 0 && idx < pageSize) {
            int realIdx = start + idx;
            if (realIdx >= defs.size()) return;
            MissionDefinition def = defs.get(realIdx);
            if (button == 1) {
                // Right click = delete
                defs.remove(realIdx);
                ModeconomiaConfig.save();
                sp.sendMessage(Text.literal("§a✔ Misión §f" + def.displayName + " §aeliminada."), false);
                openMissionsList(sp, page);
            } else {
                // Left click = if expired, reactivate; if active, open edit menu
                if (def.isExpired() || !def.active) {
                    sp.sendMessage(Text.literal("§6§l✦ §eReactivando misión: §f" + def.displayName), false);
                    sp.sendMessage(Text.literal("§7¿Duración? Escribe §e24 §7para 24h, o §emanual§7:"), false);
                    sp.sendMessage(Text.literal("§7(escribe §fcancel §7para cancelar)"), false);
                    awaitingInput.put(sp.getUuid(), "mission_extend:" + realIdx);
                } else {
                    // FIX 3: Open edit submenu for active mission
                    openMissionEdit(sp, realIdx);
                }
            }
        }
    }

    // FIX 3: Full edit menu for an existing mission
    public static void openMissionEdit(ServerPlayerEntity sp, int mIdx) {
        MissionDefinition def = ModeconomiaConfig.DATA.missions.definitions.get(mIdx);
        detailCtx.put(sp.getUuid(), "missionedit:" + mIdx);
        SimpleInventory inv = new SimpleInventory(27);
        fill(inv, Items.CYAN_STAINED_GLASS_PANE, "§3 ");
        item(inv, 4, Items.NETHER_STAR,
            "§b§l✎ Editando: §f" + def.displayName,
            "§7Tipo: §b" + def.type.name(),
            "§7Selecciona qué campo editar.");
        item(inv, 10, Items.NAME_TAG,
            "§e§lEditar Nombre",
            "§7Actual: §f" + def.displayName,
            "§7► Click para cambiar.");
        item(inv, 12, Items.WRITABLE_BOOK,
            "§e§lEditar Descripción",
            "§7Actual: §7" + (def.description.isEmpty() ? "§8Sin descripción" : def.description),
            "§7► Click para cambiar.");
        item(inv, 14, Items.GOLD_NUGGET,
            "§e§lEditar Recompensa",
            "§7Actual: §6" + MissionManager.format(def.reward) + " CC",
            "§7► Click para cambiar.");
        item(inv, 16, Items.PAPER,
            "§e§lEditar Objetivo §8(cantidad)",
            "§7Actual: §f" + def.requiredAmount,
            "§7► Click para cambiar.");
        item(inv, 22, Items.CLOCK,
            "§e§lExtender Duración",
            "§7Tiempo restante: " + def.timeRemaining(),
            "§7► Click para añadir más tiempo.");
        item(inv, 26, Items.ARROW, "§7« Volver", "§7Regresa a la lista de misiones.");
        open(sp, inv, "mission_edit", 3, "§b✎ Editar Misión");
    }

    private void handleAfkConfig(ServerPlayerEntity sp, int slot, int button) {
        switch (slot) {
            case 10 -> { BlockPos p = sp.getBlockPos();
                ModeconomiaConfig.DATA.afk.pos1X = p.getX(); ModeconomiaConfig.DATA.afk.pos1Y = p.getY();
                ModeconomiaConfig.DATA.afk.pos1Z = p.getZ();
                ModeconomiaConfig.DATA.afk.world = sp.getWorld().getRegistryKey().getValue().toString();
                ModeconomiaConfig.save();
                sp.sendMessage(Text.literal("§a✔ Pos1 guardada: §f" + p.getX() + ", " + p.getY() + ", " + p.getZ()), false);
                openAfkConfig(sp); }
            case 12 -> { BlockPos p = sp.getBlockPos();
                ModeconomiaConfig.DATA.afk.pos2X = p.getX(); ModeconomiaConfig.DATA.afk.pos2Y = p.getY();
                ModeconomiaConfig.DATA.afk.pos2Z = p.getZ();
                ModeconomiaConfig.DATA.afk.world = sp.getWorld().getRegistryKey().getValue().toString();
                ModeconomiaConfig.save();
                sp.sendMessage(Text.literal("§a✔ Pos2 guardada: §f" + p.getX() + ", " + p.getY() + ", " + p.getZ()), false);
                openAfkConfig(sp); }
            case 14 -> { double d = button == 0 ? 0.1 : -0.1;
                ModeconomiaConfig.DATA.afk.baseReward = Math.max(0, ModeconomiaConfig.DATA.afk.baseReward + d);
                ModeconomiaConfig.save(); openAfkConfig(sp); }
            case 16 -> { int d = button == 0 ? 1 : -1;
                ModeconomiaConfig.DATA.afk.intervalMinutes = Math.max(1, ModeconomiaConfig.DATA.afk.intervalMinutes + d);
                ModeconomiaConfig.save(); openAfkConfig(sp); }
            case 20, 21, 22, 23 -> {
                Rank r = slot == 20 ? Rank.TRAINER_PLUS : slot == 21 ? Rank.ELITE : slot == 22 ? Rank.LEGENDARY : Rank.MYTHICAL;
                double d = button == 0 ? 0.1 : -0.1;
                double cur = ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(r, 1.0);
                ModeconomiaConfig.DATA.afk.multipliers.put(r, Math.max(1.0, cur + d));
                ModeconomiaConfig.save(); openAfkConfig(sp); }
            case 26 -> openAdminMain(sp);
        }
    }

    // ── Shop slot map (shared between server+player shop views) ──
    private static final Map<Integer, ShopItem> shopSlotMap = new HashMap<>();

    /** shop_view = selector menu */
    private void handleShopView(ServerPlayerEntity sp, int slot) {
        switch (slot) {
            case 24 -> openShopServer(sp, 0);
            case 30 -> openShopPlayers(sp, 0);
            case 49 -> sp.closeHandledScreen();
        }
    }

    // FIX 8+9: handleShopConfig — owner adds item by hand to any slot 0-44; slot 45 also works
    private void handleShopConfig(ServerPlayerEntity sp, int slot, int button) {
        // Get current page from context
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "shopconfig:0");
        int page = 0;
        try { if (ctx.startsWith("shopconfig:")) page = Integer.parseInt(ctx.substring(11)); } catch (Exception ignored) {}

        // ── Navigation ──
        if (slot == 45 && page > 0) { openShopConfig(sp, page - 1); return; }
        if (slot == 53) {
            // If last page, slot 53 = "Siguiente"; otherwise it could be "Volver"
            List<ShopItem> all = ModeconomiaConfig.DATA.shop.items;
            int totalPages = Math.max(1, (int) Math.ceil(all.size() / 45.0));
            if (page < totalPages - 1) { openShopConfig(sp, page + 1); return; }
            openAdminMain(sp); return;
        }

        // ── Limpiar tienda ──
        if (slot == 48) {
            ModeconomiaConfig.DATA.shop.items.clear();
            ModeconomiaConfig.save();
            sp.sendMessage(Text.literal("§a✔ Tienda limpiada."), false);
            openShopConfig(sp, 0);
            return;
        }

        // ── Repoblar ──
        if (slot == 47) {
            com.cobblemania.economia.shop.ShopDefaults.populate(sp.getServer());
            sp.sendMessage(Text.literal("§a✔ Items de Cobblemon agregados."), false);
            openShopConfig(sp, page);
            return;
        }

        // ── Agregar item (slot 46 = add button) ──
        if (slot == 46 || (slot >= 0 && slot < 45)) {
            // Sort items same way as display
            List<ShopItem> sorted = new ArrayList<>(ModeconomiaConfig.DATA.shop.items);
            sorted.sort(java.util.Comparator.comparingInt(i -> i.slot));

            ShopItem existing = null;
            if (slot >= 0 && slot < 45) {
                int realIdx = page * 45 + slot;
                if (realIdx < sorted.size()) existing = sorted.get(realIdx);
            }

            if (existing != null) {
                if (button == 1) {
                    // Right click → delete
                    final ShopItem toDelete = existing;
                    ModeconomiaConfig.DATA.shop.items.removeIf(i -> i.slot == toDelete.slot);
                    ModeconomiaConfig.save();
                    sp.sendMessage(Text.literal("§a✔ Item eliminado."), false);
                    openShopConfig(sp, page);
                } else {
                    // Left click → open price+category editor
                    openShopPriceEdit(sp, existing.slot);
                }
            } else {
                // Empty slot or add button → add item from hand
                ItemStack hand = sp.getMainHandStack();
                if (!hand.isEmpty()) {
                    int targetSlot = findFreeShopSlot();
                    String nbt = ItemStackUtil.toNbtString(hand.copy(), sp.getServer().getRegistryManager());
                    ShopItem newItem = new ShopItem(targetSlot, nbt, 1.0);
                    newItem.category = com.cobblemania.economia.shop.CategoryDetector.detect(hand);
                    ModeconomiaConfig.DATA.shop.items.add(newItem);
                    ModeconomiaConfig.save();
                    awaitingInput.put(sp.getUuid(), "shopitem_price:" + targetSlot);
                    sp.sendMessage(Text.literal("§a✔ Item agregado (§e" + newItem.category + "§a). Escribe el precio:"), false);
                    sp.sendMessage(Text.literal("§7(escribe §fcancel §7para cancelar)"), false);
                } else {
                    sp.sendMessage(Text.literal("§e⚠ Sostén un item en la mano para agregarlo."), false);
                    openShopConfig(sp, page);
                }
            }
        }
    }

    private static int findFreeShopSlot() {
        java.util.Set<Integer> used = ModeconomiaConfig.DATA.shop.items.stream()
            .map(si -> si.slot).collect(java.util.stream.Collectors.toSet());
        int max = used.isEmpty() ? -1 : used.stream().mapToInt(Integer::intValue).max().getAsInt();
        for (int i = 0; i <= max + 1; i++) if (!used.contains(i)) return i;
        return max + 1;
    }

    private void handleShopSell(ServerPlayerEntity sp, int slot, int button) {
        double price = sellPriceCtx.getOrDefault(sp.getUuid(), 1.0);
        int dur = sellDurCtx.getOrDefault(sp.getUuid(), 60);
        switch (slot) {
            case 10 -> {
                double d = button == 0 ? 1.0 : -1.0;
                sellPriceCtx.put(sp.getUuid(), Math.max(0.1, price + d));
                openShopSell(sp, sellItemCtx.getOrDefault(sp.getUuid(), ItemStack.EMPTY));
            }
            case 12 -> {
                int d = button == 0 ? 15 : -15;
                sellDurCtx.put(sp.getUuid(), Math.max(1, dur + d));
                openShopSell(sp, sellItemCtx.getOrDefault(sp.getUuid(), ItemStack.EMPTY));
            }
            case 15 -> {
                ItemStack stack = sellItemCtx.getOrDefault(sp.getUuid(), ItemStack.EMPTY);
                if (stack.isEmpty()) { openShopPlayers(sp, 0); return; }
                String nbt = ItemStackUtil.toNbtString(stack, sp.getServer().getRegistryManager());
                ShopItem listing = new ShopItem(-1, nbt, price); // slot=-1, asignado dinámicamente
                listing.sellerUuid = sp.getUuid().toString();
                listing.durationMinutes = dur;
                listing.expiresAt = System.currentTimeMillis() + dur * 60_000L;
                ShopManager.addListing(listing);
                sp.getInventory().setStack(sp.getInventory().selectedSlot, ItemStack.EMPTY);
                sp.sendMessage(Text.literal("§a✔ §fItem listado por §6" + MissionManager.format(price)
                    + " CC §adurante §f" + dur + " min§a."), false);
                sellItemCtx.remove(sp.getUuid());
                sellPriceCtx.remove(sp.getUuid());
                sellDurCtx.remove(sp.getUuid());
                openShopPlayers(sp, 0);
            }
            case 17 -> openShopView(sp);
        }
    }

    private void handleShopPriceEdit(ServerPlayerEntity sp, int slot, int button) {
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "");
        if (!ctx.startsWith("shopslot:")) { openShopConfig(sp); return; }
        // Format: "shopslot:<slot>:p<catPage>"
        String shopSlotStr = ctx.substring(9);
        int catPage = 0;
        int shopSlotNum;
        int pIdx = shopSlotStr.indexOf(":p");
        if (pIdx >= 0) {
            try { catPage = Integer.parseInt(shopSlotStr.substring(pIdx + 2)); } catch (Exception ignored) {}
            shopSlotStr = shopSlotStr.substring(0, pIdx);
        }
        try { shopSlotNum = Integer.parseInt(shopSlotStr); } catch (Exception e) { openShopConfig(sp); return; }

        ShopItem item2 = ModeconomiaConfig.DATA.shop.items.stream()
            .filter(i -> i.slot == shopSlotNum).findFirst().orElse(null);
        if (item2 == null) { openShopConfig(sp); return; }

        // Price adjustments
        double delta = switch (slot) {
            case 10 -> -100; case 11 -> -10; case 12 -> -1; case 13 -> -0.1;
            case 14 ->  0.1; case 15 ->  1;  case 16 ->  10; case 17 -> 100;
            default -> 0;
        };
        if (delta != 0) {
            item2.price = Math.max(0.1, Math.round((item2.price + delta) * 100.0) / 100.0);
            ModeconomiaConfig.save();
            openShopPriceEdit(sp, shopSlotNum, catPage);
            return;
        }

        if (catPage == 0) {
            // Página 1: categorías predeterminadas
            String newCat = switch (slot) {
                case 28 -> "pokeballs";
                case 29 -> "bayas";
                case 30 -> "vitaminas";
                case 31 -> "pp";
                case 32 -> "bonguris";
                case 33 -> "evoluciones";
                case 34 -> "general";
                case 37 -> "caramelos";
                case 38 -> "mentas";
                case 39 -> "tms";
                case 40 -> "batalla";
                case 41 -> "gemas";
                case 42 -> "invocadores";
                case 43 -> "currys";
                default -> null;
            };
            if (newCat != null) {
                item2.category = newCat;
                ModeconomiaConfig.save();
                openShopPriceEdit(sp, shopSlotNum, catPage);
                return;
            }
            // Slot 44 → ir a página de categorías custom
            if (slot == 44) {
                List<ModeconomiaConfig.CustomCategory> customCats = ModeconomiaConfig.DATA.shop.customCategories;
                if (customCats != null && !customCats.isEmpty()) {
                    openShopPriceEdit(sp, shopSlotNum, 1);
                } else {
                    sp.sendMessage(Text.literal("§c✘ No hay categorías custom. Créalas en §e/ccoins owner §c→ Categorías Custom."), false);
                    openShopPriceEdit(sp, shopSlotNum, catPage);
                }
                return;
            }
        } else {
            // Página 2+: categorías custom
            List<ModeconomiaConfig.CustomCategory> customCats =
                ModeconomiaConfig.DATA.shop.customCategories != null
                ? ModeconomiaConfig.DATA.shop.customCategories : java.util.Collections.emptyList();

            int[] catSlots = {28,29,30,31,32,33,34,37,38,39,40,41,42,43};
            int catPageSize = catSlots.length;
            int catStart = (catPage - 1) * catPageSize;

            // Navegar páginas de custom
            if (slot == 27 && catPage > 1) { openShopPriceEdit(sp, shopSlotNum, catPage - 1); return; }
            if (slot == 35) {
                int catTotalPages = Math.max(1, (int) Math.ceil(customCats.size() / (double) catPageSize));
                if (catPage < catTotalPages) { openShopPriceEdit(sp, shopSlotNum, catPage + 1); return; }
            }
            // Volver a predeterminadas
            if (slot == 44) { openShopPriceEdit(sp, shopSlotNum, 0); return; }

            // Click en una categoría custom
            for (int i = 0; i < catSlots.length; i++) {
                if (catSlots[i] == slot) {
                    int realIdx = catStart + i;
                    if (realIdx < customCats.size()) {
                        item2.category = customCats.get(realIdx).id;
                        ModeconomiaConfig.save();
                        sp.sendMessage(Text.literal("§a✔ Categoría asignada: §5" + customCats.get(realIdx).label), false);
                        openShopPriceEdit(sp, shopSlotNum, catPage);
                    }
                    return;
                }
            }
        }

        // Save
        if (slot == 49) {
            ModeconomiaConfig.save();
            sp.sendMessage(Text.literal("§a✔ Item guardado: §6" + MissionManager.format(item2.price)
                + " CC §8[" + (item2.category != null ? item2.category : "general") + "]"), false);
            openShopConfig(sp);
        } else if (slot == 53) {
            openShopConfig(sp);
        }
    }

    private void handleRanksMain(ServerPlayerEntity sp, int slot) {
        switch (slot) {
            case 10 -> openRankMembers(sp, Rank.TRAINER_PLUS);
            case 12 -> openRankMembers(sp, Rank.ELITE);
            case 14 -> openRankMembers(sp, Rank.LEGENDARY);
            case 16 -> openRankMembers(sp, Rank.MYTHICAL);
            case 26 -> openAdminMain(sp);
        }
    }

    private void handleRankMembers(ServerPlayerEntity sp, int slot) {
        if (slot == 49) { openRanksMain(sp); return; }
        String rankStr = detailCtx.getOrDefault(sp.getUuid(), "TRAINER_PLUS");
        try {
            Rank rank = Rank.valueOf(rankStr);
            if (rank == Rank.TRAINER) return; // TRAINER es base, no se asigna
            List<ServerPlayerEntity> players = sp.getServer().getPlayerManager().getPlayerList();
            int idx = slot - 9;
            if (idx >= 0 && idx < players.size()) {
                UUID target = players.get(idx).getUuid();
                Rank currentRank = RankManager.getRank(players.get(idx));
                if (currentRank == rank) {
                    // Ya tiene este rango → remover (vuelve a Trainer base)
                    RankManager.removeFromAllRanks(target);
                    sp.sendMessage(Text.literal("§c✔ Rango removido. Vuelve a §7Trainer §c(base)."), false);
                } else {
                    // Asignar nuevo rango (removeFromAllRanks incluido en addToRank)
                    RankManager.addToRank(rank, target);
                    sp.sendMessage(Text.literal("§a✔ Rango " + rankDisplay(rank) + " §aasignado."), false);
                }
                openRankMembers(sp, rank);
            }
        } catch (Exception ignored) {}
    }

    private void handleNpcConfig(ServerPlayerEntity sp, int slot) {
        switch (slot) {
            case 11 -> {
                ServerWorld world = (ServerWorld) sp.getWorld();
                VillagerEntity v = EntityType.VILLAGER.spawn(world, sp.getBlockPos(), SpawnReason.COMMAND);
                if (v != null) {
                    v.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.NITWIT, 1));
                    v.setCustomName(Text.literal("§6§l✦ §eMisiones §6§l✦"));
                    v.setCustomNameVisible(true);
                    v.setAiDisabled(true);
                    v.setInvulnerable(true);
                    v.setPersistent();
                    v.setSilent(true);
                    v.getOffers().clear();
                    ModeconomiaConfig.DATA.questNpcUuid = v.getUuid().toString();
                    // Guardar posición para /misiones
                    ModeconomiaConfig.DATA.questNpcWorld = sp.getServerWorld().getRegistryKey().getValue().toString();
                    ModeconomiaConfig.DATA.questNpcX     = sp.getX();
                    ModeconomiaConfig.DATA.questNpcY     = sp.getY();
                    ModeconomiaConfig.DATA.questNpcZ     = sp.getZ();
                    ModeconomiaConfig.save();
                    sp.sendMessage(Text.literal("§a✔ NPC §e§lMisiones §aspawneado en tu posición."), false);
                }
                openNpcConfig(sp);
            }
            case 15 -> {
                if (ModeconomiaConfig.DATA.questNpcUuid != null) {
                    try {
                        net.minecraft.entity.Entity e = ((ServerWorld)sp.getWorld())
                            .getEntity(UUID.fromString(ModeconomiaConfig.DATA.questNpcUuid));
                        if (e != null) e.discard();
                    } catch (Exception ignored) {}
                    ModeconomiaConfig.DATA.questNpcUuid = null;
                    ModeconomiaConfig.save();
                    sp.sendMessage(Text.literal("§a✔ NPC Misiones eliminado."), false);
                }
                openNpcConfig(sp);
            }
            case 22 -> openAdminMain(sp);
        }
    }

    private void handleShopNpcConfig(ServerPlayerEntity sp, int slot) {
        switch (slot) {
            case 11 -> {
                ServerWorld world = (ServerWorld) sp.getWorld();
                VillagerEntity v = EntityType.VILLAGER.spawn(world, sp.getBlockPos(), SpawnReason.COMMAND);
                if (v != null) {
                    v.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.CARTOGRAPHER, 2));
                    v.setCustomName(Text.literal("§b§l✦ §fTienda §b§l✦"));
                    v.setCustomNameVisible(true);
                    v.setAiDisabled(true);
                    v.setInvulnerable(true);
                    v.setPersistent();
                    v.setSilent(true);
                    v.getOffers().clear();
                    ModeconomiaConfig.DATA.shopNpcUuid = v.getUuid().toString();
                    // Guardar posición para /tienda
                    ModeconomiaConfig.DATA.shopNpcWorld = sp.getServerWorld().getRegistryKey().getValue().toString();
                    ModeconomiaConfig.DATA.shopNpcX     = sp.getX();
                    ModeconomiaConfig.DATA.shopNpcY     = sp.getY();
                    ModeconomiaConfig.DATA.shopNpcZ     = sp.getZ();
                    ModeconomiaConfig.save();
                    sp.sendMessage(Text.literal("§a✔ NPC §b§lTienda §aspawneado en tu posición."), false);
                }
                openShopNpcConfig(sp);
            }
            case 15 -> {
                if (ModeconomiaConfig.DATA.shopNpcUuid != null) {
                    try {
                        net.minecraft.entity.Entity e = ((ServerWorld)sp.getWorld())
                            .getEntity(UUID.fromString(ModeconomiaConfig.DATA.shopNpcUuid));
                        if (e != null) e.discard();
                    } catch (Exception ignored) {}
                    ModeconomiaConfig.DATA.shopNpcUuid = null;
                    ModeconomiaConfig.save();
                    sp.sendMessage(Text.literal("§a✔ NPC Tienda eliminado."), false);
                }
                openShopNpcConfig(sp);
            }
            case 22 -> openAdminMain(sp);
        }
    }

    private void handlePlayerList(ServerPlayerEntity sp, int slot) {
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "page:0");
        int page = 0;
        try { if (ctx.startsWith("page:")) page = Integer.parseInt(ctx.substring(5)); } catch (Exception ignored) {}
        if (slot == 45 && page > 0) { openPlayerList(sp, page - 1); return; }
        if (slot == 53) { openPlayerList(sp, page + 1); return; }
        if (slot == 47) { openAdminMain(sp); return; }
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        List<String> uuids = EconomyStorage.getAllPlayerUuids();
        uuids.sort((a, b) -> Double.compare(EconomyStorage.getBalance(toUUID(b)), EconomyStorage.getBalance(toUUID(a))));
        int start = page * slots.length;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                int idx = start + i;
                if (idx < uuids.size()) openPlayerDetail(sp, uuids.get(idx));
                return;
            }
        }
    }

    // FIX 3+7: Player detail handler — edit fields + rank management
    private void handlePlayerDetail(ServerPlayerEntity sp, int slot) {
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "");
        if (!ctx.startsWith("detail:")) { openAdminMain(sp); return; }
        String uuidStr = ctx.substring(7);
        if (slot == 49) { openPlayerList(sp, 0); return; }
        try {
            UUID uuid = toUUID(uuidStr);
            // Economy actions
            if (slot == 14) { targetCtx.put(sp.getUuid(), uuid); amountCtx.put(sp.getUuid(), new double[]{0,0}); openEconomyAmount(sp); }
            else if (slot == 23) { targetCtx.put(sp.getUuid(), uuid); amountCtx.put(sp.getUuid(), new double[]{0,1}); openEconomyAmount(sp); }
            else if (slot == 32) { targetCtx.put(sp.getUuid(), uuid); amountCtx.put(sp.getUuid(), new double[]{EconomyStorage.getBalance(uuid),2}); openEconomyAmount(sp); }
            else if (slot == 41) {
                PlayerMissionData data = EconomyStorage.getMissionData(uuid);
                data.progress.clear(); data.completed.clear(); EconomyStorage.save();
                sp.sendMessage(Text.literal("§a✔ Misiones reiniciadas."), false);
                openPlayerDetail(sp, uuidStr);
            }
            // Cycle rank (slot 16)
            else if (slot == 16) {
                Rank current = getRankByUuid(sp, uuid);
                Rank next = cycleRank(current);
                RankManager.removeFromAllRanks(uuid);
                if (next != null && next != Rank.TRAINER) RankManager.addToRank(next, uuid);
                sp.sendMessage(Text.literal("§a✔ Rango actualizado a: " + rankDisplay(next)), false);
                openPlayerDetail(sp, uuidStr);
            }
            // Remove rank (slot 25) → back to TRAINER base
            else if (slot == 25) {
                RankManager.removeFromAllRanks(uuid);
                sp.sendMessage(Text.literal("§a✔ Rango removido. Vuelve a §7Trainer §a(base)."), false);
                openPlayerDetail(sp, uuidStr);
            }
        } catch (Exception ignored) {}
    }

    // Handle mission edit submenu clicks
    private void handleMissionEdit(ServerPlayerEntity sp, int slot) {
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "");
        if (!ctx.startsWith("missionedit:")) { openMissionsList(sp); return; }
        int mIdx = Integer.parseInt(ctx.substring(12));
        if (slot == 26) { openMissionsList(sp); return; }
        MissionDefinition def = ModeconomiaConfig.DATA.missions.definitions.get(mIdx);
        switch (slot) {
            case 10 -> { // Edit name
                sp.sendMessage(Text.literal("§7Nombre actual: §f" + def.displayName), false);
                sp.sendMessage(Text.literal("§7Escribe el nuevo §enombre§7:"), false);
                awaitingInput.put(sp.getUuid(), "medit_name:" + mIdx);
            }
            case 12 -> { // Edit desc
                sp.sendMessage(Text.literal("§7Descripción actual: §7" + def.description), false);
                sp.sendMessage(Text.literal("§7Escribe la nueva §edescripción§7:"), false);
                awaitingInput.put(sp.getUuid(), "medit_desc:" + mIdx);
            }
            case 14 -> { // Edit reward
                sp.sendMessage(Text.literal("§7Recompensa actual: §6" + MissionManager.format(def.reward) + " CC"), false);
                sp.sendMessage(Text.literal("§7Escribe la nueva §erecompensa §7(ej: §f0.50§7):"), false);
                awaitingInput.put(sp.getUuid(), "medit_reward:" + mIdx);
            }
            case 16 -> { // Edit goal
                sp.sendMessage(Text.literal("§7Objetivo actual: §f" + def.requiredAmount), false);
                sp.sendMessage(Text.literal("§7Escribe el nuevo §eobjetivo §7(ej: §f64§7):"), false);
                awaitingInput.put(sp.getUuid(), "medit_goal:" + mIdx);
            }
            case 22 -> { // Extend duration
                sp.sendMessage(Text.literal("§7Tiempo restante: " + def.timeRemaining()), false);
                sp.sendMessage(Text.literal("§7¿Cuánto tiempo añadir? Escribe §e24 §7(horas) o §emanual§7:"), false);
                awaitingInput.put(sp.getUuid(), "mission_extend:" + mIdx);
            }
        }
    }

    // ══════════════════════════════════════
    // TIENDAS — 3 GUIs separadas
    // ══════════════════════════════════════

    /** Selector principal: elige tienda del servidor o de jugadores */
    public static void openShopSelector(ServerPlayerEntity sp) {
        SimpleInventory inv = new SimpleInventory(54);
        // Border design — purple/gold theme
        for (int i = 0; i < 9; i++)   item(inv, i,    Items.PURPLE_STAINED_GLASS_PANE, "§5 ");
        for (int i = 45; i < 54; i++) item(inv, i,    Items.PURPLE_STAINED_GLASS_PANE, "§5 ");
        for (int i = 0; i < 5; i++) {
            item(inv, 9  + i*9, Items.PURPLE_STAINED_GLASS_PANE, "§5 ");
            item(inv, 17 + i*9, Items.PURPLE_STAINED_GLASS_PANE, "§5 ");
        }
        // Title
        item(inv, 4, ModItems.COBBLE_COIN,
            "§6§l✦ CobbleMania — Tienda §6§l✦",
            "§7Bienvenido a la tienda del servidor.",
            "§8Usa CobbleCoins (CC) para comprar.");

        // ── Solo dos botones ──
        item(inv, 24, Items.CHEST,
            "§b§l🏪 Tienda del Servidor",
            "§fItems organizados por categorías.",
            "§8Pokébolas · Bayas · Vitaminas · PP",
            "§8Bonguris · Evoluciones y más...",
            "§a► Click para entrar.");

        item(inv, 30, Items.PLAYER_HEAD,
            "§e§l👥 Tienda de Jugadores",
            "§fVende y compra con otros jugadores.",
            "§8Tus items duran 24h en la tienda.",
            "§a► Click para entrar o vender.");

        item(inv, 49, Items.BARRIER, "§c§l✘ Cerrar", "§7Cierra este menú.");
        open(sp, inv, "shop_view", 6, "§6✦ CobbleMania — Tienda ✦");
    }

    /** Alias para compatibilidad con el resto del código */
    public static void openShopView(ServerPlayerEntity sp) { openShopSelector(sp); }

    /** Tienda del Servidor — por categorías */
    /** Tienda del Servidor — menú de categorías */
    public static void openShopServer(ServerPlayerEntity sp, int page) {
        // page encodes both category and page: "cat:page" stored in detailCtx
        // When called with page=0 → show category selector
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "shopserver:cat:-:0");
        // If page==0 and no category in context, show category picker
        String activeCategory = null;
        int realPage = 0;
        if (ctx.startsWith("shopcat:")) {
            activeCategory = ctx.substring(8, ctx.lastIndexOf(':'));
            try { realPage = Integer.parseInt(ctx.substring(ctx.lastIndexOf(':')+1)); } catch (Exception ignored) {}
        }
        if (page == 0 && activeCategory == null) {
            openShopCategories(sp);
            return;
        }
        openShopServerCategory(sp, activeCategory != null ? activeCategory : "general", realPage);
    }

    /** Selector de categorías de la tienda del servidor */
    public static void openShopCategories(ServerPlayerEntity sp) {
        openShopCategories(sp, 0);
    }

    public static void openShopCategories(ServerPlayerEntity sp, int page) {
        SimpleInventory inv = new SimpleInventory(54);
        for (int i = 0; i < 54; i++) item(inv, i, Items.PURPLE_STAINED_GLASS_PANE, "§5 ");
        for (int i = 9; i < 45; i++) item(inv, i, Items.BLACK_STAINED_GLASS_PANE, "§8 ");

        List<ModeconomiaConfig.CustomCategory> customCats =
            ModeconomiaConfig.DATA.shop.customCategories != null
            ? ModeconomiaConfig.DATA.shop.customCategories
            : java.util.Collections.emptyList();

        int totalPages = customCats.isEmpty() ? 1 : 2;

        item(inv, 4, ModItems.COBBLE_COIN,
            "§6§l✦ Tienda del Servidor §6§l✦",
            "§7Elige una categoría para ver los items.",
            "§8Página " + (page + 1) + "/" + totalPages);

        if (page == 0) {
            // ── Página 1: categorías predeterminadas ──
            item(inv, 10, Items.PURPLE_DYE,       "§d§l🎯 Pokébolas",       "§7Poké · Súper · Ultra · Gran · Master", "§7Cura · Nido · Lujo · Rapidez y más.", "§a► Click para ver.");
            item(inv, 11, Items.SWEET_BERRIES,     "§a§l🍓 Bayas",           "§7Aranja · Frambu · Ziuela · Pecha",     "§7Macha · Meloc · Zirnit y más.",        "§a► Click para ver.");
            item(inv, 12, Items.BLAZE_POWDER,      "§e§l⚡ Vitaminas & PP",  "§7Proteína · Hierro · Calcio · Zinc",    "§7Carbos · PH Up · PP Nor · PP Máx.",   "§a► Click para ver.");
            item(inv, 13, Items.BROWN_DYE,         "§6§l🎋 Bonguris",        "§7Bonguri Verde · Rojo · Azul",          "§7Amarillo · Blanco · Negro.",            "§a► Click para ver.");
            item(inv, 14, Items.DIAMOND,           "§b§l💎 Evoluciones",     "§7Piedra Fuego · Agua · Trueno · Hoja",  "§7Objetos hold para evolución especial.", "§a► Click para ver.");
            item(inv, 15, Items.CHEST,             "§f§l📦 General",         "§7Items varios del servidor.",            "§a► Click para ver.");
            item(inv, 19, Items.PURPLE_DYE,        "§5§l📀 MTs (TM)",        "§7Todas las MTs de movimientos.",         "§7MT01 · MT44 · MT100 y más.",           "§a► Click para ver.");
            item(inv, 20, Items.GREEN_DYE,         "§a§l🌿 Mentas",          "§7Mentas para cambiar la naturaleza.",    "§7Menta Alegre · Tímida · Osada y más.", "§a► Click para ver.");
            item(inv, 21, Items.IRON_SWORD,        "§c§l⚔ Items de Batalla", "§7Objetos hold para combate.",            "§7Banda Focus · Pañuelo · Gafas y más.", "§a► Click para ver.");
            item(inv, 22, Items.PINK_DYE,          "§d§l🍬 Caramelos Exp.",  "§7Caramelo XS · S · M · L · XL · XXL.", "§7Para subir nivel rápidamente.",         "§a► Click para ver.");
            item(inv, 23, Items.AMETHYST_SHARD,    "§5§l💠 Gemas",           "§7Gemas de todos los tipos.",             "§7Gema Fuego · Agua · Dragón y más.",    "§a► Click para ver.");
            item(inv, 24, Items.MUSHROOM_STEW,     "§6§l🍛 Currys Pokémon",  "§7Curry para crear lazos con Pokémon.",   "§7Curry Dulce · Especiado · Amargo y más.", "§a► Click para ver.");
            item(inv, 25, Items.NETHER_STAR,       "§e§l⭐ Invocadores",     "§7Items para invocar o atraer Pokémon.", "§7Flauta Azul · Señuelo · Radar y más.", "§a► Click para ver.");

            // Botón siguiente solo si hay custom cats
            if (!customCats.isEmpty())
                item(inv, 53, Items.ARROW, "§7Siguiente: Categorías Custom »", "§7" + customCats.size() + " categoría(s) personalizada(s).");

        } else {
            // ── Página 2: categorías custom ──
            int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
            for (int i = 0; i < customCats.size() && i < slots.length; i++) {
                ModeconomiaConfig.CustomCategory cc = customCats.get(i);
                String color = cc.color != null ? cc.color : "§f";
                String desc  = (cc.description != null && !cc.description.isEmpty()) ? cc.description : "Categoría personalizada.";

                ItemStack icon;
                if (cc.iconNbt != null && !cc.iconNbt.isEmpty()) {
                    icon = ItemStackUtil.fromNbtString(cc.iconNbt, sp.getServer().getRegistryManager());
                    if (icon.isEmpty()) icon = new ItemStack(Items.BOOKSHELF);
                } else {
                    icon = new ItemStack(Items.BOOKSHELF);
                }
                icon.set(DataComponentTypes.CUSTOM_NAME, Text.literal(color + "§l" + cc.label));
                icon.remove(DataComponentTypes.LORE);
                List<Text> lore = new ArrayList<>();
                lore.add(Text.literal("§7" + desc));
                lore.add(Text.literal("§a► Click para ver."));
                icon.set(DataComponentTypes.LORE, new LoreComponent(lore));
                inv.setStack(slots[i], icon);
            }

            item(inv, 45, Items.ARROW, "§7« Anterior: Categorías Base", "§7Vuelve a las categorías predeterminadas.");
        }

        item(inv, 49, Items.BARRIER, "§c§l✘ Cerrar", "§7Cierra la tienda.");
        detailCtx.put(sp.getUuid(), "shopcategories:" + page);
        open(sp, inv, "shop_server", 6, "§6✦ Tienda — Categorías §8[" + (page+1) + "/" + totalPages + "]");
    }

    /** Tienda por categoría específica, paginada */
    public static void openShopServerCategory(ServerPlayerEntity sp, String category, int page) {
        ShopManager.cleanupExpiredListings();
        shopSlotMap.clear();

        List<ShopItem> allItems = new ArrayList<>(ShopManager.getItems());
        // Filter by category
        List<ShopItem> items = allItems.stream()
            .filter(i -> category.equals(i.category != null ? i.category : "general"))
            .collect(java.util.stream.Collectors.toList());

        int pageSize  = 45;
        int total     = items.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages - 1));
        int start = page * pageSize;

        SimpleInventory inv = new SimpleInventory(54);
        // Bottom bar color per category
        Item barItem = switch (category) {
            case "pokeballs"   -> Items.PURPLE_STAINED_GLASS_PANE;
            case "bayas"       -> Items.GREEN_STAINED_GLASS_PANE;
            case "vitaminas"   -> Items.YELLOW_STAINED_GLASS_PANE;
            case "bonguris"    -> Items.BROWN_STAINED_GLASS_PANE;
            case "evoluciones" -> Items.BLUE_STAINED_GLASS_PANE;
            case "tms"         -> Items.MAGENTA_STAINED_GLASS_PANE;
            case "mentas"      -> Items.GREEN_STAINED_GLASS_PANE;
            case "batalla"     -> Items.RED_STAINED_GLASS_PANE;
            case "caramelos"   -> Items.PINK_STAINED_GLASS_PANE;
            case "gemas"       -> Items.MAGENTA_STAINED_GLASS_PANE;
            case "currys"      -> Items.ORANGE_STAINED_GLASS_PANE;
            case "invocadores" -> Items.YELLOW_STAINED_GLASS_PANE;
            default            -> Items.WHITE_STAINED_GLASS_PANE;
        };
        for (int i = 45; i < 54; i++) item(inv, i, barItem, "§8 ");

        for (int i = 0; i < pageSize && (start + i) < total; i++) {
            ShopItem shopItem = items.get(start + i);
            ItemStack stack = ItemStackUtil.fromNbtString(shopItem.itemNbt, sp.getServer().getRegistryManager());
            if (stack.isEmpty()) continue;
            String price = MissionManager.format(shopItem.price);
            // Capture the display name BEFORE overwriting (getName uses custom name or item translation)
            String displayName = stack.getName().getString();
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§f§l" + displayName));
            // Remove any existing lore (vanilla or Cobblemon) then apply our own
            stack.remove(DataComponentTypes.LORE);
            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal("§6Precio: §e" + price + " CC"));
            lore.add(Text.literal("§8Vendedor: §7Servidor"));
            lore.add(Text.literal("§a► Click izq para comprar."));
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
            inv.setStack(i, stack);
            shopSlotMap.put(i, shopItem);
        }

        String catLabel = switch (category) {
            case "pokeballs"   -> "§d🎯 Pokébolas";
            case "bayas"       -> "§a🍓 Bayas";
            case "vitaminas"   -> "§e⚡ Vitaminas & PP";
            case "bonguris"    -> "§6🎋 Bonguris";
            case "evoluciones" -> "§9💎 Evoluciones";
            case "tms"         -> "§5📀 MTs";
            case "mentas"      -> "§a🌿 Mentas";
            case "batalla"     -> "§c⚔ Items de Batalla";
            case "caramelos"   -> "§d🍬 Caramelos Exp.";
            case "gemas"       -> "§5💠 Gemas";
            case "currys"      -> "§6🍛 Currys";
            case "invocadores" -> "§e⭐ Invocadores";
            default            -> {
                // Buscar en categorías custom
                String found = null;
                if (ModeconomiaConfig.DATA.shop.customCategories != null) {
                    for (ModeconomiaConfig.CustomCategory cc : ModeconomiaConfig.DATA.shop.customCategories) {
                        if (category.equals(cc.id)) {
                            String c = cc.color != null ? cc.color : "§f";
                            found = c + "§l" + cc.label;
                            break;
                        }
                    }
                }
                yield found != null ? found : "§f📦 General";
            }
        };

        if (page > 0)
            item(inv, 45, Items.ARROW, "§7« Anterior", "§7Página " + page + "/" + totalPages);
        item(inv, 47, Items.ARROW, "§7« Categorías", "§7Vuelve al menú de categorías.");
        item(inv, 49, barItem, catLabel + " §8[" + (page+1) + "/" + totalPages + "]",
            "§7" + total + " items · §6" + ShopManager.getItems().size() + " total servidor");
        item(inv, 51, Items.BARRIER, "§c§l✘ Cerrar", "§7Cierra la tienda.");
        if (page < totalPages - 1)
            item(inv, 53, Items.ARROW, "§7Siguiente »", "§7Página " + (page+2) + "/" + totalPages);

        detailCtx.put(sp.getUuid(), "shopcat:" + category + ":" + page);
        open(sp, inv, "shop_server", 6, "§6✦ " + catLabel);
    }

    /** Tienda de Jugadores — listings de jugadores, paginada, sin límite */
    public static void openShopPlayers(ServerPlayerEntity sp, int page) {
        ShopManager.cleanupExpiredListings();
        shopSlotMap.clear();
        List<ShopItem> listings = ShopManager.getListings().stream()
            .filter(l -> !l.isExpired())
            .collect(java.util.stream.Collectors.toList());
        int pageSize = 44; // slots 0-43, slot 44 libre para botón vender
        int total = listings.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages - 1));
        int start = page * pageSize;

        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, Items.YELLOW_STAINED_GLASS_PANE, "§e ");

        for (int i = 0; i < pageSize && (start + i) < total; i++) {
            ShopItem listing = listings.get(start + i);
            ItemStack stack = ItemStackUtil.fromNbtString(listing.itemNbt, sp.getServer().getRegistryManager());
            if (stack.isEmpty()) continue;
            String price  = MissionManager.format(listing.price);
            String seller = resolveSellerName(sp, listing.sellerUuid);
            long msLeft   = listing.expiresAt - System.currentTimeMillis();
            String timeLeft = msLeft <= 0 ? "§cExpirado"
                : msLeft < 3_600_000 ? "§e" + (msLeft / 60_000) + " min"
                : "§a" + (msLeft / 3_600_000) + "h " + ((msLeft % 3_600_000) / 60_000) + "m";
            String displayName2 = stack.getName().getString();
            stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§f§l" + displayName2 + " §7- §6" + price + " CC"));
            stack.remove(DataComponentTypes.LORE);
            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal("§7Precio: §6" + price + " CobbleCoins"));
            lore.add(Text.literal("§7Vendedor: §e" + seller));
            lore.add(Text.literal("§7Expira en: " + timeLeft));
            lore.add(Text.literal("§a► Click para comprar."));
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
            inv.setStack(i, stack);
            shopSlotMap.put(i, listing);
        }
        if (page > 0)
            item(inv, 45, Items.ARROW, "§7« Anterior", "§7Página " + page + "/" + totalPages);
        item(inv, 46, Items.EMERALD, "§6§l+ Vender mi item",
            "§7Sostén el item en la mano y haz click aquí.",
            "§7Define precio y duración de la publicación.");
        item(inv, 47, Items.BARRIER, "§c§l✘ Volver", "§7Regresa al selector.");
        item(inv, 49, Items.YELLOW_STAINED_GLASS_PANE,
            "§e§l👥 Tienda de Jugadores §8[" + (page+1) + "/" + totalPages + "]",
            "§7" + total + " items listados actualmente.");
        if (page < totalPages - 1)
            item(inv, 53, Items.ARROW, "§7Siguiente »", "§7Página " + (page+2) + "/" + totalPages);
        detailCtx.put(sp.getUuid(), "shopplayers:" + page);
        open(sp, inv, "shop_players", 6, "§e👥 Tienda de Jugadores");
    }

    private static String resolveSellerName(ServerPlayerEntity sp, String sellerUuid) {
        if (sellerUuid == null) return "Desconocido";
        try {
            UUID uuid = UUID.fromString(sellerUuid);
            ServerPlayerEntity online = sp.getServer().getPlayerManager().getPlayer(uuid);
            if (online != null) return online.getName().getString();
            String cached = EconomyStorage.getMissionData(uuid).playerName;
            return (cached != null && !cached.isEmpty()) ? cached : sellerUuid.substring(0, 8);
        } catch (Exception ignored) {
            return sellerUuid.substring(0, Math.min(8, sellerUuid.length()));
        }
    }

    private void handleShopServer(ServerPlayerEntity sp, int slot) {
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "shopcategories");

        // ── Category selector screen ──
        if (ctx.startsWith("shopcategories")) {
            int catMenuPage = 0;
            try {
                if (ctx.contains(":")) catMenuPage = Integer.parseInt(ctx.substring(ctx.lastIndexOf(':') + 1));
            } catch (Exception ignored) {}

            if (catMenuPage == 0) {
                // Página 1: predeterminadas
                String cat = switch (slot) {
                    case 10 -> "pokeballs";
                    case 11 -> "bayas";
                    case 12 -> "vitaminas";
                    case 13 -> "bonguris";
                    case 14 -> "evoluciones";
                    case 15 -> "general";
                    case 19 -> "tms";
                    case 20 -> "mentas";
                    case 21 -> "batalla";
                    case 22 -> "caramelos";
                    case 23 -> "gemas";
                    case 24 -> "currys";
                    case 25 -> "invocadores";
                    case 49 -> { sp.closeHandledScreen(); yield null; }
                    case 53 -> { openShopCategories(sp, 1); yield null; }
                    default -> null;
                };
                if (cat != null) { openShopServerCategory(sp, cat, 0); return; }

            } else {
                // Página 2: custom
                if (slot == 45) { openShopCategories(sp, 0); return; }
                if (slot == 49) { sp.closeHandledScreen(); return; }

                int[] customSlots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
                List<ModeconomiaConfig.CustomCategory> customCats =
                    ModeconomiaConfig.DATA.shop.customCategories != null
                    ? ModeconomiaConfig.DATA.shop.customCategories
                    : java.util.Collections.emptyList();
                for (int i = 0; i < customCats.size() && i < customSlots.length; i++) {
                    if (customSlots[i] == slot) {
                        openShopServerCategory(sp, customCats.get(i).id, 0);
                        return;
                    }
                }
            }
            return;
        }

        // ── Category items screen ──
        if (ctx.startsWith("shopcat:")) {
            String category = ctx.substring(8, ctx.lastIndexOf(':'));
            int page = 0;
            try { page = Integer.parseInt(ctx.substring(ctx.lastIndexOf(':') + 1)); } catch (Exception ignored) {}

            if (slot == 45 && page > 0)  { openShopServerCategory(sp, category, page - 1); return; }
            if (slot == 53)              { openShopServerCategory(sp, category, page + 1); return; }
            if (slot == 47)              { openShopCategories(sp); return; }
            if (slot == 51)              { sp.closeHandledScreen(); return; }
            if (slot >= 0 && slot < 45) {
                ShopItem si = shopSlotMap.get(slot);
                if (si != null) ShopManager.buy(sp, si);
                openShopServerCategory(sp, category, page);
            }
        }
    }

    private void handleShopPlayers(ServerPlayerEntity sp, int slot, int button) {
        String ctx = detailCtx.getOrDefault(sp.getUuid(), "shopplayers:0");
        int page = 0;
        try { if (ctx.startsWith("shopplayers:")) page = Integer.parseInt(ctx.substring(12)); } catch (Exception ignored) {}
        if (slot == 45 && page > 0) { openShopPlayers(sp, page - 1); return; }
        if (slot == 53) { openShopPlayers(sp, page + 1); return; }
        if (slot == 47) { openShopSelector(sp); return; }
        if (slot == 46) { // Botón vender
            ItemStack hand = sp.getMainHandStack();
            if (hand.isEmpty()) {
                sp.sendMessage(Text.literal("§c✘ Sostén un item en la mano para venderlo."), false);
                openShopPlayers(sp, page);
                return;
            }
            sellItemCtx.put(sp.getUuid(), hand.copy());
            sp.sendMessage(Text.literal("§6§l✦ §eVender: §f" + hand.getName().getString()), false);
            sp.sendMessage(Text.literal("§7Escribe el §eprecio §7en CobbleCoins (ej: §f5.50§7):"), false);
            sp.sendMessage(Text.literal("§7(escribe §fcancel §7para cancelar)"), false);
            awaitingInput.put(sp.getUuid(), "sell_price:0");
            return;
        }
        if (slot >= 0 && slot < 44) {
            ShopItem si = shopSlotMap.get(slot);
            if (si != null) ShopManager.buy(sp, si);
            openShopPlayers(sp, page);
        }
    }

    // ══════════════════════════════════════
    // UTILIDADES
    // ══════════════════════════════════════

    private static void open(ServerPlayerEntity sp, SimpleInventory inv,
                              String guiType, int rows, String title) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new EconomiaScreenHandler(syncId, playerInv, inv, guiType, rows),
            Text.literal(title)
        ));
    }

    private static void item(SimpleInventory inv, int slot, Item type, String name, String... lore) {
        ItemStack stack = new ItemStack(type);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        if (lore.length > 0) {
            List<Text> loreList = new ArrayList<>();
            for (String line : lore) loreList.add(Text.literal(line));
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreList));
        }
        if (slot >= 0 && slot < inv.size()) inv.setStack(slot, stack);
    }

    private static void fill(SimpleInventory inv, Item glass, String name) {
        ItemStack bg = new ItemStack(glass);
        bg.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        bg.set(DataComponentTypes.LORE, new LoreComponent(List.of()));
        for (int i = 0; i < inv.size(); i++) inv.setStack(i, bg.copy());
    }

    private static void border(SimpleInventory inv, Item borderItem) {
        ItemStack b = new ItemStack(borderItem);
        b.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§r "));
        b.set(DataComponentTypes.LORE, new LoreComponent(List.of()));
        for (int i = 0; i < 9; i++) inv.setStack(i, b.copy());
        for (int i = 45; i < 54; i++) inv.setStack(i, b.copy());
        for (int i = 0; i < 54; i += 9) inv.setStack(i, b.copy());
        for (int i = 8; i < 54; i += 9) inv.setStack(i, b.copy());
    }

    private static int findFreeSlot(SimpleInventory inv, int preferred) {
        if (preferred >= 0 && preferred < inv.size() && inv.getStack(preferred).isEmpty()) return preferred;
        for (int i = 0; i < inv.size() - 9; i++) if (inv.getStack(i).isEmpty()) return i;
        return -1;
    }

    private static UUID toUUID(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return UUID.randomUUID(); }
    }

    private static String formatAfk(long minutes) {
        if (minutes <= 0) return "0 min";
        long h = minutes / 60, m = minutes % 60;
        if (h >= 24) return (h/24) + "d " + (h%24) + "h " + m + "m";
        return h > 0 ? h + "h " + m + "m" : m + " min";
    }

    private static String rankDisplay(Rank rank) {
        if (rank == null) return "§7Trainer"; // null = base rank
        return switch (rank) {
            case TRAINER      -> "§7Trainer";
            case TRAINER_PLUS -> "§aTrainer+";
            case ELITE        -> "§bElite";
            case LEGENDARY    -> "§dLegendary";
            case MYTHICAL     -> "§6Mythical";
        };
    }

    private static Rank getRankByUuid(ServerPlayerEntity sp, UUID uuid) {
        ServerPlayerEntity target = sp.getServer().getPlayerManager().getPlayer(uuid);
        if (target != null) return RankManager.getRank(target);
        return RankManager.getRankByUuid(uuid);
    }

    private static Rank cycleRank(Rank current) {
        if (current == null || current == Rank.TRAINER) return Rank.TRAINER_PLUS;
        return switch (current) {
            case TRAINER      -> Rank.TRAINER_PLUS;
            case TRAINER_PLUS -> Rank.ELITE;
            case ELITE        -> Rank.LEGENDARY;
            case LEGENDARY    -> Rank.MYTHICAL;
            case MYTHICAL     -> Rank.TRAINER; // vuelve al base
        };
    }

    private static boolean setNpcFromLook(ServerPlayerEntity player) {
        net.minecraft.util.math.Vec3d start = player.getCameraPosVec(1.0F);
        net.minecraft.util.math.Vec3d dir = player.getRotationVec(1.0F);
        net.minecraft.util.math.Vec3d end = start.add(dir.multiply(5.0));
        var ctx2 = new net.minecraft.world.RaycastContext(start, end,
            net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
            net.minecraft.world.RaycastContext.FluidHandling.NONE, player);
        var hit = player.getWorld().raycast(ctx2);
        if (hit == null || hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS) return false;
        BlockPos pos = ((net.minecraft.util.hit.BlockHitResult) hit).getBlockPos();
        var entities = player.getWorld().getOtherEntities(player,
            new net.minecraft.util.math.Box(pos).expand(1.5),
            e -> e.getType() == EntityType.VILLAGER);
        if (entities.isEmpty()) return false;
        ModeconomiaConfig.DATA.questNpcUuid = entities.get(0).getUuid().toString();
        ModeconomiaConfig.save();
        return true;
    }

    // ══════════════════════════════════════
    // CHAT INPUT
    // ══════════════════════════════════════
    private static void saveMission(ServerPlayerEntity sp, String typeName, String displayName,
                                     String desc, String filter1, String filter2,
                                     int goal, double reward, long durMs) {
        try {
            com.cobblemania.economia.data.MissionType type =
                com.cobblemania.economia.data.MissionType.valueOf(typeName);
            String id = typeName.toLowerCase() + "_" + (System.currentTimeMillis() % 100000);
            MissionDefinition def = new MissionDefinition(id, displayName, type, goal);
            def.description = desc;       // texto visible al jugador
            def.filter1     = filter1;    // filtro interno (tipo, especie, ball, etc.)
            def.filter2     = filter2;    // filtro interno 2 (segunda ball, etc.)
            def.reward      = reward;
            def.expiresAt   = System.currentTimeMillis() + durMs;
            def.active      = true;
            ModeconomiaConfig.DATA.missions.definitions.add(def);
            ModeconomiaConfig.save();
            String durLabel = durMs == 86_400_000L ? "24h"
                : durMs < 3_600_000L   ? (durMs / 60_000)      + " min"
                : durMs < 86_400_000L  ? (durMs / 3_600_000)   + "h"
                : (durMs / 86_400_000) + "d";
            sp.sendMessage(Text.literal(
                "§a§l✔ Misión creada: §f" + displayName +
                " §8| §6" + MissionManager.format(reward) + " CC §8| §e" + durLabel), false);
        } catch (Exception e) {
            sp.sendMessage(Text.literal("§c✘ Error al guardar: " + e.getMessage()), false);
        }
        openMissionsList(sp);
    }

    /**
     * Returns the prompt message for filter N (1 or 2) of a mission type.
     * Returns null if no filter is needed at that position.
     */
    private static String getFilterPrompt(String typeName, int filterNum) {
        String ALL_TYPES = com.cobblemania.economia.util.PokemonTranslations.allTypesHint();
        String ALL_BALLS = com.cobblemania.economia.util.PokemonTranslations.allBallsHint();
        String ALL_RANKS = "§8Rangos (escribe exactamente): §fentrenador §8(Entrenador) §8· §flider §8(Líder Gimnasio) §8· §falto mando §8(Alto Mando) §8· §fcampeon §8(Campeón)";

        try {
            com.cobblemania.economia.data.MissionType type =
                com.cobblemania.economia.data.MissionType.valueOf(typeName);
            return switch (type) {
                case CAPTURE_SPECIFIC_TYPE, DEFEAT_SPECIFIC_TYPE,
                     CAPTURE_SHINY_SPECIFIC_TYPE, WIN_BATTLE_WITH_TYPE,
                     WIN_RANKED_WITH_TYPE ->
                    filterNum == 1
                        ? "§6§l▶ Filtro: Tipo de Pokémon\n§7Escribe el código §e(ej: §fFIRE§e, §fGHOST§e)§7:\n" + ALL_TYPES
                        : null;

                case CAPTURE_SPECIFIC_SPECIES, DEFEAT_SPECIFIC_SPECIES,
                     EVOLVE_SPECIFIC_SPECIES ->
                    filterNum == 1
                        ? "§6§l▶ Filtro: Especie\n§7Escribe el nombre §een inglés §7(ej: §fcharmander§7, §fpikachu§7, §fgengar§7, §feevee§7):"
                        : null;

                case CAPTURE_WITH_BALL, CAPTURE_SHINY_WITH_BALL ->
                    filterNum == 1
                        ? "§6§l▶ Filtro: Pokébola\n§7Escribe el código §e(ej: §fFAST_BALL§e, §fMASTER_BALL§e)§7:\n" + ALL_BALLS
                        : null;

                case CAPTURE_TYPE_WITH_BALL ->
                    filterNum == 1
                        ? "§6§l▶ Filtro 1: Tipo de Pokémon\n§7Escribe el código §e(ej: §fFIRE§e, §fGHOST§e)§7:\n" + ALL_TYPES
                        : filterNum == 2
                        ? "§6§l▶ Filtro 2: Pokébola\n§7Escribe el código §e(ej: §fFAST_BALL§e, §fNET_BALL§e)§7:\n" + ALL_BALLS
                        : null;

                case CAPTURE_LEVEL_RANGE ->
                    filterNum == 1
                        ? "§6§l▶ Filtro: Rango de nivel\n§7Escribe §fMIN:MAX §e(ej: §f20:40 §7para Pokémon nivel 20 a 40, §f1:10§7 para bebés):"
                        : null;

                case CAPTURE_IN_BIOME ->
                    filterNum == 1
                        ? "§6§l▶ Filtro: Bioma\n§7Escribe el ID del bioma §e(ej: §fforest§7, §fdesert§7, §focean§7, §fplains§7, §fswamp§7, §fjungle§7, §ftaiga§7):"
                        : null;

                case BREAK_SPECIFIC_BLOCK ->
                    filterNum == 1
                        ? "§6§l▶ Filtro: Bloque específico\n§7Escribe el ID §e(ej: §fminecraft:diamond_ore§7, §fminecraft:oak_log§7, §fminecraft:stone§7):"
                        : null;

                case WIN_RANKED_SPECIFIC_RANK ->
                    filterNum == 1
                        ? "§6§l▶ Filtro: Rango del rival\n§7Escribe §een minúsculas §e(ej: §fcampeon§7, §falto mando§7)§7:\n" + ALL_RANKS
                        : null;

                case REACH_ELO ->
                    filterNum == 1
                        ? "§6§l▶ Filtro: ELO mínimo\n§7Escribe el número §e(ej: §f1200§7, §f1500§7, §f2000§7):"
                        : null;

                default -> null;
            };
        } catch (Exception e) { return null; }
    }

    /** Parsea el string de filtros "F1¦F2" y llama saveMission */
    private static void saveMissionFromParts(ServerPlayerEntity sp,
            String typeName, String displayName, String desc, String filtersStr,
            int goal, double reward, long durMs) {
        String[] filters = filtersStr.split("¦", -1);
        String filter1 = filters.length > 0 ? filters[0] : "";
        String filter2 = filters.length > 1 ? filters[1] : "";
        saveMission(sp, typeName, displayName, desc, filter1, filter2, goal, reward, durMs);
    }

    public static void handleChatInput(ServerPlayerEntity sp, String message) {
        String ctx = awaitingInput.remove(sp.getUuid());
        if (ctx == null) return;

        if (message.equalsIgnoreCase("cancel")) {
            sp.sendMessage(Text.literal("§7Cancelado."), false);
            if      (ctx.startsWith("sell_price:"))       openShopSell(sp, sellItemCtx.getOrDefault(sp.getUuid(), ItemStack.EMPTY));
            else if (ctx.startsWith("sell_dur:"))         openShopSell(sp, sellItemCtx.getOrDefault(sp.getUuid(), ItemStack.EMPTY));
            else if (ctx.startsWith("mission_name:"))     openMissionTypePicker(sp);
            else if (ctx.startsWith("mission_desc§"))        openMissionTypePicker(sp);
            else if (ctx.startsWith("mission_goal§"))        openMissionsList(sp);
            else if (ctx.startsWith("mission_reward§"))      openMissionsList(sp);
            else if (ctx.startsWith("mission_dur§"))         openMissionsList(sp);
            else if (ctx.startsWith("mission_durunit§"))     openMissionsList(sp);
            else if (ctx.startsWith("mission_durmanual§"))   openMissionsList(sp);
            else if (ctx.startsWith("mission_extend:"))   openMissionsList(sp);
            else if (ctx.startsWith("mission_extunit:"))  openMissionsList(sp);
            else if (ctx.startsWith("mission_extamt:"))   openMissionsList(sp);
            else if (ctx.startsWith("shopitem_price:"))   openShopConfig(sp);
            else if (ctx.startsWith("medit_"))            { String midx = ctx.replaceAll("[^:]+:", ""); try { openMissionEdit(sp, Integer.parseInt(midx)); } catch (Exception e) { openMissionsList(sp); } }
            else if (ctx.startsWith("new_custom_cat"))    openCustomCategoriesAdmin(sp, 0);
            return;
        }

        // ── Nueva categoría custom — paso 1: nombre ──
        if (ctx.startsWith("new_custom_cat_name")) {
            // key format: "new_custom_cat_name§§<iconNbt>"  (§§ separator so icon nbt won't collide)
            String iconNbt = "";
            int sep = ctx.indexOf("§§");
            if (sep >= 0) iconNbt = ctx.substring(sep + 2);

            String name = message.trim();
            if (name.isEmpty() || name.length() > 24) {
                sp.sendMessage(Text.literal("§c✘ Nombre inválido (1-24 caracteres). Inténtalo de nuevo:"), false);
                awaitingInput.put(sp.getUuid(), ctx);
                return;
            }
            // Generar ID: minúsculas, sin espacios
            String id = name.toLowerCase().replaceAll("[^a-z0-9]", "_");
            // Verificar que no exista
            if (ModeconomiaConfig.DATA.shop.customCategories == null)
                ModeconomiaConfig.DATA.shop.customCategories = new java.util.ArrayList<>();
            for (ModeconomiaConfig.CustomCategory cc : ModeconomiaConfig.DATA.shop.customCategories) {
                if (cc.id.equals(id)) {
                    sp.sendMessage(Text.literal("§c✘ Ya existe una categoría con ese nombre (ID: §f" + id + "§c). Usa otro nombre:"), false);
                    awaitingInput.put(sp.getUuid(), ctx);
                    return;
                }
            }
            // Pedir descripción
            sp.sendMessage(Text.literal("§5§l✦ §dEscribe la §bdescripción §7de la categoría §f" + name + "§7:"), false);
            sp.sendMessage(Text.literal("§8(Descripción visible en el tooltip, ej: §fItems especiales del servidor§8)"), false);
            awaitingInput.put(sp.getUuid(), "new_custom_cat_desc§§" + id + "§§" + name + "§§" + iconNbt);
            return;
        }

        // ── Nueva categoría custom — paso 2: descripción ──
        if (ctx.startsWith("new_custom_cat_desc§§")) {
            String[] parts = ctx.split("§§", 4);
            String id      = parts.length > 1 ? parts[1] : "custom";
            String label   = parts.length > 2 ? parts[2] : id;
            String iconNbt = parts.length > 3 ? parts[3] : "";
            String desc    = message.trim();

            // Pedir color
            awaitingInput.put(sp.getUuid(), "new_custom_cat_color§§" + id + "§§" + label + "§§" + desc + "§§" + iconNbt);
            sp.sendMessage(Text.literal("§5§l✦ §dElige el color de §f" + label + "§d:"), false);
            sp.sendMessage(Text.literal("§c1§8) Rojo   §a2§8) Verde   §e3§8) Amarillo   §b4§8) Aqua"), false);
            sp.sendMessage(Text.literal("§d5§8) Rosa   §f6§8) Blanco  §68§8) Naranja    §57§8) Violeta"), false);
            sp.sendMessage(Text.literal("§8(Escribe el número o un código de color como §f§e§7)"), false);
            return;
        }

        // ── Nueva categoría custom — paso 3: color ──
        if (ctx.startsWith("new_custom_cat_color§§")) {
            String[] parts = ctx.split("§§", 5);
            String id      = parts.length > 1 ? parts[1] : "custom";
            String label   = parts.length > 2 ? parts[2] : id;
            String desc    = parts.length > 3 ? parts[3] : "";
            String iconNbt = parts.length > 4 ? parts[4] : "";
            String color = switch (message.trim()) {
                case "1" -> "§c";
                case "2" -> "§a";
                case "3" -> "§e";
                case "4" -> "§b";
                case "5" -> "§d";
                case "6" -> "§f";
                case "7" -> "§5";
                case "8" -> "§6";
                default  -> message.trim().startsWith("§") ? message.trim().substring(0,2) : "§f";
            };
            ModeconomiaConfig.CustomCategory cc = new ModeconomiaConfig.CustomCategory(id, label, color, desc, iconNbt);
            ModeconomiaConfig.DATA.shop.customCategories.add(cc);
            ModeconomiaConfig.save();
            sp.sendMessage(Text.literal("§a✔ Categoría " + color + "§l" + label + " §acreada con ID §7" + id + "§a."), false);
            sp.sendMessage(Text.literal("§7Ahora agrega items a esta categoría desde §e/ccoins owner §7→ Tienda."), false);
            openCustomCategoriesAdmin(sp, 0);
            return;
        }

        // ── Sell price ──
        if (ctx.startsWith("sell_price:")) {
            try {
                double price = Double.parseDouble(message.trim());
                if (price <= 0) throw new NumberFormatException();
                sellPriceCtx.put(sp.getUuid(), price);
                sp.sendMessage(Text.literal("§a✔ Precio establecido: §6" + MissionManager.format(price) + " CC"), false);
                sp.sendMessage(Text.literal("§7Ahora escribe la §eduración en minutos §7(ej: §f60§7):"), false);
                awaitingInput.put(sp.getUuid(), "sell_dur:" + ctx.substring(11));
            } catch (NumberFormatException e) {
                sp.sendMessage(Text.literal("§c✘ Escribe un número válido. Ej: §f5.50"), false);
                awaitingInput.put(sp.getUuid(), ctx);
            }
            return;
        }

        // ── Sell duration ──
        if (ctx.startsWith("sell_dur:")) {
            try {
                int dur = Integer.parseInt(message.trim());
                if (dur <= 0) throw new NumberFormatException();
                sellDurCtx.put(sp.getUuid(), dur);
                ItemStack stack = sellItemCtx.getOrDefault(sp.getUuid(), ItemStack.EMPTY);
                double price = sellPriceCtx.getOrDefault(sp.getUuid(), 1.0);
                if (!stack.isEmpty()) {
                    String nbt = ItemStackUtil.toNbtString(stack, sp.getServer().getRegistryManager());
                    ShopItem listing = new ShopItem(-1, nbt, price);
                    listing.sellerUuid = sp.getUuid().toString();
                    listing.durationMinutes = dur;
                    listing.expiresAt = System.currentTimeMillis() + dur * 60_000L;
                    ShopManager.addListing(listing);
                    sp.getInventory().setStack(sp.getInventory().selectedSlot, ItemStack.EMPTY);
                    sp.sendMessage(Text.literal("§a✔ Item listado por §6" + MissionManager.format(price)
                        + " CC §adurante §f" + dur + " min§a."), false);
                    sellItemCtx.remove(sp.getUuid());
                    sellPriceCtx.remove(sp.getUuid());
                    sellDurCtx.remove(sp.getUuid());
                }
                openShopPlayers(sp, 0);
            } catch (NumberFormatException e) {
                sp.sendMessage(Text.literal("§c✘ Escribe un número entero. Ej: §f60"), false);
                awaitingInput.put(sp.getUuid(), ctx);
            }
            return;
        }

        // ── Mission creation flow ──
        // Keys: mission_name:TYPE → mission_desc§TYPE§NAME → mission_filter§TYPE§NAME§DESC
        //       → mission_goal§TYPE§NAME§DESC§F1§F2 → mission_reward§... → mission_dur§...

        if (ctx.startsWith("mission_name:")) {
            String typeName    = ctx.substring(13);
            String displayName = message.trim();
            sp.sendMessage(Text.literal("§a✔ Nombre: §f" + displayName), false);
            sp.sendMessage(Text.literal("§7Escribe la §edescripción §7visible al jugador en el NPC:"), false);
            sp.sendMessage(Text.literal("§8(Esta es la descripción pública, ej: §fCaptura tipos fantasma con Veloz Ball§8)"), false);
            awaitingInput.put(sp.getUuid(), "mission_desc§" + typeName + "§" + displayName);
            return;
        }

        if (ctx.startsWith("mission_desc§")) {
            String[] parts = ctx.split("§", 3);
            if (parts.length < 3) { openMissionsList(sp); return; }
            String typeName = parts[1], displayName = parts[2];
            String desc = message.trim();
            sp.sendMessage(Text.literal("§a✔ Descripción: §f" + (desc.isEmpty() ? "(sin descripción)" : desc)), false);

            // Check if this mission type needs filter fields
            String filterPrompt = getFilterPrompt(typeName, 1);
            if (filterPrompt != null) {
                sp.sendMessage(Text.literal(filterPrompt), false);
                awaitingInput.put(sp.getUuid(), "mission_filter§" + typeName + "§" + displayName + "§" + desc + "§");
            } else {
                // No filters needed → go straight to goal
                sp.sendMessage(Text.literal("§7Escribe el §eobjetivo §7(número, ej: §f64§7):"), false);
                awaitingInput.put(sp.getUuid(), "mission_goal§" + typeName + "§" + displayName + "§" + desc + "§§");
            }
            return;
        }

        if (ctx.startsWith("mission_filter§")) {
            // Parts: [prefix, type, name, desc, filtersBuiltSoFar]
            String[] parts = ctx.split("§", 5);
            if (parts.length < 5) { openMissionsList(sp); return; }
            String typeName = parts[1], displayName = parts[2], desc = parts[3];
            String filtersSoFar = parts[4]; // "¦"-separated filter values built so far

            String newFilter = message.trim();
            // Use ¦ (broken bar) as filter separator — safe, can't be typed in chat
            String allFilters = filtersSoFar.isEmpty() ? newFilter : filtersSoFar + "¦" + newFilter;
            int filterCount = allFilters.split("¦").length;

            sp.sendMessage(Text.literal("§a✔ Filtro guardado: §f" + newFilter), false);

            // Check if there's a second filter needed
            String filter2Prompt = getFilterPrompt(typeName, 2);
            if (filter2Prompt != null && filterCount < 2) {
                sp.sendMessage(Text.literal(filter2Prompt), false);
                awaitingInput.put(sp.getUuid(), "mission_filter§" + typeName + "§" + displayName + "§" + desc + "§" + allFilters);
            } else {
                // All filters collected → go to goal
                sp.sendMessage(Text.literal("§7Escribe el §eobjetivo §7(número, ej: §f64§7):"), false);
                awaitingInput.put(sp.getUuid(), "mission_goal§" + typeName + "§" + displayName + "§" + desc + "§" + allFilters);
            }
            return;
        }

        if (ctx.startsWith("mission_goal§")) {
            String[] parts = ctx.split("§", 5);  // [prefix, type, name, desc, filters]
            if (parts.length < 5) { openMissionsList(sp); return; }
            try {
                int goal = Integer.parseInt(message.trim());
                if (goal <= 0) throw new NumberFormatException();
                sp.sendMessage(Text.literal("§a✔ Objetivo: §f" + goal), false);
                sp.sendMessage(Text.literal("§7Escribe la §erecompensa §7en CC (ej: §f10.00§7):"), false);
                awaitingInput.put(sp.getUuid(), "mission_reward§" + parts[1] + "§" + parts[2] + "§" + parts[3] + "§" + parts[4] + "§" + goal);
            } catch (NumberFormatException e) {
                sp.sendMessage(Text.literal("§c✘ Número inválido. Ej: §f64"), false);
                awaitingInput.put(sp.getUuid(), ctx);
            }
            return;
        }

        if (ctx.startsWith("mission_reward§")) {
            String[] parts = ctx.split("§", 6);  // [prefix, type, name, desc, filters, goal]
            if (parts.length < 6) { openMissionsList(sp); return; }
            try {
                double reward = Double.parseDouble(message.trim());
                if (reward <= 0) throw new NumberFormatException();
                sp.sendMessage(Text.literal("§a✔ Recompensa: §6" + MissionManager.format(reward) + " CC"), false);
                sp.sendMessage(Text.literal("§7¿Duración? Escribe §e24 §7(24h), o §emanual§7:"), false);
                awaitingInput.put(sp.getUuid(), "mission_dur§" + parts[1] + "§" + parts[2] + "§" + parts[3] + "§" + parts[4] + "§" + parts[5] + "§" + reward);
            } catch (NumberFormatException e) {
                sp.sendMessage(Text.literal("§c✘ Número inválido. Ej: §f10.00"), false);
                awaitingInput.put(sp.getUuid(), ctx);
            }
            return;
        }

        if (ctx.startsWith("mission_dur§")) {
            // Formato: mission_dur§TYPE§NAME§DESC§FILTERS§GOAL§REWARD
            // Split robusto desde el final para no romperse con § en desc
            String[] allParts = ctx.split("§");
            if (allParts.length < 7) { openMissionsList(sp); return; }
            String reward   = allParts[allParts.length - 1].trim();
            String goal     = allParts[allParts.length - 2].trim();
            String filters  = allParts[allParts.length - 3].trim();
            String typeName = allParts[1].trim();
            String name     = allParts[2].trim();
            StringBuilder descBuilder = new StringBuilder();
            for (int di = 3; di <= allParts.length - 4; di++) {
                if (descBuilder.length() > 0) descBuilder.append("§");
                descBuilder.append(allParts[di]);
            }
            String desc = descBuilder.toString();

            String trimmed = message.trim().toLowerCase();
            if (trimmed.equalsIgnoreCase("manual")) {
                sp.sendMessage(Text.literal("§7Presiona la unidad de tiempo:"), false);
                sp.sendMessage(Text.literal("§e  h §7→ horas   (ej: §fh §7luego §f2 §7= 2 horas)"), false);
                sp.sendMessage(Text.literal("§e  m §7→ minutos (ej: §fm §7luego §f54 §7= 54 minutos)"), false);
                sp.sendMessage(Text.literal("§e  d §7→ días    (ej: §fd §7luego §f2 §7= 2 días)"), false);
                awaitingInput.put(sp.getUuid(), "mission_durunit§" + typeName + "§" + name + "§" + desc + "§" + filters + "§" + goal + "§" + reward);
            } else {
                try {
                    int hours = Integer.parseInt(trimmed);
                    if (hours <= 0) throw new NumberFormatException();
                    saveMissionFromParts(sp, typeName, name, desc, filters,
                        Integer.parseInt(goal), Double.parseDouble(reward), hours * 3_600_000L);
                } catch (NumberFormatException e) {
                    sp.sendMessage(Text.literal("§c✘ Escribe §f24 §c(24h) o §emanual§c."), false);
                    awaitingInput.put(sp.getUuid(), ctx);
                }
            }
            return;
        }

        // Paso 1 de manual: el jugador elige la unidad (h, m, d)
        if (ctx.startsWith("mission_durunit§")) {
            // Formato: mission_durunit§TYPE§NAME§DESC§FILTERS§GOAL§REWARD
            String[] allParts = ctx.split("§");
            if (allParts.length < 7) { openMissionsList(sp); return; }
            String unit = message.trim().toLowerCase();
            if (!unit.equals("h") && !unit.equals("m") && !unit.equals("d")) {
                sp.sendMessage(Text.literal("§c✘ Unidad inválida. Escribe §eh§c, §em§c o §ed§c."), false);
                awaitingInput.put(sp.getUuid(), ctx);
                return;
            }
            // Reconstruir campos desde el final (robusto contra § en desc)
            String reward   = allParts[allParts.length - 1].trim();
            String goal     = allParts[allParts.length - 2].trim();
            String filters  = allParts[allParts.length - 3].trim();
            String typeName = allParts[1].trim();
            String name     = allParts[2].trim();
            StringBuilder descBuilder = new StringBuilder();
            for (int di = 3; di <= allParts.length - 4; di++) {
                if (descBuilder.length() > 0) descBuilder.append("§");
                descBuilder.append(allParts[di]);
            }
            String desc = descBuilder.toString();

            String unitLabel = unit.equals("h") ? "horas" : unit.equals("m") ? "minutos" : "días";
            sp.sendMessage(Text.literal("§7¿Cuántos §e" + unitLabel + " §7quieres? (ej: §f" +
                (unit.equals("h") ? "2" : unit.equals("m") ? "54" : "2") + "§7):"), false);
            awaitingInput.put(sp.getUuid(), "mission_durmanual§" + unit + "§" + typeName + "§" + name + "§" + desc + "§" + filters + "§" + goal + "§" + reward);
            return;
        }

        // Paso 2 de manual: el jugador escribe la cantidad
        if (ctx.startsWith("mission_durmanual§")) {
            // Formato: mission_durmanual§UNIT§TYPE§NAME§DESC§FILTERS§GOAL§REWARD
            // Usar split sin límite y tomar desde el final para robustez contra § en desc/name
            String[] allParts = ctx.split("§");
            if (allParts.length < 8) { openMissionsList(sp); return; }
            try {
                // Los últimos 2 campos siempre son GOAL y REWARD (números sin §)
                // Los primeros 2 son prefix y UNIT
                String unit   = allParts[1].trim();
                String reward = allParts[allParts.length - 1].trim();
                String goal   = allParts[allParts.length - 2].trim();
                // Filters = allParts[allParts.length - 3]
                // Reconstruir desc y name desde el medio (pueden contener § si se colaron)
                String typeName   = allParts[2].trim();
                String name       = allParts[3].trim();
                // desc = todo entre [4] y [length-4] inclusive, reunido con §
                StringBuilder descBuilder = new StringBuilder();
                for (int di = 4; di <= allParts.length - 4; di++) {
                    if (descBuilder.length() > 0) descBuilder.append("§");
                    descBuilder.append(allParts[di]);
                }
                String desc    = descBuilder.toString();
                String filters = allParts[allParts.length - 3].trim();

                long amount = Long.parseLong(message.trim());
                if (amount <= 0) throw new NumberFormatException();
                long durMs = switch (unit) {
                    case "h" -> amount * 3_600_000L;
                    case "m" -> amount * 60_000L;
                    case "d" -> amount * 86_400_000L;
                    default  -> amount * 3_600_000L;
                };
                String unitLabel = unit.equals("h") ? "hora(s)" : unit.equals("m") ? "minuto(s)" : "día(s)";
                sp.sendMessage(Text.literal("§a✔ Duración: §f" + amount + " " + unitLabel), false);
                saveMissionFromParts(sp, typeName, name, desc, filters,
                    Integer.parseInt(goal), Double.parseDouble(reward), durMs);
            } catch (NumberFormatException e) {
                sp.sendMessage(Text.literal("§c✘ Número inválido. Escribe solo el número (ej: §f54§c)."), false);
                awaitingInput.put(sp.getUuid(), ctx);
            }
            return;
        }

        // ── Extend/reactivate mission ──
        if (ctx.startsWith("mission_extend:")) {
            int mIdx = Integer.parseInt(ctx.substring(15));
            if (message.trim().equalsIgnoreCase("24")) {
                MissionDefinition def = ModeconomiaConfig.DATA.missions.definitions.get(mIdx);
                def.active    = true;
                def.expiresAt = System.currentTimeMillis() + 86_400_000L;
                ModeconomiaConfig.save();
                sp.sendMessage(Text.literal("§a✔ Misión §f" + def.displayName + " §aactivada por 24h."), false);
                openMissionsList(sp);
            } else if (message.trim().equalsIgnoreCase("manual")) {
                sp.sendMessage(Text.literal("§7Unidad: §em §7(min), §eh §7(horas), §ed §7(días):"), false);
                awaitingInput.put(sp.getUuid(), "mission_extunit:" + mIdx);
            } else {
                sp.sendMessage(Text.literal("§c✘ Escribe §e24 §co §emanual§c."), false);
                awaitingInput.put(sp.getUuid(), ctx);
            }
            return;
        }
        if (ctx.startsWith("mission_extunit:")) {
            String unit = message.trim().toLowerCase();
            if (!unit.equals("m") && !unit.equals("h") && !unit.equals("d")) {
                sp.sendMessage(Text.literal("§c✘ Escribe §em§c, §eh §co §ed§c."), false);
                awaitingInput.put(sp.getUuid(), ctx); return;
            }
            String mIdxStr = ctx.substring(16);
            sp.sendMessage(Text.literal("§7¿Cuánto? (ej: §f360§7):"), false);
            awaitingInput.put(sp.getUuid(), "mission_extamt:" + unit + ":" + mIdxStr);
            return;
        }
        if (ctx.startsWith("mission_extamt:")) {
            String rest = ctx.substring(15);
            int sep = rest.indexOf(":");
            String unit  = rest.substring(0, sep);
            int mIdx = Integer.parseInt(rest.substring(sep + 1));
            try {
                long amount = Long.parseLong(message.trim());
                long durMs  = switch (unit) {
                    case "h" -> amount * 3_600_000L;
                    case "d" -> amount * 86_400_000L;
                    default  -> amount * 60_000L;
                };
                MissionDefinition def = ModeconomiaConfig.DATA.missions.definitions.get(mIdx);
                def.active    = true;
                def.expiresAt = (def.isExpired() ? System.currentTimeMillis() : def.expiresAt) + durMs;
                ModeconomiaConfig.save();
                sp.sendMessage(Text.literal("§a✔ Misión §f" + def.displayName + " §aextendida/reactivada."), false);
            } catch (Exception e) {
                sp.sendMessage(Text.literal("§c✘ Número inválido."), false);
                awaitingInput.put(sp.getUuid(), ctx); return;
            }
            openMissionsList(sp);
            return;
        }

        // ── FIX 3: Mission field edits ──
        if (ctx.startsWith("medit_name:")) {
            int mIdx = Integer.parseInt(ctx.substring(11));
            ModeconomiaConfig.DATA.missions.definitions.get(mIdx).displayName = message.trim();
            ModeconomiaConfig.save();
            sp.sendMessage(Text.literal("§a✔ Nombre actualizado."), false);
            openMissionEdit(sp, mIdx); return;
        }
        if (ctx.startsWith("medit_desc:")) {
            int mIdx = Integer.parseInt(ctx.substring(11));
            ModeconomiaConfig.DATA.missions.definitions.get(mIdx).description = message.trim();
            ModeconomiaConfig.save();
            sp.sendMessage(Text.literal("§a✔ Descripción actualizada."), false);
            openMissionEdit(sp, mIdx); return;
        }
        if (ctx.startsWith("medit_reward:")) {
            int mIdx = Integer.parseInt(ctx.substring(13));
            try {
                double v = Double.parseDouble(message.trim());
                if (v <= 0) throw new NumberFormatException();
                ModeconomiaConfig.DATA.missions.definitions.get(mIdx).reward = v;
                ModeconomiaConfig.save();
                sp.sendMessage(Text.literal("§a✔ Recompensa actualizada: §6" + MissionManager.format(v) + " CC"), false);
                openMissionEdit(sp, mIdx);
            } catch (NumberFormatException e) {
                sp.sendMessage(Text.literal("§c✘ Número inválido."), false);
                awaitingInput.put(sp.getUuid(), ctx);
            }
            return;
        }
        if (ctx.startsWith("medit_goal:")) {
            int mIdx = Integer.parseInt(ctx.substring(11));
            try {
                int v = Integer.parseInt(message.trim());
                if (v <= 0) throw new NumberFormatException();
                ModeconomiaConfig.DATA.missions.definitions.get(mIdx).requiredAmount = v;
                ModeconomiaConfig.save();
                sp.sendMessage(Text.literal("§a✔ Objetivo actualizado: §f" + v), false);
                openMissionEdit(sp, mIdx);
            } catch (NumberFormatException e) {
                sp.sendMessage(Text.literal("§c✘ Número inválido."), false);
                awaitingInput.put(sp.getUuid(), ctx);
            }
            return;
        }

        // ── Shop item price (admin) ──
        if (ctx.startsWith("shopitem_price:")) {
            int shopSlot;
            try { shopSlot = Integer.parseInt(ctx.substring(15)); }
            catch (Exception e) { openShopConfig(sp); return; }
            try {
                double price = Double.parseDouble(message.trim());
                if (price <= 0) throw new NumberFormatException();
                ShopItem shopItem = ModeconomiaConfig.DATA.shop.items.stream()
                    .filter(i -> i.slot == shopSlot).findFirst().orElse(null);
                if (shopItem != null) {
                    shopItem.price = price;
                    ModeconomiaConfig.save();
                    sp.sendMessage(Text.literal("§a✔ Precio actualizado: §6" + MissionManager.format(price) + " CC"), false);
                }
            } catch (NumberFormatException e) {
                sp.sendMessage(Text.literal("§c✘ Escribe un número válido. Ej: §f5.50"), false);
                awaitingInput.put(sp.getUuid(), ctx); return;
            }
            openShopConfig(sp);
            return;
        }

        // ── Categoría custom para item de tienda ──
        if (ctx.startsWith("shopitem_customcat:")) {
            int shopSlot2;
            try { shopSlot2 = Integer.parseInt(ctx.substring(19)); }
            catch (Exception e) { openShopConfig(sp); return; }
            String requestedId = message.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
            List<ModeconomiaConfig.CustomCategory> cats2 = ModeconomiaConfig.DATA.shop.customCategories;
            boolean found2 = cats2 != null && cats2.stream().anyMatch(cc -> cc.id.equals(requestedId));
            if (!found2) {
                sp.sendMessage(Text.literal("§c✘ ID de categoría no existe: §f" + requestedId + "§c. Inténtalo de nuevo:"), false);
                awaitingInput.put(sp.getUuid(), ctx);
                return;
            }
            ShopItem shopItem2 = ModeconomiaConfig.DATA.shop.items.stream()
                .filter(i -> i.slot == shopSlot2).findFirst().orElse(null);
            if (shopItem2 != null) {
                shopItem2.category = requestedId;
                ModeconomiaConfig.save();
                sp.sendMessage(Text.literal("§a✔ Categoría asignada: §5" + requestedId), false);
            }
            openShopPriceEdit(sp, shopSlot2, 0);
            return;
        }

        sp.sendMessage(Text.literal("§c✘ Contexto no reconocido."), false);
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canUse(PlayerEntity player) { return true; }
}
