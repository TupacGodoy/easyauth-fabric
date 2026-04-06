package com.cobblemania.economia.gui;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.data.MissionDefinition;
import com.cobblemania.economia.data.ShopItem;
import com.cobblemania.economia.data.rank.Rank;
import com.cobblemania.economia.mission.MissionManager;
import com.cobblemania.economia.rank.RankManager;
import com.cobblemania.economia.shop.ShopManager;
import com.cobblemania.economia.util.ItemStackUtil;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import com.cobblemania.economia.data.PlayerMissionData;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;

public class MenuScreenHandler extends ScreenHandler {
	private final MenuKind kind;
	private final SimpleInventory menuInventory;
	private final MenuContext context;
	private final Map<Integer, UUID> playerSlots = new HashMap<>();
	private final Map<Integer, ShopItem> shopSlots = new HashMap<>();
	private final ServerPlayerEntity serverPlayer;
	private static final int SHOP_SELL_SLOT = 45;
	private static final int SHOP_CLOSE_SLOT = 53;
	private static final int SHOP_MENU_SIZE = 54;

	public MenuScreenHandler(int syncId, PlayerInventory playerInventory, MenuKind kind, MenuContext context, int size) {
		super(ModeconomiaScreenHandlers.MENU, syncId);
		this.kind = kind;
		this.context = context == null ? new MenuContext() : context;
		this.menuInventory = new SimpleInventory(size);
		this.serverPlayer = playerInventory.player instanceof ServerPlayerEntity server ? server : null;
		addMenuSlots();
		addPlayerSlots(playerInventory);
		if (this.serverPlayer != null) {
			refresh();
		}
	}

	public MenuScreenHandler(int syncId, PlayerInventory playerInventory, MenuKind kind, int size) {
		this(syncId, playerInventory, kind, new MenuContext(), size);
	}

	public MenuKind getKind() {
		return kind;
	}

	public int getMenuRows() {
		return menuInventory.size() / 9;
	}

	public List<Text> getTooltipLines(int slotIndex) {
		// El tooltip se construye directamente desde el lore del item en MenuScreen.
		// Este método devuelve lista vacía — toda la info está en el lore del item.
		return Collections.emptyList();
	}

