package com.cobblemania.economia.config;

import com.cobblemania.economia.main.CobbleMania;
import com.cobblemania.economia.data.MissionDefinition;
import com.cobblemania.economia.data.MissionType;
import com.cobblemania.economia.data.ShopItem;
import com.cobblemania.economia.data.rank.Rank;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModeconomiaConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "modeconomia.json";
	private static Path configPath;

	public static ModeconomiaConfigData DATA = new ModeconomiaConfigData();

	private ModeconomiaConfig() {
	}

	public static void load() {
		configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		if (!Files.exists(configPath)) {
			applyDefaults();
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(configPath)) {
			ModeconomiaConfigData loaded = GSON.fromJson(reader, ModeconomiaConfigData.class);
			if (loaded != null) {
				DATA = loaded;
			}
			applyDefaults();
		} catch (IOException | JsonParseException e) {
			CobbleMania.LOGGER.error("Failed to load Modeconomia config. Using defaults.", e);
			applyDefaults();
		}
	}

	public static void save() {
		if (configPath == null) {
			configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		}
		try (Writer writer = Files.newBufferedWriter(configPath)) {
			GSON.toJson(DATA, writer);
		} catch (IOException e) {
			CobbleMania.LOGGER.error("Failed to save Modeconomia config.", e);
		}
	}

	private static void applyDefaults() {
		if (DATA.afk == null) {
			DATA.afk = new AfkConfig();
		}
		if (DATA.missions == null) {
			DATA.missions = new MissionsConfig();
		}
		if (DATA.shop == null) {
			DATA.shop = new ShopConfig();
		}
		if (DATA.ranks == null) {
			DATA.ranks = new RankConfig();
		}
		if (DATA.ranks.rankMembers == null) {
			DATA.ranks.rankMembers = new EnumMap<>(Rank.class);
		}
		for (Rank rank : Rank.values()) {
			if (rank == Rank.TRAINER) continue;
			DATA.ranks.rankMembers.putIfAbsent(rank, new ArrayList<>());
		}

		if (DATA.afk.baseReward <= 0) {
			DATA.afk.baseReward = 0.3;
		}
		if (DATA.afk.intervalMinutes <= 0) {
			DATA.afk.intervalMinutes = 30;
		}
		if (DATA.afk.multipliers == null || DATA.afk.multipliers.isEmpty()) {
			DATA.afk.multipliers = defaultAfkMultipliers();
		}

		if (DATA.missions.baseReward <= 0) {
			DATA.missions.baseReward = 0.4;
		}
		if (DATA.missions.dailyCount <= 0) {
			DATA.missions.dailyCount = 5;
		}
		if (DATA.missions.multipliers == null || DATA.missions.multipliers.isEmpty()) {
			DATA.missions.multipliers = defaultMissionMultipliers();
		}
		if (DATA.missions.definitions == null || DATA.missions.definitions.isEmpty()) {
			DATA.missions.definitions = defaultMissions();
		}
		if (DATA.shop.items == null) {
			DATA.shop.items = new ArrayList<>();
		}
		if (DATA.shop.listings == null) {
			DATA.shop.listings = new ArrayList<>();
		}
		// Categorías custom — inicializar si null
		if (DATA.shop.customCategories == null) {
			DATA.shop.customCategories = new ArrayList<>();
		}
	}

	private static Map<Rank, Double> defaultAfkMultipliers() {
		Map<Rank, Double> values = new EnumMap<>(Rank.class);
		values.put(Rank.TRAINER_PLUS, 1.3);
		values.put(Rank.ELITE, 1.8);
		values.put(Rank.LEGENDARY, 2.0);
		values.put(Rank.MYTHICAL, 2.7);
		return values;
	}

	private static Map<Rank, Double> defaultMissionMultipliers() {
		Map<Rank, Double> values = new EnumMap<>(Rank.class);
		values.put(Rank.TRAINER_PLUS, 1.5);
		values.put(Rank.ELITE, 1.9);
		values.put(Rank.LEGENDARY, 2.4);
		values.put(Rank.MYTHICAL, 2.9);
		return values;
	}

	/**
	 * Pool de misiones disponibles para autogeneración diaria.
	 * Estas misiones se rotan automáticamente cada día (5 misiones/día, 3 CC c/u).
	 */
	private static List<MissionDefinition> defaultMissions() {
		List<MissionDefinition> missions = new ArrayList<>();

		// ════════════════════════════════════════════════════
		// VANILLA — PASIVAS (auto-tracking)
		// ════════════════════════════════════════════════════
		MissionDefinition m;
		m = new MissionDefinition("break_blocks", "Rompe bloques", MissionType.BREAK_BLOCKS, 64); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("kill_mobs", "Mata mobs", MissionType.KILL_MOBS, 10); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("walk_distance", "Camina distancia", MissionType.WALK_DISTANCE, 500); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("playtime", "Tiempo conectado", MissionType.PLAYTIME_MINUTES, 30); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("join_server", "Conéctate hoy", MissionType.JOIN_SERVER, 1); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("place_blocks", "Coloca bloques", MissionType.PLACE_BLOCKS, 32); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("craft_items", "Craftea items", MissionType.CRAFT_ITEMS, 16); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("fish_items", "Pesca veces", MissionType.FISH_ITEMS, 5); m.reward = 0.3; missions.add(m);

		// ════════════════════════════════════════════════════
		// COBBLEMON — CAPTURA BÁSICA (MANUAL)
		// ════════════════════════════════════════════════════
		m = new MissionDefinition("capture_pokemon", "Captura Pokémon", MissionType.CAPTURE_COBBLEMON, 5); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("capture_shiny", "Captura un shiny", MissionType.CAPTURE_SHINY_COBBLEMON, 1); m.reward = 3.0; missions.add(m);
		m = new MissionDefinition("capture_legendary", "Captura legendario", MissionType.CAPTURE_LEGENDARY_COBBLEMON, 1); m.reward = 5.0; missions.add(m);
		m = new MissionDefinition("capture_radiant", "Captura radiante", MissionType.CAPTURE_RADIANT_COBBLEMON, 1); m.reward = 4.0; missions.add(m);
		m = new MissionDefinition("capture_paradox", "Captura paradoja", MissionType.CAPTURE_PARADOX_COBBLEMON, 2); m.reward = 2.0; missions.add(m);

		// ════════════════════════════════════════════════════
		// COBBLEMON — CAPTURA CON FILTRO (MANUAL)
		// ════════════════════════════════════════════════════
		// Tipos elementales
		m = new MissionDefinition("catch_fire", "Captura tipo Fuego", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "fire"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_water", "Captura tipo Agua", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "water"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_grass", "Captura tipo Planta", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "grass"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_electric", "Captura tipo Eléctrico", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "electric"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_psychic", "Captura tipo Psíquico", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "psychic"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_ghost", "Captura tipo Fantasma", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "ghost"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_dragon", "Captura tipo Dragón", MissionType.CAPTURE_SPECIFIC_TYPE, 2, "dragon"); m.reward = 0.4; missions.add(m);

		// Poké Balls
		m = new MissionDefinition("use_poke_ball", "Usa Poké Ball", MissionType.CAPTURE_WITH_BALL, 3, "poke_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_fast_ball", "Usa Fast Ball", MissionType.CAPTURE_WITH_BALL, 3, "fast_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_quick_ball", "Usa Quick Ball", MissionType.CAPTURE_WITH_BALL, 3, "quick_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_dusk_ball", "Usa Dusk Ball", MissionType.CAPTURE_WITH_BALL, 3, "dusk_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_net_ball", "Usa Net Ball", MissionType.CAPTURE_WITH_BALL, 3, "net_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_dive_ball", "Usa Dive Ball", MissionType.CAPTURE_WITH_BALL, 3, "dive_ball"); m.reward = 0.3; missions.add(m);

		// ════════════════════════════════════════════════════
		// COBBLEMON — CONDICIONES ESPECIALES (MANUAL)
		// ════════════════════════════════════════════════════
		m = new MissionDefinition("catch_at_night", "Captura de noche", MissionType.CATCH_AT_NIGHT, 3); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("catch_during_rain", "Captura con lluvia", MissionType.CATCH_DURING_RAIN, 3); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("catch_no_damage", "Captura sin daño", MissionType.CATCH_WITHOUT_DAMAGE, 2); m.reward = 0.5; missions.add(m);

		// ════════════════════════════════════════════════════
		// COBBLEMON — COMBATE (MANUAL)
		// ════════════════════════════════════════════════════
		m = new MissionDefinition("defeat_wild", "Derrota salvajes", MissionType.DEFEAT_WILD_COBBLEMON, 10); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("win_pvp", "Gana batalla PvP", MissionType.WIN_TRAINER_BATTLE, 1); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("defeat_fire", "Derrota tipo Fuego", MissionType.DEFEAT_SPECIFIC_TYPE, 5, "fire"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("defeat_water", "Derrota tipo Agua", MissionType.DEFEAT_SPECIFIC_TYPE, 5, "water"); m.reward = 0.3; missions.add(m);

		// ════════════════════════════════════════════════════
		// COBBLEMON — EVOLUCIÓN Y CRIANZA (MANUAL)
		// ════════════════════════════════════════════════════
		m = new MissionDefinition("evolve_pokemon", "Evoluciona Pokémon", MissionType.EVOLVE_COBBLEMON, 2); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("hatch_egg", "Eclosiona huevos", MissionType.HATCH_EGG, 1); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("evolve_with_stone", "Evoluciona con piedra", MissionType.EVOLVE_USING_STONE, 2); m.reward = 0.5; missions.add(m);

		// ════════════════════════════════════════════════════
		// COBBLEMON — INTERACCIÓN (MANUAL)
		// ════════════════════════════════════════════════════
		m = new MissionDefinition("trade_pokemon", "Intercambia Pokémon", MissionType.TRADE_POKEMON, 1); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("nickname_pokemon", "Pon apodo", MissionType.NICKNAME_POKEMON, 3); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("revive_fossil", "Revive fósil", MissionType.REVIVE_FOSSIL, 1); m.reward = 0.5; missions.add(m);

		// ════════════════════════════════════════════════════
		// COBBLEMON — COLECCIÓN (PASSIVE)
		// ════════════════════════════════════════════════════
		m = new MissionDefinition("pokedex_entries", "Registra en Pokédex", MissionType.COLLECT_POKEDEX_ENTRIES, 5); m.reward = 0.3; missions.add(m);

		// ════════════════════════════════════════════════════
		// COBBLEMON — NUEVAS MISIONES VARIADAS (MANUAL)
		// ════════════════════════════════════════════════════
		// Más tipos de captura
		m = new MissionDefinition("catch_steel", "Captura tipo Acero", MissionType.CAPTURE_SPECIFIC_TYPE, 2, "steel"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("catch_fairy", "Captura tipo Hada", MissionType.CAPTURE_SPECIFIC_TYPE, 2, "fairy"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("catch_dark", "Captura tipo Siniestro", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "dark"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_flying", "Captura tipo Volador", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "flying"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_ground", "Captura tipo Tierra", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "ground"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_bug", "Captura tipo Bicho", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "bug"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_ice", "Captura tipo Hielo", MissionType.CAPTURE_SPECIFIC_TYPE, 2, "ice"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("catch_poison", "Captura tipo Veneno", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "poison"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_fighting", "Captura tipo Lucha", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "fighting"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_rock", "Captura tipo Roca", MissionType.CAPTURE_SPECIFIC_TYPE, 3, "rock"); m.reward = 0.3; missions.add(m);

		// Más bolas especializadas
		m = new MissionDefinition("use_luxury_ball", "Usa Luxury Ball", MissionType.CAPTURE_WITH_BALL, 3, "luxury_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_heal_ball", "Usa Heal Ball", MissionType.CAPTURE_WITH_BALL, 3, "heal_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_repeat_ball", "Usa Repeat Ball", MissionType.CAPTURE_WITH_BALL, 3, "repeat_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_timer_ball", "Usa Timer Ball", MissionType.CAPTURE_WITH_BALL, 3, "timer_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_nest_ball", "Usa Nest Ball", MissionType.CAPTURE_WITH_BALL, 3, "nest_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_lure_ball", "Usa Lure Ball", MissionType.CAPTURE_WITH_BALL, 3, "lure_ball"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("use_moon_ball", "Usa Moon Ball", MissionType.CAPTURE_WITH_BALL, 2, "moon_ball"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("use_friend_ball", "Usa Friend Ball", MissionType.CAPTURE_WITH_BALL, 2, "friend_ball"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("use_level_ball", "Usa Level Ball", MissionType.CAPTURE_WITH_BALL, 2, "level_ball"); m.reward = 0.4; missions.add(m);

		// Capturas con condiciones combinadas
		m = new MissionDefinition("catch_fire_type_with_ball", "Captura Fuego con Dusk Ball", MissionType.CAPTURE_TYPE_WITH_BALL, 2, "fire"); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("catch_water_type_with_ball", "Captura Agua con Net Ball", MissionType.CAPTURE_TYPE_WITH_BALL, 2, "water"); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("catch_ghost_at_night", "Captura Fantasma de noche", MissionType.CAPTURE_SPECIFIC_TYPE, 2, "ghost"); m.reward = 0.5; missions.add(m);

		// Más misiones de combate
		m = new MissionDefinition("defeat_ghost", "Derrota tipo Fantasma", MissionType.DEFEAT_SPECIFIC_TYPE, 5, "ghost"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("defeat_psychic", "Derrota tipo Psíquico", MissionType.DEFEAT_SPECIFIC_TYPE, 5, "psychic"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("defeat_dragon", "Derrota tipo Dragón", MissionType.DEFEAT_SPECIFIC_TYPE, 3, "dragon"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("defeat_steel", "Derrota tipo Acero", MissionType.DEFEAT_SPECIFIC_TYPE, 5, "steel"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("defeat_fairy", "Derrota tipo Hada", MissionType.DEFEAT_SPECIFIC_TYPE, 5, "fairy"); m.reward = 0.3; missions.add(m);

		// Misiones de evolución específicas
		m = new MissionDefinition("evolve_with_stone_fire", "Evoluciona con Piedra Fuego", MissionType.EVOLVE_USING_STONE, 1); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("evolve_with_stone_water", "Evoluciona con Piedra Agua", MissionType.EVOLVE_USING_STONE, 1); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("evolve_with_stone_thunder", "Evoluciona con Piedra Trueno", MissionType.EVOLVE_USING_STONE, 1); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("evolve_with_stone_leaf", "Evoluciona con Piedra Hoja", MissionType.EVOLVE_USING_STONE, 1); m.reward = 0.5; missions.add(m);

		// Misiones de rango de nivel
		m = new MissionDefinition("catch_low_level", "Captura Pokémon nivel bajo (1-10)", MissionType.CAPTURE_LEVEL_RANGE, 5, "1:10"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("catch_mid_level", "Captura Pokémon nivel medio (20-40)", MissionType.CAPTURE_LEVEL_RANGE, 3, "20:40"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("catch_high_level", "Captura Pokémon nivel alto (50+)", MissionType.CAPTURE_LEVEL_RANGE, 2, "50:100"); m.reward = 0.5; missions.add(m);

		// Misiones de bioma
		m = new MissionDefinition("catch_in_forest", "Captura en bosque", MissionType.CAPTURE_IN_BIOME, 3, "forest"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("catch_in_mountain", "Captura en montaña", MissionType.CAPTURE_IN_BIOME, 3, "mountain"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("catch_in_beach", "Captura en playa", MissionType.CAPTURE_IN_BIOME, 3, "beach"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("catch_in_cave", "Captura en cueva", MissionType.CAPTURE_IN_BIOME, 3, "cave"); m.reward = 0.4; missions.add(m);

		// Más misiones de condiciones especiales
		m = new MissionDefinition("catch_shiny_type_fire", "Captura shiny tipo Fuego", MissionType.CAPTURE_SHINY_SPECIFIC_TYPE, 1, "fire"); m.reward = 4.0; missions.add(m);
		m = new MissionDefinition("catch_shiny_type_water", "Captura shiny tipo Agua", MissionType.CAPTURE_SHINY_SPECIFIC_TYPE, 1, "water"); m.reward = 4.0; missions.add(m);
		m = new MissionDefinition("catch_shiny_type_ghost", "Captura shiny tipo Fantasma", MissionType.CAPTURE_SHINY_SPECIFIC_TYPE, 1, "ghost"); m.reward = 4.0; missions.add(m);

		// Misiones específicas de especies populares
		m = new MissionDefinition("catch_pikachu", "Captura Pikachu", MissionType.CAPTURE_SPECIFIC_SPECIES, 1, "pikachu"); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("catch_eevee", "Captura Eevee", MissionType.CAPTURE_SPECIFIC_SPECIES, 1, "eevee"); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("catch_ditto", "Captura Ditto", MissionType.CAPTURE_SPECIFIC_SPECIES, 1, "ditto"); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("catch_gible", "Captura Gible", MissionType.CAPTURE_SPECIFIC_SPECIES, 1, "gible"); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("catch_larvitar", "Captura Larvitar", MissionType.CAPTURE_SPECIFIC_SPECIES, 1, "larvitar"); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("catch_bagon", "Captura Bagon", MissionType.CAPTURE_SPECIFIC_SPECIES, 1, "bagon"); m.reward = 0.5; missions.add(m);
		m = new MissionDefinition("catch_beldum", "Captura Beldum", MissionType.CAPTURE_SPECIFIC_SPECIES, 1, "beldum"); m.reward = 0.5; missions.add(m);

		// Misiones de defeat específicas
		m = new MissionDefinition("defeat_rattata", "Derrota Rattata", MissionType.DEFEAT_SPECIFIC_SPECIES, 5, "rattata"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("defeat_pidgey", "Derrota Pidgey", MissionType.DEFEAT_SPECIFIC_SPECIES, 5, "pidgey"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("defeat_zubat", "Derrota Zubat", MissionType.DEFEAT_SPECIFIC_SPECIES, 5, "zubat"); m.reward = 0.3; missions.add(m);

		// Misiones vanilla adicionales
		m = new MissionDefinition("break_stone", "Rompe piedra", MissionType.BREAK_SPECIFIC_BLOCK, 32, "stone"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("break_ore", "Rompe mena", MissionType.BREAK_SPECIFIC_BLOCK, 16, "coal_ore"); m.reward = 0.4; missions.add(m);
		m = new MissionDefinition("kill_zombie", "Mata zombies", MissionType.KILL_SPECIFIC_MOB, 10, "zombie"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("kill_skeleton", "Mata esqueletos", MissionType.KILL_SPECIFIC_MOB, 10, "skeleton"); m.reward = 0.3; missions.add(m);
		m = new MissionDefinition("kill_creeper", "Mata creepers", MissionType.KILL_SPECIFIC_MOB, 5, "creeper"); m.reward = 0.4; missions.add(m);

		return missions;
	}

	public static class ModeconomiaConfigData {
		public AfkConfig afk = new AfkConfig();
		public MissionsConfig missions = new MissionsConfig();
		public ShopConfig shop = new ShopConfig();
		public RankConfig ranks = new RankConfig();

		// NPC Misionero
		public String questNpcUuid;
		public String questNpcWorld;
		public double questNpcX;
		public double questNpcY;
		public double questNpcZ;

		// NPC Tienda
		public String shopNpcUuid;
		public String shopNpcWorld;
		public double shopNpcX;
		public double shopNpcY;
		public double shopNpcZ;
	}

	public static class AfkConfig {
		public String world;
		public int pos1X;
		public int pos1Y;
		public int pos1Z;
		public int pos2X;
		public int pos2Y;
		public int pos2Z;
		public double baseReward = 0.3;
		public int intervalMinutes = 30;
		public Map<Rank, Double> multipliers = new EnumMap<>(Rank.class);
	}

	public static class MissionsConfig {
		public double baseReward = 0.4;
		public int dailyCount = 5;
		public Map<Rank, Double> multipliers = new EnumMap<>(Rank.class);
		public List<MissionDefinition> definitions = new ArrayList<>();
	}

	public static class ShopConfig {
		public List<ShopItem> items = new ArrayList<>();
		public List<ShopItem> listings = new ArrayList<>();
		/** Categorías personalizadas creadas por el owner */
		public List<CustomCategory> customCategories = new ArrayList<>();
	}

	/** Categoría custom de la tienda creada por el owner. */
	public static class CustomCategory {
		public String id;          // clave interna única, ej: "especiales"
		public String label;       // nombre visible, ej: "✦ Especiales"
		public String color;       // código de color MC, ej: "§e"
		public String description; // descripción visible en el tooltip
		public String iconNbt;     // NBT del item ícono (tomado de la mano al crear)
		public CustomCategory() {}
		public CustomCategory(String id, String label, String color) {
			this.id = id; this.label = label; this.color = color;
		}
		public CustomCategory(String id, String label, String color, String description, String iconNbt) {
			this.id = id; this.label = label; this.color = color;
			this.description = description; this.iconNbt = iconNbt;
		}
	}

	public static class RankConfig {
		public Map<Rank, List<String>> rankMembers = new EnumMap<>(Rank.class);
	}
}
