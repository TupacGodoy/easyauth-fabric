package com.cobblemania.economia.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PokemonTranslations {
    private PokemonTranslations() {}

    private static final Map<String, String> TYPES = new LinkedHashMap<>();
    private static final Map<String, String> BALLS = new LinkedHashMap<>();

    static {
        TYPES.put("NORMAL",   "Normal");     TYPES.put("FIRE",     "Fuego");
        TYPES.put("WATER",    "Agua");       TYPES.put("GRASS",    "Planta");
        TYPES.put("ELECTRIC", "Eléctrico");  TYPES.put("ICE",      "Hielo");
        TYPES.put("FIGHTING", "Lucha");      TYPES.put("POISON",   "Veneno");
        TYPES.put("GROUND",   "Tierra");     TYPES.put("FLYING",   "Volador");
        TYPES.put("PSYCHIC",  "Psíquico");   TYPES.put("BUG",      "Bicho");
        TYPES.put("ROCK",     "Roca");       TYPES.put("GHOST",    "Fantasma");
        TYPES.put("DRAGON",   "Dragón");     TYPES.put("DARK",     "Siniestro");
        TYPES.put("STEEL",    "Acero");      TYPES.put("FAIRY",    "Hada");

        BALLS.put("POKE_BALL",    "Poké Ball");      BALLS.put("GREAT_BALL",   "Super Ball");
        BALLS.put("ULTRA_BALL",   "Ultra Ball");     BALLS.put("MASTER_BALL",  "Master Ball");
        BALLS.put("SAFARI_BALL",  "Safari Ball");    BALLS.put("FAST_BALL",    "Veloz Ball");
        BALLS.put("LEVEL_BALL",   "Nivel Ball");     BALLS.put("LURE_BALL",    "Señuelo Ball");
        BALLS.put("HEAVY_BALL",   "Peso Ball");      BALLS.put("LOVE_BALL",    "Amor Ball");
        BALLS.put("FRIEND_BALL",  "Amigo Ball");     BALLS.put("MOON_BALL",    "Luna Ball");
        BALLS.put("SPORT_BALL",   "Deporte Ball");   BALLS.put("NET_BALL",     "Red Ball");
        BALLS.put("NEST_BALL",    "Nido Ball");      BALLS.put("REPEAT_BALL",  "Repetición Ball");
        BALLS.put("TIMER_BALL",   "Temporizador B"); BALLS.put("LUXURY_BALL",  "Lujo Ball");
        BALLS.put("PREMIER_BALL", "Premier Ball");   BALLS.put("DUSK_BALL",    "Oscuridad Ball");
        BALLS.put("HEAL_BALL",    "Cura Ball");      BALLS.put("QUICK_BALL",   "Rápida Ball");
        BALLS.put("CHERISH_BALL", "Tesoro Ball");    BALLS.put("DIVE_BALL",    "Buceo Ball");
        BALLS.put("DREAM_BALL",   "Sueño Ball");     BALLS.put("BEAST_BALL",   "Ultra Ente Ball");
        BALLS.put("PARK_BALL",    "Parque Ball");    BALLS.put("STRANGE_BALL", "Extraña Ball");
    }

    /**
     * Translates any filter value (type, ball, rank, etc.) to "KEY (Español)".
     * If no translation found, returns the raw value unchanged.
     */
    public static String translate(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        String key = raw.trim().toUpperCase();
        if (TYPES.containsKey(key)) return key + " §8(§f" + TYPES.get(key) + "§8)";
        if (BALLS.containsKey(key)) return key + " §8(§f" + BALLS.get(key) + "§8)";
        // Ranks
        return switch (key) {
            case "ENTRENADOR", "entrenador"  -> "entrenador §8(§fEntrenador§8)";
            case "LIDER", "lider"             -> "lider §8(§fLíder Gimnasio§8)";
            case "ALTO_MANDO", "alto mando",
                 "ALTO MANDO"                 -> "alto mando §8(§fAlto Mando§8)";
            case "CAMPEON", "campeon"          -> "campeon §8(§fCampeón§8)";
            default            -> raw;
        };
    }

    public static String allTypesHint() {
        StringBuilder sb = new StringBuilder("§8Tipos disponibles:\n");
        int i = 0;
        for (Map.Entry<String, String> e : TYPES.entrySet()) {
            if (i > 0) sb.append(i % 3 == 0 ? "\n" : " §8· ");
            sb.append("§f").append(e.getKey()).append(" §8(").append(e.getValue()).append("§8)");
            i++;
        }
        return sb.toString();
    }

    public static String allBallsHint() {
        StringBuilder sb = new StringBuilder("§8Pokébolas disponibles:\n");
        int i = 0;
        for (Map.Entry<String, String> e : BALLS.entrySet()) {
            if (i > 0) sb.append(i % 3 == 0 ? "\n" : " §8· ");
            sb.append("§f").append(e.getKey()).append(" §8(").append(e.getValue()).append("§8)");
            i++;
        }
        return sb.toString();
    }
}