	private void addMenuSlots() {
		int size = menuInventory.size();
		int rows = size / 9;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < 9; col++) {
				int index = row * 9 + col;
				addSlot(new Slot(menuInventory, index, 8 + col * 18, 18 + row * 18));
			}
		}
	}

	private void addPlayerSlots(PlayerInventory playerInventory) {
		int baseY = menuInventory.size() / 9 * 18 + 32;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, baseY + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(playerInventory, col, 8 + col * 18, baseY + 58));
		}
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (slotIndex < menuInventory.size()) {
			if (player instanceof ServerPlayerEntity serverPlayer) {
				handleMenuClick(slotIndex, button, actionType, serverPlayer);
			}
			return;
		}
		super.onSlotClick(slotIndex, button, actionType, player);
	}

	private void handleMenuClick(int slotIndex, int button, SlotActionType actionType, ServerPlayerEntity player) {
		switch (kind) {
			case ADMIN_MAIN -> handleAdminMain(slotIndex, player);
			case ECONOMY_ACTION -> handleEconomyAction(slotIndex, player);
			case ECONOMY_PLAYER_SELECT -> handleEconomyPlayerSelect(slotIndex, player);
			case ECONOMY_AMOUNT -> handleEconomyAmount(slotIndex, button, player);
			case MISSIONS_CONFIG -> handleMissionsConfig(slotIndex, button, player);
			case MISSIONS_LIST -> handleMissionsList(slotIndex, button, player);
			case MISSIONS_VIEW -> {
				if (slotIndex == 26) {
					player.closeHandledScreen();
				}
			}
			case AFK_CONFIG -> handleAfkConfig(slotIndex, button, player);
			case SHOP_VIEW -> handleShopView(slotIndex, player);
			case SHOP_CONFIG -> handleShopConfig(slotIndex, button, actionType, player);
			case SHOP_PRICE -> handleShopPrice(slotIndex, button, player);
			case SHOP_SELL -> handleShopSell(slotIndex, player);
			case SHOP_DURATION -> handleShopDuration(slotIndex, button, player);
			case RANKS_MAIN -> handleRanksMain(slotIndex, player);
			case RANK_MEMBERS -> handleRankMembers(slotIndex, player);
			case NPC_CONFIG -> handleNpcConfig(slotIndex, player);
			case SHOP_NPC_CONFIG -> handleShopNpcConfig(slotIndex, player);
			case PLAYER_LIST -> handlePlayerList(slotIndex, player);
			case PLAYER_DETAIL -> handlePlayerDetail(slotIndex, player);
			case PLAYER_MENU -> handlePlayerMenu(slotIndex, player);
		}
	}

	private void handleAdminMain(int slotIndex, ServerPlayerEntity player) {
		if (!player.hasPermissionLevel(4)) {
			player.closeHandledScreen();
			return;
		}
		if (slotIndex == 10) {
			open(player, MenuKind.ECONOMY_ACTION, new MenuContext());
		} else if (slotIndex == 12) {
			open(player, MenuKind.MISSIONS_CONFIG, new MenuContext());
		} else if (slotIndex == 14) {
			open(player, MenuKind.AFK_CONFIG, new MenuContext());
		} else if (slotIndex == 16) {
			open(player, MenuKind.SHOP_CONFIG, new MenuContext());
		} else if (slotIndex == 8) {
			open(player, MenuKind.PLAYER_LIST, new MenuContext());
		} else if (slotIndex == 20) {
			open(player, MenuKind.NPC_CONFIG, new MenuContext());
		} else if (slotIndex == 22) {
			open(player, MenuKind.RANKS_MAIN, new MenuContext());
		} else if (slotIndex == 24) {
			open(player, MenuKind.SHOP_VIEW, new MenuContext());
		} else if (slotIndex == 26) {
			player.closeHandledScreen();
		}
	}

	private void handleEconomyAction(int slotIndex, ServerPlayerEntity player) {
		if (slotIndex == 10) {
			context.economyAction = MenuContext.EconomyAction.GIVE;
			open(player, MenuKind.ECONOMY_PLAYER_SELECT, context);
		} else if (slotIndex == 12) {
			context.economyAction = MenuContext.EconomyAction.TAKE;
			open(player, MenuKind.ECONOMY_PLAYER_SELECT, context);
		} else if (slotIndex == 14) {
			context.economyAction = MenuContext.EconomyAction.SET;
			open(player, MenuKind.ECONOMY_PLAYER_SELECT, context);
		} else if (slotIndex == 16) {
			context.economyAction = MenuContext.EconomyAction.VIEW;
			open(player, MenuKind.ECONOMY_PLAYER_SELECT, context);
		} else if (slotIndex == 22) {
			open(player, MenuKind.ADMIN_MAIN, new MenuContext());
		}
	}

	private void handleEconomyPlayerSelect(int slotIndex, ServerPlayerEntity player) {
		UUID target = playerSlots.get(slotIndex);
		if (target == null) {
			if (slotIndex == 49) {
				open(player, MenuKind.ECONOMY_ACTION, context);
			}
			return;
		}
		context.targetPlayer = target;
		if (context.economyAction == MenuContext.EconomyAction.VIEW) {
			double balance = EconomyStorage.getBalance(target);
			player.sendMessage(Text.literal("Balance: " + MissionManager.format(balance) + " CobbleCoins."), false);
			open(player, MenuKind.ECONOMY_ACTION, new MenuContext());
			return;
		}
		context.amount = 0;
		open(player, MenuKind.ECONOMY_AMOUNT, context);
	}

	private void handleEconomyAmount(int slotIndex, int button, ServerPlayerEntity player) {
		double delta = switch (slotIndex) {
			case 10 -> -100;
			case 11 -> -10;
			case 12 -> -1;
			case 13 -> -0.1;
			case 15 -> 0.1;
			case 16 -> 1;
			case 17 -> 10;
			case 18 -> 100;
			default -> 0;
		};
		if (delta != 0) {
			context.amount = Math.max(0, context.amount + delta);
			refresh();
			return;
		}
		if (slotIndex == 22) {
			applyEconomyAction(player);
			// Volver al detalle del jugador si venimos de ahi
			if (context.detailPlayerUuid != null) {
				MenuContext ctx = new MenuContext();
				ctx.detailPlayerUuid = context.detailPlayerUuid;
				open(player, MenuKind.PLAYER_DETAIL, ctx);
			} else {
				open(player, MenuKind.ECONOMY_ACTION, new MenuContext());
			}
			return;
		}
		if (slotIndex == 26) {
			open(player, MenuKind.ECONOMY_PLAYER_SELECT, context);
		}
	}

	private void applyEconomyAction(ServerPlayerEntity player) {
		if (context.targetPlayer == null || context.economyAction == null) {
			return;
		}
		switch (context.economyAction) {
			case GIVE -> EconomyStorage.addBalance(context.targetPlayer, context.amount);
			case TAKE -> EconomyStorage.takeBalance(context.targetPlayer, context.amount);
			case SET -> EconomyStorage.setBalance(context.targetPlayer, context.amount);
			default -> {
			}
		}
		player.sendMessage(Text.literal("§a✔ §fAcción aplicada correctamente."), false);
	}

	private void handleMissionsConfig(int slotIndex, int button, ServerPlayerEntity player) {
		if (slotIndex == 10) {
			double delta = button == 0 ? 0.1 : -0.1;
			ModeconomiaConfig.DATA.missions.baseReward = Math.max(0, ModeconomiaConfig.DATA.missions.baseReward + delta);
			ModeconomiaConfig.save();
			refresh();
			return;
		}
		if (slotIndex == 12) {
			int delta = button == 0 ? 1 : -1;
			ModeconomiaConfig.DATA.missions.dailyCount = Math.max(1, ModeconomiaConfig.DATA.missions.dailyCount + delta);
			ModeconomiaConfig.save();
			refresh();
			return;
		}
		if (slotIndex == 14) {
			open(player, MenuKind.MISSIONS_LIST, new MenuContext());
			return;
		}
		if (slotIndex == 16) {
			if (setNpcFromLook(player)) {
				player.sendMessage(Text.literal("§a✔ §fNPC de misiones actualizado correctamente."), false);
			} else {
				player.sendMessage(Text.literal("§c✘ §fMira a un aldeano para asignarlo como NPC de misiones."), false);
			}
			return;
		}
		if (slotIndex >= 20 && slotIndex <= 23) {
			Rank rank = switch (slotIndex) {
				case 20 -> Rank.TRAINER_PLUS;
				case 21 -> Rank.ELITE;
				case 22 -> Rank.LEGENDARY;
				case 23 -> Rank.MYTHICAL;
				default -> null;
			};
			if (rank != null) {
				double delta = button == 0 ? 0.1 : -0.1;
				double current = ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(rank, 1.0);
				ModeconomiaConfig.DATA.missions.multipliers.put(rank, Math.max(1.0, current + delta));
				ModeconomiaConfig.save();
				refresh();
			}
			return;
		}
		if (slotIndex == 26) {
			open(player, MenuKind.ADMIN_MAIN, new MenuContext());
		}
	}

	private void handleMissionsList(int slotIndex, int button, ServerPlayerEntity player) {
		List<MissionDefinition> definitions = ModeconomiaConfig.DATA.missions.definitions;
		if (slotIndex >= 0 && slotIndex < definitions.size()) {
			MissionDefinition definition = definitions.get(slotIndex);
			int delta = button == 0 ? 1 : -1;
			definition.requiredAmount = Math.max(1, definition.requiredAmount + delta);
			ModeconomiaConfig.save();
			refresh();
			return;
		}
		if (slotIndex == 22) {
			open(player, MenuKind.MISSIONS_CONFIG, new MenuContext());
		}
	}

	private void handleAfkConfig(int slotIndex, int button, ServerPlayerEntity player) {
		if (slotIndex == 10) {
			BlockPos pos = player.getBlockPos();
			ModeconomiaConfig.DATA.afk.pos1X = pos.getX();
			ModeconomiaConfig.DATA.afk.pos1Y = pos.getY();
			ModeconomiaConfig.DATA.afk.pos1Z = pos.getZ();
			ModeconomiaConfig.DATA.afk.world = player.getWorld().getRegistryKey().getValue().toString();
			ModeconomiaConfig.save();
			refresh();
			return;
		}
		if (slotIndex == 12) {
			BlockPos pos = player.getBlockPos();
			ModeconomiaConfig.DATA.afk.pos2X = pos.getX();
			ModeconomiaConfig.DATA.afk.pos2Y = pos.getY();
			ModeconomiaConfig.DATA.afk.pos2Z = pos.getZ();
			ModeconomiaConfig.DATA.afk.world = player.getWorld().getRegistryKey().getValue().toString();
			ModeconomiaConfig.save();
			refresh();
			return;
		}
		if (slotIndex == 14) {
			double delta = button == 0 ? 0.1 : -0.1;
			ModeconomiaConfig.DATA.afk.baseReward = Math.max(0, ModeconomiaConfig.DATA.afk.baseReward + delta);
			ModeconomiaConfig.save();
			refresh();
			return;
		}
		if (slotIndex == 16) {
			int delta = button == 0 ? 1 : -1;
			ModeconomiaConfig.DATA.afk.intervalMinutes = Math.max(1, ModeconomiaConfig.DATA.afk.intervalMinutes + delta);
			ModeconomiaConfig.save();
			refresh();
			return;
		}
		if (slotIndex >= 20 && slotIndex <= 23) {
			Rank rank = switch (slotIndex) {
				case 20 -> Rank.TRAINER_PLUS;
				case 21 -> Rank.ELITE;
				case 22 -> Rank.LEGENDARY;
				case 23 -> Rank.MYTHICAL;
				default -> null;
			};
			if (rank != null) {
				double delta = button == 0 ? 0.1 : -0.1;
				double current = ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(rank, 1.0);
				ModeconomiaConfig.DATA.afk.multipliers.put(rank, Math.max(1.0, current + delta));
				ModeconomiaConfig.save();
				refresh();
			}
			return;
		}
		if (slotIndex == 26) {
			open(player, MenuKind.ADMIN_MAIN, new MenuContext());
		}
	}

	private void handleShopView(int slotIndex, ServerPlayerEntity player) {
		if (slotIndex == SHOP_SELL_SLOT) {
			context.shopPrice = Math.max(0.1, context.shopPrice);
			context.amount = context.shopPrice;
			context.shopSelling = false;
			open(player, MenuKind.SHOP_SELL, context);
			return;
		}
		if (slotIndex == SHOP_CLOSE_SLOT) {
			player.closeHandledScreen();
			return;
		}
		ShopItem item = shopSlots.get(slotIndex);
		if (item != null) {
			ShopManager.buy(player, item);
			refresh();
		}
	}

	private void handleShopSell(int slotIndex, ServerPlayerEntity player) {
		if (slotIndex == 11) {
			context.shopSelling = true;
			context.amount = context.shopPrice;
			open(player, MenuKind.SHOP_PRICE, context);
			return;
		}
		if (slotIndex == 15) {
			open(player, MenuKind.SHOP_DURATION, context);
			return;
		}
		if (slotIndex == 22) {
			if (createListing(player)) {
				open(player, MenuKind.SHOP_VIEW, new MenuContext());
			}
			return;
		}
		if (slotIndex == 26) {
			open(player, MenuKind.SHOP_VIEW, new MenuContext());
		}
	}

	private void handleShopDuration(int slotIndex, int button, ServerPlayerEntity player) {
		int delta = switch (slotIndex) {
			case 10 -> -60;
			case 11 -> -15;
			case 12 -> -5;
			case 14 -> 5;
			case 15 -> 15;
			case 16 -> 60;
			default -> 0;
		};
		if (delta != 0) {
			context.shopDurationMinutes = Math.max(1, context.shopDurationMinutes + delta);
			refresh();
			return;
		}
		if (slotIndex == 22) {
			if (createListing(player)) {
				open(player, MenuKind.SHOP_VIEW, new MenuContext());
			}
			return;
		}
		if (slotIndex == 26) {
			open(player, MenuKind.SHOP_SELL, context);
		}
	}

	private void handleShopConfig(int slotIndex, int button, SlotActionType actionType, ServerPlayerEntity player) {
		if (slotIndex == 53) {
			open(player, MenuKind.ADMIN_MAIN, new MenuContext());
			return;
		}
		if (slotIndex >= menuInventory.size()) {
			return;
		}
		ItemStack cursor = getCursorStack();
		ShopItem existing = getShopItem(slotIndex);
		if (!cursor.isEmpty()) {
			setShopItem(slotIndex, cursor.copy(), existing != null ? existing.price : 1.0);
			refresh();
			return;
		}
		if (actionType == SlotActionType.PICKUP && player.isSneaking()) {
			removeShopItem(slotIndex);
			refresh();
			return;
		}
		if (actionType == SlotActionType.PICKUP && existing != null) {
			context.selectedSlot = slotIndex;
			context.amount = existing.price;
			open(player, MenuKind.SHOP_PRICE, context);
		}
	}

	private void handleShopPrice(int slotIndex, int button, ServerPlayerEntity player) {
		double delta = switch (slotIndex) {
			case 10 -> -10;
			case 11 -> -1;
			case 12 -> -0.1;
			case 14 -> 0.1;
			case 15 -> 1;
			case 16 -> 10;
			default -> 0;
		};
		if (delta != 0) {
			context.amount = Math.max(0, context.amount + delta);
			refresh();
			return;
		}
		if (slotIndex == 22) {
			if (context.shopSelling) {
				context.shopPrice = Math.max(0.1, context.amount);
				context.shopSelling = false;
				open(player, MenuKind.SHOP_DURATION, context);
			} else {
				updateShopPrice(context.selectedSlot, context.amount);
				open(player, MenuKind.SHOP_CONFIG, new MenuContext());
			}
			return;
		}
		if (slotIndex == 26) {
			if (context.shopSelling) {
				context.shopSelling = false;
				open(player, MenuKind.SHOP_SELL, context);
			} else {
				open(player, MenuKind.SHOP_CONFIG, new MenuContext());
			}
		}
	}

	private void handleRanksMain(int slotIndex, ServerPlayerEntity player) {
		Rank rank = switch (slotIndex) {
			case 10 -> Rank.TRAINER_PLUS;
			case 12 -> Rank.ELITE;
			case 14 -> Rank.LEGENDARY;
			case 16 -> Rank.MYTHICAL;
			default -> null;
		};
		if (rank != null) {
			context.selectedRank = rank;
			open(player, MenuKind.RANK_MEMBERS, context);
			return;
		}
		if (slotIndex == 26) {
			open(player, MenuKind.ADMIN_MAIN, new MenuContext());
		}
	}

	private void handleRankMembers(int slotIndex, ServerPlayerEntity player) {
		UUID target = playerSlots.get(slotIndex);
		if (target == null) {
			if (slotIndex == 49) {
				open(player, MenuKind.RANKS_MAIN, new MenuContext());
			}
			return;
		}
		if (context.selectedRank == null) {
			return;
		}
		if (RankManager.isInRank(context.selectedRank, target)) {
			RankManager.removeFromRank(context.selectedRank, target);
		} else {
			RankManager.addToRank(context.selectedRank, target);
		}
		refresh();
	}

	private boolean setNpcFromLook(ServerPlayerEntity player) {
		Vec3d start = player.getCameraPosVec(1.0F);
		Vec3d rotation = player.getRotationVec(1.0F);
		Vec3d end = start.add(rotation.multiply(5.0));
		RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player);
		var hit = player.getWorld().raycast(context);
		if (hit == null || hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS) {
			return false;
		}
		BlockPos pos = ((net.minecraft.util.hit.BlockHitResult) hit).getBlockPos();
		var entities = player.getWorld().getOtherEntities(player, new net.minecraft.util.math.Box(pos).expand(1.5),
			entity -> entity.getType() == EntityType.VILLAGER);
		if (entities.isEmpty()) {
			return false;
		}
		ModeconomiaConfig.DATA.questNpcUuid = entities.get(0).getUuid().toString();
		ModeconomiaConfig.save();
		return true;
	}

	private void refresh() {
		menuInventory.clear();
		playerSlots.clear();
		switch (kind) {
			case ADMIN_MAIN -> buildAdminMain();
			case ECONOMY_ACTION -> buildEconomyAction();
			case ECONOMY_PLAYER_SELECT -> buildEconomyPlayerSelect();
			case ECONOMY_AMOUNT -> buildEconomyAmount();
			case MISSIONS_CONFIG -> buildMissionsConfig();
			case MISSIONS_LIST -> buildMissionsList();
			case MISSIONS_VIEW -> buildMissionsView();
			case AFK_CONFIG -> buildAfkConfig();
			case SHOP_VIEW -> buildShopView();
			case SHOP_CONFIG -> buildShopConfig();
			case SHOP_PRICE -> buildShopPrice();
			case SHOP_SELL -> buildShopSell();
			case SHOP_DURATION -> buildShopDuration();
			case RANKS_MAIN -> buildRanksMain();
			case RANK_MEMBERS -> buildRankMembers();
			case NPC_CONFIG -> buildNpcConfig();
			case SHOP_NPC_CONFIG -> buildShopNpcConfig();
			case PLAYER_LIST -> buildPlayerList();
			case PLAYER_DETAIL -> buildPlayerDetail();
			case PLAYER_MENU -> buildPlayerMenu();
		}
		sendContentUpdates();
	}

	private void buildAdminMain() {
		// ── Bordes decorativos ──
		ItemStack borde = icon(Items.PURPLE_STAINED_GLASS_PANE, "§5 ");
		ItemStack bordeD = icon(Items.GRAY_STAINED_GLASS_PANE, "§8 ");
		for (int s : new int[]{0,1,2,3,5,6,7,8,9,17,18,19,21,23,25}) set(s, borde);
		// ── Titulo ──
		set(4, icon(Items.NETHER_STAR,
			"§6§l✦ CobbleMania §e§l— Panel Admin §6§l✦",
			"§7Bienvenido al panel de administración.",
			"§8Solo accesible para OP nivel 4."));
		// ── Opciones ──
		set(10, icon(Items.EMERALD,
			"§a§l● Economía",
			"§7Administrar §6CobbleCoins §7de jugadores.",
			"§8» Dar, quitar, asignar o ver balance."));
		set(12, icon(Items.WRITABLE_BOOK,
			"§b§l● Misiones",
			"§7Configurar misiones diarias y NPC.",
			"§8» Editar objetivos, recompensas y multiplicadores.",
			"§8» Asignar NPC de misiones."));
		set(14, icon(Items.CLOCK,
			"§e§l● Zona AFK",
			"§7Configurar zona AFK y pagos automáticos.",
			"§8» Definir coordenadas Pos1/Pos2.",
			"§8» Configurar recompensa e intervalo."));
		set(16, icon(Items.CHEST,
			"§d§l● Tienda §7(Config)",
			"§7Configurar la tienda del servidor.",
			"§8» Agregar items con precio fijo.",
			"§8» Los jugadores pueden comprarlos con CC."));
		set(20, icon(Items.VILLAGER_SPAWN_EGG,
			"§6§l● NPC Misionero",
			"§7Gestionar el aldeano de misiones.",
			"§8» Spawnear en tu posición actual.",
			"§8» Remover el NPC registrado."));
		set(22, icon(Items.NAME_TAG,
			"§c§l● Rangos",
			"§7Gestionar rangos y multiplicadores.",
			"§8» Trainer+, Elite, Legendary, Mythical.",
			"§8» Afecta misiones y zona AFK."));
		set(24, icon(Items.PLAYER_HEAD,
			"§f§l● Jugadores",
			"§7Ver lista de todos los jugadores.",
			"§8» Balance, misiones, AFK y estadísticas.",
			"§8» Gestionar CC individualmente."));
		set(26, icon(Items.BARRIER,
			"§c§l✘ Cerrar",
			"§7Cierra el panel de administración."));
	}

	private void buildEconomyAction() {
		ItemStack b = icon(Items.GREEN_STAINED_GLASS_PANE, "§2 ");
		for (int s : new int[]{0,1,2,3,5,6,7,8,9,17,18,19,20,21,23,24,25}) set(s, b);
		set(4, icon(Items.EMERALD,
			"§a§l✦ Economía §7— §fAcciones",
			"§7Selecciona una acción para realizar.",
			"§8Requiere seleccionar jugador y monto."));
		set(10, icon(Items.LIME_CONCRETE,
			"§a§l[+] Dar CobbleCoins",
			"§7Agrega CobbleCoins al balance de un jugador.",
			"§8El jugador puede gastar los CC recibidos.",
			"§7► Click para continuar."));
		set(12, icon(Items.RED_CONCRETE,
			"§c§l[-] Quitar CobbleCoins",
			"§7Resta CobbleCoins del balance de un jugador.",
			"§8No puede bajar de 0.",
			"§7► Click para continuar."));
		set(14, icon(Items.GOLD_BLOCK,
			"§e§l[=] Asignar Balance",
			"§7Define el balance exacto del jugador.",
			"§8Sobreescribe el valor anterior.",
			"§7► Click para continuar."));
		set(16, icon(Items.PAPER,
			"§f§l[?] Ver Balance",
			"§7Muestra el balance actual de un jugador.",
			"§8Solo lectura — no modifica nada."));
		set(22, icon(Items.ARROW,
			"§7« Volver al panel",
			"§7Regresa al menú principal de admin."));
	}

	private void buildEconomyPlayerSelect() {
		int slot = 0;
		for (ServerPlayerEntity target : playerInventoryPlayerList()) {
			set(slot, icon(Items.PLAYER_HEAD, target.getGameProfile().getName(), "Selecciona jugador."));
			playerSlots.put(slot, target.getUuid());
			slot++;
			if (slot >= menuInventory.size()) {
				break;
			}
		}
		set(49, icon(Items.BARRIER, "Volver", "Regresa al menu."));
	}

	private void buildEconomyAmount() {
		ItemStack bg = icon(Items.GRAY_STAINED_GLASS_PANE, "§8 ");
		for (int s : new int[]{0,1,2,3,4,5,6,7,8,9,14,19,20,21,23,24,25}) set(s, bg);
		// ── Restar (izquierda) ──
		set(10, icon(Items.RED_STAINED_GLASS_PANE, "§c§l-100", "§7Resta §c100 §7al monto.", "§8Click izquierdo."));
		set(11, icon(Items.RED_STAINED_GLASS_PANE, "§c§l-10",  "§7Resta §c10 §7al monto."));
		set(12, icon(Items.RED_STAINED_GLASS_PANE, "§c§l-1",   "§7Resta §c1 §7al monto."));
		set(13, icon(Items.RED_STAINED_GLASS_PANE, "§c§l-0.1", "§7Resta §c0.1 §7al monto."));
		// ── Sumar (derecha) ──
		set(15, icon(Items.LIME_STAINED_GLASS_PANE, "§a§l+0.1", "§7Suma §a0.1 §7al monto."));
		set(16, icon(Items.LIME_STAINED_GLASS_PANE, "§a§l+1",   "§7Suma §a1 §7al monto."));
		set(17, icon(Items.LIME_STAINED_GLASS_PANE, "§a§l+10",  "§7Suma §a10 §7al monto."));
		set(18, icon(Items.LIME_STAINED_GLASS_PANE, "§a§l+100", "§7Suma §a100 §7al monto.", "§8Click izquierdo."));
		// ── Confirmar ──
		set(22, icon(Items.EMERALD,
			"§a§l✔ Confirmar: §6" + MissionManager.format(context.amount) + " CC",
			"§7Aplica la acción al jugador seleccionado.",
			"§8Monto: §6" + MissionManager.format(context.amount) + " CobbleCoins"));
		set(26, icon(Items.BARRIER, "§c§l✘ Cancelar", "§7Vuelve sin aplicar cambios."));
	}

	private void buildMissionsConfig() {
		ItemStack b = icon(Items.BLUE_STAINED_GLASS_PANE, "§1 ");
		for (int s : new int[]{0,1,2,3,5,6,7,8,9,17,18,19,24,25}) set(s, b);
		set(4, icon(Items.WRITABLE_BOOK,
			"§b§l✦ Config Misiones",
			"§7Ajusta las recompensas y multiplicadores.",
			"§8Click izq = aumentar | Click der = reducir"));
		set(10, icon(Items.EMERALD,
			"§a§lRecompensa base: §6" + MissionManager.format(ModeconomiaConfig.DATA.missions.baseReward) + " CC",
			"§7CobbleCoins por misión completada.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1",
			"§8Afectado por multiplicadores de rango."));
		set(12, icon(Items.PAPER,
			"§e§lMisiones diarias: §f" + ModeconomiaConfig.DATA.missions.dailyCount,
			"§7Cantidad de misiones disponibles por día.",
			"§8Click §aizq §8= +1 | Click §cder §8= -1"));
		set(14, icon(Items.BOOK,
			"§f§l✎ Editar objetivos",
			"§7Cambia los requerimientos de cada misión.",
			"§8Ej: romper 64 bloques, matar 10 mobs."));
		set(16, icon(Items.VILLAGER_SPAWN_EGG,
			"§6§l⚑ Asignar NPC",
			"§7Mira a un aldeano y haz click para asignarlo.",
			"§8El NPC abrirá el menú de misiones al interactuar."));
		// Multiplicadores de rango
		set(20, icon(Items.LIME_DYE,
			"§a§lTrainer+ ×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.TRAINER_PLUS, 1.0),
			"§7Multiplicador de recompensa para §aTrainer+§7.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1"));
		set(21, icon(Items.CYAN_DYE,
			"§b§lElite ×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.ELITE, 1.0),
			"§7Multiplicador de recompensa para §bElite§7.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1"));
		set(22, icon(Items.PURPLE_DYE,
			"§d§lLegendary ×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.LEGENDARY, 1.0),
			"§7Multiplicador de recompensa para §dLegendary§7.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1"));
		set(23, icon(Items.ORANGE_DYE,
			"§6§lMythical ×" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.MYTHICAL, 1.0),
			"§7Multiplicador de recompensa para §6Mythical§7.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1"));
		set(26, icon(Items.ARROW, "§7« Volver", "§7Regresa al panel de administración."));
	}

	private void buildMissionsList() {
		List<MissionDefinition> definitions = ModeconomiaConfig.DATA.missions.definitions;
		for (int i = 0; i < definitions.size() && i < menuInventory.size(); i++) {
			MissionDefinition def = definitions.get(i);
			set(i, icon(Items.BOOK, def.displayName + " (" + def.requiredAmount + ")",
				"Click izq/der para +/- 1"));
		}
		set(22, icon(Items.BARRIER, "Volver", "Regresa al menu."));
	}

	private void buildMissionsView() {
		if (serverPlayer == null) return;
		var progress = EconomyStorage.getMissionData(serverPlayer.getUuid());
		List<MissionDefinition> definitions = ModeconomiaConfig.DATA.missions.definitions;
		// Borde
		ItemStack borde = icon(Items.CYAN_STAINED_GLASS_PANE, "§3 ");
		for (int s = 0; s < 27; s++) {
			boolean esBorde = s < 9 || s > 17 || s == 9 || s == 17;
			if (esBorde && s != 4 && (s < definitions.size() == false || s >= 9))
				set(s, borde);
		}
		// Misiones
		double baseReward = ModeconomiaConfig.DATA.missions.baseReward;
		for (int i = 0; i < definitions.size() && i < 9; i++) {
			MissionDefinition def = definitions.get(i);
			int current = progress.progress.getOrDefault(def.id, 0);
			int goal = def.requiredAmount;
			boolean done = current >= goal;
			String status = done ? "§a§l✔ COMPLETADA" : "§e" + current + "§7/§f" + goal;
			double multiplier = 1.0;
			com.cobblemania.economia.data.rank.Rank rank = com.cobblemania.economia.rank.RankManager.getRank(serverPlayer);
			if (rank != null) multiplier = ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(rank, 1.0);
			double reward = baseReward * multiplier;
			set(i + 9, icon(
				done ? Items.LIME_CONCRETE : Items.ORANGE_CONCRETE,
				(done ? "§a§l" : "§e§l") + def.displayName,
				"§7Progreso: " + status,
				"§7Recompensa: §6+" + MissionManager.format(reward) + " CC",
				done ? "§8Ya recibiste la recompensa." : "§8Completa la misión para cobrar."
			));
		}
		set(4, icon(Items.NETHER_STAR,
			"§b§l✦ Misiones del día",
			"§7Completa misiones para ganar §6CobbleCoins§7.",
			"§8Se reinician cada día a medianoche.",
			"§7Completadas: §a" + progress.completed.size() + "§7/§f" + definitions.size()));
		set(26, icon(Items.BARRIER, "§c§l✘ Cerrar", "§7Cierra este menú."));
	}

	private void buildAfkConfig() {
		ItemStack b = icon(Items.YELLOW_STAINED_GLASS_PANE, "§e ");
		for (int s : new int[]{0,1,2,3,5,6,7,8,9,17,18,19,24,25}) set(s, b);
		set(4, icon(Items.CLOCK,
			"§e§l✦ Config Zona AFK",
			"§7Configura la zona donde los jugadores ganan CC.",
			"§8Parate en la esquina y usa Pos1/Pos2."));
		set(10, icon(Items.LIME_CONCRETE,
			"§a§l▶ Set Pos1",
			"§7Guarda tu posición actual como esquina 1.",
			"§8Coords: §f" + (serverPlayer != null ?
				serverPlayer.getBlockX()+", "+serverPlayer.getBlockY()+", "+serverPlayer.getBlockZ()
				: "desconocido"),
			"§7► Párate en la esquina y haz click."));
		set(12, icon(Items.LIME_CONCRETE,
			"§a§l▶ Set Pos2",
			"§7Guarda tu posición actual como esquina 2.",
			"§8Coords: §f" + (serverPlayer != null ?
				serverPlayer.getBlockX()+", "+serverPlayer.getBlockY()+", "+serverPlayer.getBlockZ()
				: "desconocido"),
			"§7► Párate en la esquina opuesta y haz click."));
		set(14, icon(Items.EMERALD,
			"§a§lRecompensa: §6" + MissionManager.format(ModeconomiaConfig.DATA.afk.baseReward) + " CC",
			"§7CobbleCoins por intervalo en zona AFK.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1"));
		set(16, icon(Items.CLOCK,
			"§e§lIntervalo: §f" + ModeconomiaConfig.DATA.afk.intervalMinutes + " min",
			"§7Minutos entre cada pago en zona AFK.",
			"§8Click §aizq §8= +1 min | Click §cder §8= -1 min"));
		set(20, icon(Items.LIME_DYE,
			"§a§lTrainer+ ×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.TRAINER_PLUS, 1.0),
			"§7Multiplicador AFK para §aTrainer+§7.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1"));
		set(21, icon(Items.CYAN_DYE,
			"§b§lElite ×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.ELITE, 1.0),
			"§7Multiplicador AFK para §bElite§7.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1"));
		set(22, icon(Items.PURPLE_DYE,
			"§d§lLegendary ×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.LEGENDARY, 1.0),
			"§7Multiplicador AFK para §dLegendary§7.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1"));
		set(23, icon(Items.ORANGE_DYE,
			"§6§lMythical ×" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.MYTHICAL, 1.0),
			"§7Multiplicador AFK para §6Mythical§7.",
			"§8Click §aizq §8= +0.1 | Click §cder §8= -0.1"));
		set(26, icon(Items.ARROW, "§7« Volver", "§7Regresa al panel de administración."));
	}

	private void buildShopView() {
		if (serverPlayer == null) {
			return;
		}
		ShopManager.cleanupExpiredListings();
		shopSlots.clear();
		boolean[] usedSlots = new boolean[menuInventory.size()];
		if (SHOP_SELL_SLOT < usedSlots.length) {
			usedSlots[SHOP_SELL_SLOT] = true;
		}
		if (SHOP_CLOSE_SLOT < usedSlots.length) {
			usedSlots[SHOP_CLOSE_SLOT] = true;
		}
		boolean dirtySlots = false;
		for (ShopItem item : ShopManager.getItems()) {
			if (item.slot < 0 || item.slot >= usedSlots.length) {
				continue;
			}
			ItemStack stack = ItemStackUtil.fromNbtString(item.itemNbt, serverPlayer.getServer().getRegistryManager());
			if (stack.isEmpty()) {
				continue;
			}
			String priceLabel = MissionManager.format(item.price);
			applyName(stack, stack.getName().getString() + " - " + priceLabel);
			appendLore(stack, "Precio: " + priceLabel + " CobbleCoins.", "Click para comprar.");
			set(item.slot, stack);
			shopSlots.put(item.slot, item);
			usedSlots[item.slot] = true;
		}
		for (ShopItem listing : ShopManager.getListings()) {
			if (listing.isExpired()) {
				continue;
			}
			int slot = listing.slot;
			if (slot < 0 || slot >= usedSlots.length || usedSlots[slot]) {
				slot = findNextFreeShopSlot(usedSlots);
				if (slot >= 0 && slot < usedSlots.length) {
					listing.slot = slot;
					dirtySlots = true;
				}
			}
			if (slot < 0 || slot >= usedSlots.length) {
				continue;
			}
			ItemStack stack = ItemStackUtil.fromNbtString(listing.itemNbt, serverPlayer.getServer().getRegistryManager());
			if (stack.isEmpty()) {
				continue;
			}
			String priceLabel = MissionManager.format(listing.price);
			applyName(stack, stack.getName().getString() + " - " + priceLabel);
			String seller = resolveSellerName(listing.sellerUuid);
			String remaining = formatDuration(listing.expiresAt - System.currentTimeMillis());
			appendLore(stack, "Precio: " + priceLabel + " CobbleCoins.", "Vendedor: " + seller, "Vence en " + remaining);
			set(slot, stack);
			shopSlots.put(slot, listing);
			usedSlots[slot] = true;
		}
		if (dirtySlots) {
			ModeconomiaConfig.save();
		}
		set(SHOP_SELL_SLOT, icon(Items.EMERALD, "§6§l+ Vender item", "§7Sostén un item en la mano, define precio y duración.", "§8Tu item estará visible para todos los jugadores."));
		set(SHOP_CLOSE_SLOT, icon(Items.BARRIER, "§c✘ Cerrar", "§7Cierra la tienda."));
	}

	private void buildShopConfig() {
		for (ShopItem item : ModeconomiaConfig.DATA.shop.items) {
			ItemStack stack = ItemStackUtil.fromNbtString(item.itemNbt, serverPlayer.getServer().getRegistryManager());
			if (stack.isEmpty()) {
				continue;
			}
			String priceLabel = MissionManager.format(item.price);
			applyName(stack, stack.getName().getString() + " (" + priceLabel + ")");
			appendLore(stack, "Click para editar precio.", "Shift-click para eliminar.");
			set(item.slot, stack);
		}
		set(53, icon(Items.BARRIER, "Volver", "Regresa al menu."));
	}

	private void buildShopPrice() {
		set(10, icon(Items.RED_STAINED_GLASS_PANE, "-10", "Resta 10."));
		set(11, icon(Items.RED_STAINED_GLASS_PANE, "-1", "Resta 1."));
		set(12, icon(Items.RED_STAINED_GLASS_PANE, "-0.1", "Resta 0.1."));
		set(14, icon(Items.GREEN_STAINED_GLASS_PANE, "+0.1", "Suma 0.1."));
		set(15, icon(Items.GREEN_STAINED_GLASS_PANE, "+1", "Suma 1."));
		set(16, icon(Items.GREEN_STAINED_GLASS_PANE, "+10", "Suma 10."));
		set(22, icon(Items.EMERALD, "Guardar: " + MissionManager.format(context.amount), "Guarda el precio."));
		set(26, icon(Items.BARRIER, "Volver", "Regresa al menu."));
	}

	private void buildShopSell() {
		set(11, icon(Items.EMERALD, "Precio: " + MissionManager.format(context.shopPrice), "Click para ajustar."));
		set(13, icon(Items.CHEST, "Item a vender", "Sostén el item en tu mano."));
		set(15, icon(Items.CLOCK, "Duración: " + context.shopDurationMinutes + " min", "Click para ajustar."));
		set(22, icon(Items.IRON_NUGGET, "Listar item", "Confirma y retira el item de tu mano."));
		set(26, icon(Items.BARRIER, "Cancelar", "Regresa a la tienda."));
	}

	private void buildShopDuration() {
		set(10, icon(Items.RED_STAINED_GLASS_PANE, "-60 min", "Reduce 1 hora."));
		set(11, icon(Items.RED_STAINED_GLASS_PANE, "-15 min", "Reduce 15 minutos."));
		set(12, icon(Items.RED_STAINED_GLASS_PANE, "-5 min", "Reduce 5 minutos."));
		set(14, icon(Items.GREEN_STAINED_GLASS_PANE, "+5 min", "Aumenta 5 minutos."));
		set(15, icon(Items.GREEN_STAINED_GLASS_PANE, "+15 min", "Aumenta 15 minutos."));
		set(16, icon(Items.GREEN_STAINED_GLASS_PANE, "+60 min", "Aumenta 1 hora."));
		set(22, icon(Items.CLOCK, "Guardar: " + context.shopDurationMinutes + " min", "Confirma duración."));
		set(26, icon(Items.BARRIER, "Volver", "Regresa al panel de venta."));
	}

	private void buildRanksMain() {
		ItemStack b = icon(Items.MAGENTA_STAINED_GLASS_PANE, "§d ");
		for (int s : new int[]{0,1,2,3,5,6,7,8,9,11,13,15,17,18,19,20,21,22,23,24,25}) set(s, b);
		set(4, icon(Items.NETHER_STAR,
			"§d§l✦ Gestión de Rangos",
			"§7Selecciona un rango para editar sus miembros.",
			"§8Los rangos afectan los multiplicadores de CC."));
		set(10, icon(Items.LIME_DYE,
			"§a§l▶ Trainer+",
			"§7Gestionar miembros del rango §aTrainer+§7.",
			"§8Misiones: §ax" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.TRAINER_PLUS, 1.5),
			"§8AFK: §ax" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.TRAINER_PLUS, 1.3),
			"§7► Click para ver miembros."));
		set(12, icon(Items.CYAN_DYE,
			"§b§l▶ Elite",
			"§7Gestionar miembros del rango §bElite§7.",
			"§8Misiones: §bx" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.ELITE, 1.9),
			"§8AFK: §bx" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.ELITE, 1.8),
			"§7► Click para ver miembros."));
		set(14, icon(Items.PURPLE_DYE,
			"§d§l▶ Legendary",
			"§7Gestionar miembros del rango §dLegendary§7.",
			"§8Misiones: §dx" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.LEGENDARY, 2.4),
			"§8AFK: §dx" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.LEGENDARY, 2.0),
			"§7► Click para ver miembros."));
		set(16, icon(Items.ORANGE_DYE,
			"§6§l▶ Mythical",
			"§7Gestionar miembros del rango §6Mythical§7.",
			"§8Misiones: §6x" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(Rank.MYTHICAL, 2.9),
			"§8AFK: §6x" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(Rank.MYTHICAL, 2.7),
			"§7► Click para ver miembros."));
		set(26, icon(Items.ARROW, "§7« Volver", "§7Regresa al panel de administración."));
	}

	private void buildRankMembers() {
		if (context.selectedRank == null) {
			return;
		}
		int slot = 0;
		for (ServerPlayerEntity target : playerInventoryPlayerList()) {
			String name = target.getGameProfile().getName();
			boolean inRank = RankManager.isInRank(context.selectedRank, target.getUuid());
			String label = (inRank ? "[Quitar] " : "[Agregar] ") + name;
			set(slot, icon(Items.PAPER, label, "Click para alternar."));
			playerSlots.put(slot, target.getUuid());
			slot++;
			if (slot >= menuInventory.size()) {
				break;
			}
		}
		set(49, icon(Items.BARRIER, "Volver", "Regresa al menu."));
	}

	private List<ServerPlayerEntity> playerInventoryPlayerList() {
		if (serverPlayer == null || serverPlayer.getServer() == null) {
			return java.util.Collections.emptyList();
		}
		return serverPlayer.getServer().getPlayerManager().getPlayerList();
	}

	private void set(int slot, ItemStack stack) {
		if (slot >= 0 && slot < menuInventory.size()) {
			menuInventory.setStack(slot, stack);
		}
	}

	private ItemStack icon(net.minecraft.item.Item item, String name) {
		ItemStack stack = new ItemStack(item);
		applyName(stack, name);
		return stack;
	}

	private ItemStack icon(net.minecraft.item.Item item, String name, String... loreLines) {
		ItemStack stack = new ItemStack(item);
		applyName(stack, name);
		applyLore(stack, loreLines);
		return stack;
	}

	private List<Text> tooltipAdminMain(int slotIndex) {
		return switch (slotIndex) {
			case 10 -> List.of(Text.literal("Esmeralda: administrar CobbleCoins."));
			case 12 -> List.of(Text.literal("Libro: configurar misiones y NPC."));
			case 14 -> List.of(Text.literal("Reloj: configurar zona AFK y pagos."));
			case 16 -> List.of(Text.literal("Cofre: configurar items y precios."));
			case 22 -> List.of(Text.literal("Etiqueta: gestionar rangos y bonos."));
			case 24 -> List.of(Text.literal("Cartel: abrir tienda para comprar."));
			case 26 -> List.of(Text.literal("Cerrar el menu."));
			default -> Collections.emptyList();
		};
	}

	private List<Text> tooltipEconomyAction(int slotIndex) {
		return switch (slotIndex) {
			case 10 -> List.of(Text.literal("Agrega monedas a un jugador."));
			case 12 -> List.of(Text.literal("Quita monedas a un jugador."));
			case 14 -> List.of(Text.literal("Define balance exacto."));
			case 16 -> List.of(Text.literal("Muestra el balance."));
			case 22 -> List.of(Text.literal("Regresa al menu."));
			default -> Collections.emptyList();
		};
	}

	private List<Text> tooltipEconomyPlayerSelect(int slotIndex) {
		if (slotIndex == 49) {
			return List.of(Text.literal("Regresa al menu."));
		}
		return List.of(Text.literal("Selecciona jugador."));
	}

	private List<Text> tooltipEconomyAmount(int slotIndex) {
		return switch (slotIndex) {
			case 10 -> List.of(Text.literal("Resta 100."));
			case 11 -> List.of(Text.literal("Resta 10."));
			case 12 -> List.of(Text.literal("Resta 1."));
			case 13 -> List.of(Text.literal("Resta 0.1."));
			case 15 -> List.of(Text.literal("Suma 0.1."));
			case 16 -> List.of(Text.literal("Suma 1."));
			case 17 -> List.of(Text.literal("Suma 10."));
			case 18 -> List.of(Text.literal("Suma 100."));
			case 22 -> List.of(Text.literal("Aplica la accion."));
			case 26 -> List.of(Text.literal("Regresa al menu."));
			default -> Collections.emptyList();
		};
	}

	private List<Text> tooltipMissionsConfig(int slotIndex) {
		return switch (slotIndex) {
			case 10 -> List.of(Text.literal("Click izq/der para +/- 0.1"));
			case 12 -> List.of(Text.literal("Click izq/der para +/- 1"));
			case 14 -> List.of(Text.literal("Cambia objetivos."));
			case 16 -> List.of(Text.literal("Mira a un aldeano."));
			case 20, 21, 22, 23 -> List.of(Text.literal("Click izq/der para +/- 0.1"));
			case 26 -> List.of(Text.literal("Regresa al menu."));
			default -> Collections.emptyList();
		};
	}

	private List<Text> tooltipMissionsList(int slotIndex) {
		if (slotIndex == 22) {
			return List.of(Text.literal("Regresa al menu."));
		}
		return List.of(Text.literal("Click izq/der para +/- 1"));
	}

	private List<Text> tooltipMissionsView(int slotIndex) {
		if (slotIndex == 26) {
			return List.of(Text.literal("Cierra el menu."));
		}
		return List.of(Text.literal("Recompensa base en CobbleCoins."));
	}

	private List<Text> tooltipAfkConfig(int slotIndex) {
		return switch (slotIndex) {
			case 10, 12 -> List.of(Text.literal("Usa tu posicion actual."));
			case 14 -> List.of(Text.literal("Click izq/der para +/- 0.1"));
			case 16 -> List.of(Text.literal("Click izq/der para +/- 1"));
			case 20, 21, 22, 23 -> List.of(Text.literal("Click izq/der para +/- 0.1"));
			case 26 -> List.of(Text.literal("Regresa al menu."));
			default -> Collections.emptyList();
		};
	}

	private List<Text> tooltipShopView(int slotIndex) {
		if (slotIndex == SHOP_SELL_SLOT) {
			return List.of(Text.literal("Vende el item que sostienes en la mano."), Text.literal("Ajusta precio y duración."));
		}
		if (slotIndex == SHOP_CLOSE_SLOT) {
			return List.of(Text.literal("Cierra el menu."));
		}
		return List.of(Text.literal("Click para comprar."), Text.literal("Revisa el precio y duración en la descripción."));
	}

	private List<Text> tooltipShopConfig(int slotIndex) {
		if (slotIndex == 53) {
			return List.of(Text.literal("Regresa al menu."));
		}
		return List.of(Text.literal("Click para editar precio."), Text.literal("Shift-click para eliminar."));
	}

	private List<Text> tooltipShopPrice(int slotIndex) {
		return switch (slotIndex) {
			case 10 -> List.of(Text.literal("Resta 10."));
			case 11 -> List.of(Text.literal("Resta 1."));
			case 12 -> List.of(Text.literal("Resta 0.1."));
			case 14 -> List.of(Text.literal("Suma 0.1."));
			case 15 -> List.of(Text.literal("Suma 1."));
			case 16 -> List.of(Text.literal("Suma 10."));
			case 22 -> List.of(Text.literal("Guarda el precio."));
			case 26 -> List.of(Text.literal("Regresa al menu."));
			default -> Collections.emptyList();
		};
	}

	private List<Text> tooltipShopSell(int slotIndex) {
		return switch (slotIndex) {
			case 11 -> List.of(Text.literal("Define cuánto cobrarás."));
			case 13 -> List.of(Text.literal("Sostén el item que quieres vender."));
			case 15 -> List.of(Text.literal("Elige cuánto durará la publicación."));
			case 22 -> List.of(Text.literal("Confirma la lista."));
			case 26 -> List.of(Text.literal("Regresa a la tienda."));
			default -> Collections.emptyList();
		};
	}

	private List<Text> tooltipShopDuration(int slotIndex) {
		return switch (slotIndex) {
			case 10 -> List.of(Text.literal("Reduce 60 minutos."));
			case 11 -> List.of(Text.literal("Reduce 15 minutos."));
			case 12 -> List.of(Text.literal("Reduce 5 minutos."));
			case 14 -> List.of(Text.literal("Aumenta 5 minutos."));
			case 15 -> List.of(Text.literal("Aumenta 15 minutos."));
			case 16 -> List.of(Text.literal("Aumenta 60 minutos."));
			case 22 -> List.of(Text.literal("Confirma duración."));
			case 26 -> List.of(Text.literal("Regresa al panel de venta."));
			default -> Collections.emptyList();
		};
	}

	private List<Text> tooltipRanksMain(int slotIndex) {
		return switch (slotIndex) {
			case 10, 12, 14, 16 -> List.of(Text.literal("Editar miembros."));
			case 26 -> List.of(Text.literal("Regresa al menu."));
			default -> Collections.emptyList();
		};
	}

	private List<Text> tooltipRankMembers(int slotIndex) {
		if (slotIndex == 49) {
			return List.of(Text.literal("Regresa al menu."));
		}
		return List.of(Text.literal("Click para alternar."));
	}

	private boolean createListing(ServerPlayerEntity player) {
		if (player.getServer() == null) {
			return false;
		}
		ItemStack stack = player.getInventory().getMainHandStack();
		if (stack.isEmpty()) {
			player.sendMessage(Text.literal("§c✘ §fSostén un item en tu mano para venderlo."), false);
			open(player, MenuKind.SHOP_SELL, context);
			return false;
		}
		int slot = findNextFreeShopSlot();
		if (slot < 0) {
			player.sendMessage(Text.literal("§c✘ §fLa tienda está llena. Espera a que se libere un espacio."), false);
			open(player, MenuKind.SHOP_SELL, context);
			return false;
		}
		double price = Math.max(0.1, context.shopPrice);
		int duration = Math.max(1, context.shopDurationMinutes);
		ItemStack listingStack = stack.copy();
		listingStack.setCount(stack.getCount());
		String nbt = ItemStackUtil.toNbtString(listingStack, player.getServer().getRegistryManager());
		ShopItem listing = new ShopItem(slot, nbt, price);
		listing.sellerUuid = player.getUuid().toString();
		listing.durationMinutes = duration;
		listing.expiresAt = System.currentTimeMillis() + duration * 60_000L;
		ShopManager.addListing(listing);
		player.getInventory().setStack(player.getInventory().selectedSlot, ItemStack.EMPTY);
		player.sendMessage(Text.literal("Listaste " + listingStack.getName().getString() + " por " + MissionManager.format(price) + " CobbleCoins durante " + duration + " minutos."), false);
		return true;
	}

	private int findNextFreeShopSlot(boolean[] usedSlots) {
		for (int i = 0; i < usedSlots.length; i++) {
			if (!usedSlots[i]) {
				return i;
			}
		}
		return -1;
	}

	private int findNextFreeShopSlot() {
		ShopManager.cleanupExpiredListings();
		boolean[] usedSlots = new boolean[SHOP_MENU_SIZE];
		if (SHOP_SELL_SLOT < usedSlots.length) {
			usedSlots[SHOP_SELL_SLOT] = true;
		}
		if (SHOP_CLOSE_SLOT < usedSlots.length) {
			usedSlots[SHOP_CLOSE_SLOT] = true;
		}
		for (ShopItem item : ShopManager.getItems()) {
			if (item.slot >= 0 && item.slot < usedSlots.length) {
				usedSlots[item.slot] = true;
			}
		}
		for (ShopItem listing : ShopManager.getListings()) {
			if (listing.slot >= 0 && listing.slot < usedSlots.length) {
				usedSlots[listing.slot] = true;
			}
		}
		return findNextFreeShopSlot(usedSlots);
	}

	private String resolveSellerName(String sellerUuid) {
		if (sellerUuid == null || sellerUuid.isEmpty()) {
			return "Administración";
		}
		try {
			UUID uuid = UUID.fromString(sellerUuid);
			if (serverPlayer != null && serverPlayer.getServer() != null) {
				ServerPlayerEntity seller = serverPlayer.getServer().getPlayerManager().getPlayer(uuid);
				if (seller != null) {
					return seller.getGameProfile().getName();
				}
			}
			return uuid.toString().substring(0, 8);
		} catch (IllegalArgumentException e) {
			return sellerUuid;
		}
	}

	private String formatDuration(long millis) {
		if (millis <= 0) {
			return "menor a 1 minuto";
		}
		long seconds = millis / 1000;
		long days = seconds / 86_400;
		long hours = (seconds % 86_400) / 3600;
		long minutes = (seconds % 3600) / 60;
		if (days > 0) {
			return days + "d " + hours + "h";
		}
		if (hours > 0) {
			return hours + "h " + minutes + "m";
		}
		if (minutes > 0) {
			return minutes + "m";
		}
		return seconds + "s";
	}

	private void applyName(ItemStack stack, String name) {
		Text title = Text.literal(name);
		stack.set(DataComponentTypes.ITEM_NAME, title);
		stack.set(DataComponentTypes.CUSTOM_NAME, title);
	}

	private void applyLore(ItemStack stack, String[] loreLines) {
		java.util.List<Text> lore = new java.util.ArrayList<>();
		for (String line : loreLines) {
			lore.add(Text.literal(line));
		}
		stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
	}

	private void appendLore(ItemStack stack, String... loreLines) {
		java.util.List<Text> lore = new java.util.ArrayList<>();
		LoreComponent existing = stack.get(DataComponentTypes.LORE);
		if (existing != null) {
			lore.addAll(existing.lines());
		}
		for (String line : loreLines) {
			lore.add(Text.literal(line));
		}
		stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
	}

	private ShopItem getShopItem(int slotIndex) {
		for (ShopItem item : ModeconomiaConfig.DATA.shop.items) {
			if (item.slot == slotIndex) {
				return item;
			}
		}
		return null;
	}

	private void setShopItem(int slotIndex, ItemStack stack, double price) {
		removeShopItem(slotIndex);
		if (serverPlayer == null) {
			return;
		}
		String nbt = ItemStackUtil.toNbtString(stack, serverPlayer.getServer().getRegistryManager());
		ModeconomiaConfig.DATA.shop.items.add(new ShopItem(slotIndex, nbt, price));
		ModeconomiaConfig.save();
	}

	private void removeShopItem(int slotIndex) {
		ModeconomiaConfig.DATA.shop.items.removeIf(item -> item.slot == slotIndex);
		ModeconomiaConfig.save();
	}

	private void updateShopPrice(int slotIndex, double price) {
		ShopItem item = getShopItem(slotIndex);
		if (item != null) {
			item.price = price;
			ModeconomiaConfig.save();
		}
	}



	// ── Menu del jugador (/ccoins) ──
	private void buildPlayerMenu() {
		if (serverPlayer == null) return;
		java.util.UUID uuid = serverPlayer.getUuid();
		double balance = EconomyStorage.getBalance(uuid);
		com.cobblemania.economia.data.PlayerMissionData data = EconomyStorage.getMissionData(uuid);
		com.cobblemania.economia.data.rank.Rank rank = com.cobblemania.economia.rank.RankManager.getRank(serverPlayer);
		String rankName = switch (rank) {
			case TRAINER      -> "§7Trainer";
			case TRAINER_PLUS -> "§aTrainer+";
			case ELITE        -> "§bElite";
			case LEGENDARY    -> "§dLegendary";
			case MYTHICAL     -> "§6Mythical";
		};
		String rankMult = (rank != com.cobblemania.economia.data.rank.Rank.TRAINER)
			? "§8Misiones x" + ModeconomiaConfig.DATA.missions.multipliers.getOrDefault(rank, 1.0)
			+ " | AFK x" + ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(rank, 1.0)
			: "§8Sin multiplicadores activos";
		List<MissionDefinition> defs = ModeconomiaConfig.DATA.missions.definitions;
		int completedToday = data.completed.size();
		int totalToday = defs != null ? defs.size() : 0;

		// ── Borde decorativo dorado ──
		ItemStack borde = icon(Items.GOLD_NUGGET, "§6 ");
		ItemStack bordeO = icon(Items.ORANGE_STAINED_GLASS_PANE, "§6 ");
		for (int s : new int[]{0,1,2,3,4,5,6,7,8,9,17,45,46,47,48,49,50,51,52,53}) set(s, bordeO);
		for (int s : new int[]{18,26,27,35,36,44}) set(s, borde);

		// ── Fila 2: Perfil ──
		set(10, icon(Items.PLAYER_HEAD,
			"§6§l✦ " + serverPlayer.getName().getString(),
			"§7Rango: " + rankName,
			rankMult,
			"§7Total ganado: §6" + MissionManager.format(data.totalEarned) + " CC",
			"§7Tiempo AFK: §e" + formatAfkTime(data.totalAfkMinutes)));

		set(13, icon(Items.GOLD_INGOT,
			"§6§lBalance: §f" + MissionManager.format(balance) + " CC",
			"§7Tus CobbleCoins disponibles.",
			"§8Usálos en la tienda o envíalos con /pay.",
			"§7► Usa §f/pay <jugador> <monto> §7para transferir."));

		set(16, icon(Items.DIAMOND,
			"§b§lEstadísticas",
			"§7Semana: §b" + data.weeklyCompleted + " misiones",
			"§7Mes: §d" + data.monthlyCompleted + " misiones",
			"§7Total histórico: §f" + data.totalDailyCompleted + " misiones",
			"§7CC ganados total: §6" + MissionManager.format(data.totalEarned)));

		// ── Fila 3: Misiones ──
		set(28, icon(completedToday >= totalToday ? Items.LIME_CONCRETE : Items.ORANGE_CONCRETE,
			"§b§lMisiones del día §7[" + completedToday + "/" + totalToday + "]",
			completedToday >= totalToday
				? "§a§l✔ ¡Completaste todas las misiones de hoy!"
				: "§7Te faltan §e" + (totalToday - completedToday) + " §7misiones hoy.",
			"§8Habla con el NPC §eMisionero §8para ver el detalle.",
			"§7Esta semana: §b" + data.weeklyCompleted + " §8| Este mes: §d" + data.monthlyCompleted));

		set(31, icon(Items.CHEST,
			"§d§l● Tienda",
			"§7Compra items del servidor o de otros jugadores.",
			"§8Tienda del server + mercado de jugadores.",
			"§7► Click para abrir la tienda."));

		set(34, icon(Items.CLOCK,
			"§e§lZona AFK",
			"§7Gana CC por estar en la zona AFK.",
			"§8Cada §f" + ModeconomiaConfig.DATA.afk.intervalMinutes + " min §8→ §6+"
				+ MissionManager.format(ModeconomiaConfig.DATA.afk.baseReward
					* (rank != null ? ModeconomiaConfig.DATA.afk.multipliers.getOrDefault(rank, 1.0) : 1.0))
				+ " CC",
			"§7► Usa §f/afk §7para teleportarte."));

		// ── Cerrar ──
		set(40, icon(Items.BARRIER, "§c§l✘ Cerrar", "§7Cierra este menú."));
	}

		private void handlePlayerMenu(int slotIndex, ServerPlayerEntity player) {
		if (slotIndex == 31) {
			open(player, MenuKind.SHOP_VIEW, new MenuContext());
		} else if (slotIndex == 40) {
			player.closeHandledScreen();
		}
	}

	// ── Lista de jugadores ──
	private static final int PAGE_SIZE = 45; // 5 filas x 9

	private void buildPlayerList() {
		if (serverPlayer == null) return;
		java.util.List<String> uuids = EconomyStorage.getAllPlayerUuids();
		// Ordenar por balance descendente
		uuids.sort((a, b) -> Double.compare(
			EconomyStorage.getBalance(java.util.UUID.fromString(b)),
			EconomyStorage.getBalance(java.util.UUID.fromString(a))
		));

		int page      = context.page;
		int total     = uuids.size();
		int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
		int start     = page * PAGE_SIZE;
		int end       = Math.min(start + PAGE_SIZE, total);

		for (int i = start; i < end; i++) {
			int slot = i - start;
			String uuidStr = uuids.get(i);
			try {
				java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
				PlayerMissionData data = EconomyStorage.getMissionData(uuid);
				double balance = EconomyStorage.getBalance(uuid);
				String name = data.playerName != null && !data.playerName.isEmpty()
					? data.playerName : uuidStr.substring(0, 8);

				// Color del nombre segun balance
				String nameColor = balance >= 100 ? "§6" : balance >= 10 ? "§e" : "§f";

				set(slot, icon(Items.PLAYER_HEAD,
					nameColor + name,
					"§7Balance: §6" + MissionManager.format(balance) + " CC",
					"§7Misiones hoy: §a" + data.completed.size(),
					"§7Misiones semana: §b" + data.weeklyCompleted,
					"§7Misiones mes: §d" + data.monthlyCompleted,
					"§7Total misiones: §f" + data.totalDailyCompleted,
					"§7Tiempo AFK: §e" + formatAfkTime(data.totalAfkMinutes),
					"§7Total ganado: §6" + MissionManager.format(data.totalEarned) + " CC",
					"§8Ultimo acceso: " + (data.lastSeenDate.isEmpty() ? "desconocido" : data.lastSeenDate),
					"§7Click para ver detalle y gestionar."
				));
				playerSlots.put(slot, uuid);
			} catch (Exception ignored) {}
		}

		// Navegación en fila 6 (slots 45-53)
		if (page > 0) {
			set(45, icon(Items.ARROW, "§7« Anterior", "§7Pagina " + page + " de " + totalPages));
		}
		set(49, icon(Items.PAPER,
			"§ePagina §f" + (page + 1) + " §ede §f" + totalPages,
			"§7Total jugadores: §f" + total));
		if (page < totalPages - 1) {
			set(53, icon(Items.ARROW, "§7Siguiente »", "§7Pagina " + (page + 2) + " de " + totalPages));
		}
		set(47, icon(Items.BARRIER, "§c✘ Volver", "§7Regresa al menu principal."));
	}

	private void handlePlayerList(int slotIndex, ServerPlayerEntity player) {
		if (slotIndex == 45 && context.page > 0) {
			context.page--;
			refresh();
		} else if (slotIndex == 53) {
			context.page++;
			refresh();
		} else if (slotIndex == 47) {
			open(player, MenuKind.ADMIN_MAIN, new MenuContext());
		} else {
			java.util.UUID target = playerSlots.get(slotIndex);
			if (target != null) {
				MenuContext ctx = new MenuContext();
				ctx.detailPlayerUuid = target.toString();
				open(player, MenuKind.PLAYER_DETAIL, ctx);
			}
		}
	}

	// ── Detalle de jugador ──
	private void buildPlayerDetail() {
		if (serverPlayer == null || context.detailPlayerUuid == null) return;
		try {
			java.util.UUID uuid = java.util.UUID.fromString(context.detailPlayerUuid);
			PlayerMissionData data = EconomyStorage.getMissionData(uuid);
			double balance = EconomyStorage.getBalance(uuid);
			String name = data.playerName != null && !data.playerName.isEmpty()
				? data.playerName : context.detailPlayerUuid.substring(0, 8);

			// INFO — columna izquierda
			set(10, icon(Items.GOLD_INGOT,
				"§6§lBalance: §f" + MissionManager.format(balance) + " CC",
				"§7Balance actual del jugador.",
				"§8Usa los botones de abajo para modificarlo."));
			set(19, icon(Items.WRITABLE_BOOK,
				"§b§lMisiones de hoy: §f" + data.completed.size(),
				"§7Completadas hoy: §a" + data.completed.size(),
				"§7Esta semana: §b" + data.weeklyCompleted,
				"§7Este mes: §d" + data.monthlyCompleted,
				"§7Total historico: §f" + data.totalDailyCompleted));
			set(28, icon(Items.CLOCK,
				"§e§lTiempo AFK: §f" + formatAfkTime(data.totalAfkMinutes),
				"§7Tiempo total en zona AFK.",
				"§7Total ganado lifetime: §6" + MissionManager.format(data.totalEarned) + " CC"));
			set(37, icon(Items.NETHER_STAR,
				"§d§lEstadisticas",
				"§7Ultimo acceso: §f" + (data.lastSeenDate.isEmpty() ? "desconocido" : data.lastSeenDate),
				"§7UUID: §8" + context.detailPlayerUuid.substring(0, 13) + "..."));

			// ACCIONES — columna derecha
			set(14, icon(Items.LIME_CONCRETE,
				"§a§l+ Dar CobbleCoins",
				"§7Abre el panel para dar CC a §e" + name + "§7."));
			set(23, icon(Items.RED_CONCRETE,
				"§c§l- Quitar CobbleCoins",
				"§7Abre el panel para quitar CC a §e" + name + "§7."));
			set(32, icon(Items.GOLD_BLOCK,
				"§e§l= Asignar Balance",
				"§7Define el balance exacto de §e" + name + "§7."));
			set(41, icon(Items.BARRIER,
				"§c§l✘ Resetear Misiones",
				"§7Reinicia todas las misiones diarias de §e" + name + "§7.",
				"§8El jugador podrá completarlas de nuevo hoy."));

			// Header con nombre
			set(4, icon(Items.PLAYER_HEAD,
				"§6§l" + name,
				"§7Gestionando al jugador §e" + name));

			// Volver
			set(49, icon(Items.ARROW, "§7« Volver a la lista", "§7Regresa a la lista de jugadores."));

		} catch (Exception e) {
			set(13, icon(Items.BARRIER, "§cError al cargar jugador", "§7UUID invalido o datos corruptos."));
		}
	}

	private void handlePlayerDetail(int slotIndex, ServerPlayerEntity player) {
		if (slotIndex == 49) {
			MenuContext ctx = new MenuContext();
			ctx.page = 0;
			open(player, MenuKind.PLAYER_LIST, ctx);
			return;
		}
		if (context.detailPlayerUuid == null) return;
		try {
			java.util.UUID target = java.util.UUID.fromString(context.detailPlayerUuid);
			if (slotIndex == 14) {
				context.targetPlayer = target;
				context.economyAction = MenuContext.EconomyAction.GIVE;
				context.amount = 0;
				open(player, MenuKind.ECONOMY_AMOUNT, context);
			} else if (slotIndex == 23) {
				context.targetPlayer = target;
				context.economyAction = MenuContext.EconomyAction.TAKE;
				context.amount = 0;
				open(player, MenuKind.ECONOMY_AMOUNT, context);
			} else if (slotIndex == 32) {
				context.targetPlayer = target;
				context.economyAction = MenuContext.EconomyAction.SET;
				context.amount = EconomyStorage.getBalance(target);
				open(player, MenuKind.ECONOMY_AMOUNT, context);
			} else if (slotIndex == 41) {
				// Reset misiones del jugador
				PlayerMissionData data = EconomyStorage.getMissionData(target);
				data.progress.clear();
				data.completed.clear();
				EconomyStorage.save();
				player.sendMessage(Text.literal("§a✔ Misiones de §e" +
					(data.playerName.isEmpty() ? context.detailPlayerUuid.substring(0,8) : data.playerName)
					+ " §areiniciadas."), false);
				refresh();
			}
		} catch (Exception e) {
			player.sendMessage(Text.literal("§c✘ Error al procesar accion."), false);
		}
	}

	private String formatAfkTime(long minutes) {
		if (minutes <= 0) return "0 min";
		long hours = minutes / 60;
		long mins  = minutes % 60;
		if (hours >= 24) {
			long days = hours / 24;
			hours = hours % 24;
			return days + "d " + hours + "h " + mins + "m";
		}
		if (hours > 0) return hours + "h " + mins + "m";
		return mins + " min";
	}

	// ── NPC Misionero ──
	private void buildNpcConfig() {
		String npcStatus = ModeconomiaConfig.DATA.questNpcUuid != null
			? "§aActivo §7(UUID: §f" + ModeconomiaConfig.DATA.questNpcUuid.substring(0, 8) + "...§7)"
			: "§cNo spawneado";
		set(4, icon(Items.VILLAGER_SPAWN_EGG, "§6§lNPC Misionero", "§7Estado: " + npcStatus));
		set(11, icon(Items.LIME_CONCRETE, "§a§l▶ Spawnear NPC",
			"§7Spawnea el aldeano Misionero en tu posicion actual.",
			"§8El NPC estará estático, invulnerable y sin trades.",
			"§8Los jugadores le hacen clic derecho para ver misiones."));
		set(13, icon(Items.PAPER, "§e§lℹ Estado del NPC",
			"§7Estado: " + npcStatus,
			"§8Si ya existe un NPC registrado, spawnear uno nuevo",
			"§8reemplazará el UUID guardado en la config."));
		set(15, icon(Items.RED_CONCRETE, "§c§l✘ Remover NPC",
			"§7Elimina el NPC Misionero registrado.",
			"§8Busca y descarta la entidad del mundo actual.",
			"§8No afecta a las misiones ni al progreso de jugadores."));
		set(22, icon(Items.ARROW, "§7« Volver", "§7Regresa al menu principal."));
	}

	private void handleNpcConfig(int slotIndex, ServerPlayerEntity player) {
		if (slotIndex == 11) {
			// Spawnear NPC en la posicion del admin
			ServerWorld world = (ServerWorld) player.getWorld();
			BlockPos pos = player.getBlockPos();
			VillagerEntity villager = EntityType.VILLAGER.spawn(
				world, pos, SpawnReason.COMMAND);
			if (villager == null) {
				player.sendMessage(Text.literal("§c✘ No se pudo spawnear el NPC."), false);
				return;
			}
			villager.setVillagerData(new VillagerData(
				VillagerType.PLAINS, VillagerProfession.NITWIT, 1));
			villager.setCustomName(Text.literal("§6§l✦ §eMisionero §6§l✦"));
			villager.setCustomNameVisible(true);
			villager.setAiDisabled(true);
			villager.setInvulnerable(true);
			villager.setPersistent();
			villager.setSilent(true);
			villager.getOffers().clear();
			ModeconomiaConfig.DATA.questNpcUuid = villager.getUuid().toString();
			ModeconomiaConfig.save();
			player.sendMessage(Text.literal("§a✔ NPC §e§lMisionero §aspawneado en tu posición."), false);
			refresh();
		} else if (slotIndex == 15) {
			// Remover NPC
			if (ModeconomiaConfig.DATA.questNpcUuid == null) {
				player.sendMessage(Text.literal("§c✘ No hay NPC Misionero registrado."), false);
				return;
			}
			try {
				java.util.UUID uuid = java.util.UUID.fromString(ModeconomiaConfig.DATA.questNpcUuid);
				ServerWorld world = (ServerWorld) player.getWorld();
				net.minecraft.entity.Entity entity = world.getEntity(uuid);
				if (entity != null) {
					entity.discard();
					player.sendMessage(Text.literal("§a✔ NPC Misionero eliminado correctamente."), false);
				} else {
					player.sendMessage(Text.literal(
						"§e⚠ Entidad no encontrada en este mundo. UUID eliminado de la config."), false);
				}
			} catch (IllegalArgumentException e) {
				player.sendMessage(Text.literal("§c✘ UUID inválido en la config."), false);
			}
			ModeconomiaConfig.DATA.questNpcUuid = null;
			ModeconomiaConfig.save();
			refresh();
		} else if (slotIndex == 22) {
			open(player, MenuKind.ADMIN_MAIN, new MenuContext());
		}
	}

	private List<Text> tooltipNpcConfig(int slotIndex) {
		return switch (slotIndex) {
			case 11 -> List.of(
				Text.literal("§7Spawnea el NPC en tu posición."),
				Text.literal("§8El UUID se guarda automáticamente."));
			case 13 -> List.of(Text.literal("§7Información del NPC registrado."));
			case 15 -> List.of(
				Text.literal("§7Elimina el NPC del mundo."),
				Text.literal("§8Se limpia el UUID de la config."));
			case 22 -> List.of(Text.literal("§7Regresa al menu principal."));
			default -> Collections.emptyList();
		};
	}

	private void buildShopNpcConfig() {
		String status = ModeconomiaConfig.DATA.shopNpcUuid != null
			? "§aActivo §7(UUID: §f" + ModeconomiaConfig.DATA.shopNpcUuid.substring(0, 8) + "...§7)"
			: "§cNo spawneado";
		set(4, icon(Items.CHEST, "§b§lNPC Tendero", "§7Estado: " + status));
		set(11, icon(Items.LIME_CONCRETE, "§a§l▶ Spawnear NPC",
			"§7Spawnea el aldeano Tendero en tu posicion actual.",
			"§8El NPC estará estático, invulnerable y sin trades.",
			"§8Los jugadores le hacen clic derecho para abrir la tienda."));
		set(13, icon(Items.PAPER, "§e§lℹ Estado del NPC",
			"§7Estado: " + status,
			"§8Si ya existe un NPC registrado, spawnear uno nuevo",
			"§8reemplazará el UUID guardado en la config."));
		set(15, icon(Items.RED_CONCRETE, "§c§l✘ Remover NPC",
			"§7Elimina el NPC Tendero registrado.",
			"§8Busca y descarta la entidad del mundo actual."));
		set(22, icon(Items.ARROW, "§7« Volver", "§7Regresa al menu principal."));
	}

	private void handleShopNpcConfig(int slotIndex, ServerPlayerEntity player) {
		if (slotIndex == 11) {
			ServerWorld world = (ServerWorld) player.getWorld();
			BlockPos pos = player.getBlockPos();
			VillagerEntity villager = EntityType.VILLAGER.spawn(world, pos, SpawnReason.COMMAND);
			if (villager == null) {
				player.sendMessage(Text.literal("§c✘ No se pudo spawnear el NPC."), false);
				return;
			}
			villager.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.CARTOGRAPHER, 2));
			villager.setCustomName(Text.literal("§b§l✦ §fTendero §b§l✦"));
			villager.setCustomNameVisible(true);
			villager.setAiDisabled(true);
			villager.setInvulnerable(true);
			villager.setPersistent();
			villager.setSilent(true);
			villager.getOffers().clear();
			ModeconomiaConfig.DATA.shopNpcUuid = villager.getUuid().toString();
			ModeconomiaConfig.save();
			player.sendMessage(Text.literal("§a✔ NPC §b§lTendero §aspawneado en tu posición."), false);
			refresh();
		} else if (slotIndex == 15) {
			if (ModeconomiaConfig.DATA.shopNpcUuid == null) {
				player.sendMessage(Text.literal("§c✘ No hay NPC Tendero registrado."), false);
				return;
			}
			try {
				java.util.UUID uuid = java.util.UUID.fromString(ModeconomiaConfig.DATA.shopNpcUuid);
				ServerWorld world = (ServerWorld) player.getWorld();
				net.minecraft.entity.Entity entity = world.getEntity(uuid);
				if (entity != null) {
					entity.discard();
					player.sendMessage(Text.literal("§a✔ NPC Tendero eliminado correctamente."), false);
				} else {
					player.sendMessage(Text.literal(
						"§e⚠ Entidad no encontrada en este mundo. UUID eliminado de la config."), false);
				}
			} catch (IllegalArgumentException e) {
				player.sendMessage(Text.literal("§c✘ UUID inválido en la config."), false);
			}
			ModeconomiaConfig.DATA.shopNpcUuid = null;
			ModeconomiaConfig.save();
			refresh();
		} else if (slotIndex == 22) {
			open(player, MenuKind.ADMIN_MAIN, new MenuContext());
		}
	}

	public static void open(ServerPlayerEntity player, MenuKind kind, MenuContext context) {
		player.openHandledScreen(new MenuFactory(kind, context));
	}

	private static class MenuFactory implements ExtendedScreenHandlerFactory {
		private final MenuKind kind;
		private final MenuContext context;

		private MenuFactory(MenuKind kind, MenuContext context) {
			this.kind = kind;
			this.context = context;
		}

		@Override
		public Integer getScreenOpeningData(ServerPlayerEntity player) {
			return kind.ordinal();
		}

		@Override
		public Text getDisplayName() {
			return Text.literal("§6§lCobbleMania §e— Economía");
		}

		@Override
		public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
			int size = switch (kind) {
				case SHOP_VIEW, SHOP_CONFIG, ECONOMY_PLAYER_SELECT, RANK_MEMBERS -> 54;
				case SHOP_SELL, SHOP_DURATION, NPC_CONFIG, SHOP_NPC_CONFIG -> 27;
				case PLAYER_LIST, PLAYER_DETAIL, PLAYER_MENU -> 54;
				default -> 27;
			};
			return new MenuScreenHandler(syncId, inv, kind, context, size);
		}
	}
}
