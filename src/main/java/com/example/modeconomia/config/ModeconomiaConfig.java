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

	private static List<MissionDefinition> defaultMissions() {
		List<MissionDefinition> missions = new ArrayList<>();
		missions.add(new MissionDefinition("break_blocks",      "Rompe bloques",       MissionType.BREAK_BLOCKS,        64));
		missions.add(new MissionDefinition("kill_mobs",         "Mata mobs",           MissionType.KILL_MOBS,           10));
		missions.add(new MissionDefinition("walk_distance",     "Camina distancia",    MissionType.WALK_DISTANCE,      500));
		missions.add(new MissionDefinition("playtime",          "Tiempo conectado",    MissionType.PLAYTIME_MINUTES,    30));
		missions.add(new MissionDefinition("join_server",       "Conéctate hoy",       MissionType.JOIN_SERVER,          1));
		missions.add(new MissionDefinition("capture_pokemon",   "Captura Pokémon",     MissionType.CAPTURE_COBBLEMON,    5));
		missions.add(new MissionDefinition("defeat_wild",       "Derrota Pokémon salvajes", MissionType.DEFEAT_WILD_COBBLEMON, 10));
		missions.add(new MissionDefinition("hatch_egg",         "Eclosiona huevos",    MissionType.HATCH_EGG,            1));
		missions.add(new MissionDefinition("evolve_pokemon",    "Evoluciona Pokémon",  MissionType.EVOLVE_COBBLEMON,     3));
		missions.add(new MissionDefinition("use_poke_ball",     "Usa Poké Ball",       MissionType.CAPTURE_WITH_BALL,    3, "poke_ball"));
		missions.add(new MissionDefinition("catch_fire_type",   "Captura tipo Fuego",  MissionType.CAPTURE_SPECIFIC_TYPE,3, "fire"));
		missions.add(new MissionDefinition("catch_at_night",    "Captura de noche",    MissionType.CATCH_AT_NIGHT,       3));
		missions.add(new MissionDefinition("catch_shiny",       "Captura un shiny",    MissionType.CAPTURE_SHINY_COBBLEMON, 1));
		missions.add(new MissionDefinition("catch_legendary",   "Captura legendario",  MissionType.CAPTURE_LEGENDARY_COBBLEMON, 1));
		missions.add(new MissionDefinition("trade_pokemon",     "Intercambia Pokémon", MissionType.TRADE_POKEMON,        1));
		missions.add(new MissionDefinition("win_pvp",          "Gana batalla PvP",    MissionType.WIN_TRAINER_BATTLE,   1));
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
