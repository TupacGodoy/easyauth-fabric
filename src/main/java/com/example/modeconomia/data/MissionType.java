package com.cobblemania.economia.data;

/**
 * Tipos de misión disponibles en CobbleMania.
 *
 * Categorías:
 *   PASSIVE  — se completan automáticamente sin necesidad de seleccionar
 *   MANUAL   — el jugador debe seleccionarlas en el NPC para que cuente el progreso
 *
 * Para tipos con [desc], el campo description de la MissionDefinition
 * contiene el filtro (ej: "FIRE", "pikachu", "20:40", "FAST_BALL", "FIRE:FAST_BALL").
 */
public enum MissionType {

    // ════════════════════════════════════
    // VANILLA — PASSIVE (auto-tracking)
    // ════════════════════════════════════
    BREAK_BLOCKS,        // Romper X bloques de cualquier tipo
    BREAK_SPECIFIC_BLOCK,// Romper X bloques de tipo concreto [desc=block_id, ej: "stone"]
    WALK_DISTANCE,       // Caminar X bloques
    PLAYTIME_MINUTES,    // Estar conectado X minutos
    JOIN_SERVER,         // Conectarse al servidor (cuenta 1 por día)
    PLACE_BLOCKS,        // Colocar X bloques
    CRAFT_ITEMS,         // Craftear X items
    FISH_ITEMS,          // Pescar X veces

    // ════════════════════════════════════
    // COBBLEMON — CAPTURA BÁSICA (MANUAL)
    // ════════════════════════════════════
    CAPTURE_COBBLEMON,             // Capturar X Pokémon (cualquiera)
    CAPTURE_SHINY_COBBLEMON,       // Capturar X Pokémon shiny
    CAPTURE_LEGENDARY_COBBLEMON,   // Capturar X Pokémon legendario o mítico
    CAPTURE_RADIANT_COBBLEMON,     // Capturar X Pokémon radiante (label "radiant")
    CAPTURE_PARADOX_COBBLEMON,     // Capturar X Pokémon paradoja

    // ════════════════════════════════════
    // COBBLEMON — CAPTURA CON FILTRO (MANUAL) [desc requerida]
    // ════════════════════════════════════
    CAPTURE_SPECIFIC_SPECIES,      // Capturar especie [desc="pikachu"]
    CAPTURE_SPECIFIC_TYPE,         // Capturar tipo [desc="FIRE"]
    CAPTURE_WITH_BALL,             // Capturar con ball [desc="fast_ball"]
    CAPTURE_TYPE_WITH_BALL,        // Capturar tipo con ball [desc="FIRE:FAST_BALL"]
    CAPTURE_SHINY_SPECIFIC_TYPE,   // Capturar shiny de tipo [desc="GHOST"]
    CAPTURE_SHINY_WITH_BALL,       // Capturar shiny con ball [desc="master_ball"]
    CAPTURE_LEVEL_RANGE,           // Capturar en rango de nivel [desc="20:40"]
    CAPTURE_IN_BIOME,              // Capturar en bioma [desc="desert"]

    // ════════════════════════════════════
    // COBBLEMON — CONDICIONES ESPECIALES (MANUAL)
    // ════════════════════════════════════
    CATCH_AT_NIGHT,                // Capturar X Pokémon de noche (hora juego 13000-23000)
    CATCH_DURING_RAIN,             // Capturar X Pokémon mientras llueve
    CATCH_WITHOUT_DAMAGE,          // Capturar X Pokémon sin recibir daño en la batalla

    // ════════════════════════════════════
    // COBBLEMON — COMBATE (MANUAL)
    // ════════════════════════════════════
    DEFEAT_COBBLEMON,              // Derrotar X Pokémon en batalla (cualquiera)
    DEFEAT_WILD_COBBLEMON,         // Derrotar X Pokémon salvajes
    DEFEAT_SPECIFIC_SPECIES,       // Derrotar especie [desc="rattata"]
    DEFEAT_SPECIFIC_TYPE,          // Derrotar tipo [desc="WATER"]
    WIN_TRAINER_BATTLE,            // Ganar X batallas contra jugadores (PvP)
    WIN_BATTLE_WITH_TYPE,          // Ganar X batallas usando Pokémon de tipo [desc="FIRE"]
    WIN_STREAK,                    // Ganar X batallas consecutivas (se resetea al perder)

    // ════════════════════════════════════
    // COBBLEMON — CRIANZA Y EVOLUCIÓN (MANUAL)
    // ════════════════════════════════════
    EVOLVE_COBBLEMON,              // Evolucionar X Pokémon (cualquiera)
    EVOLVE_SPECIFIC_SPECIES,       // Evolucionar especie [desc="eevee"]
    HATCH_EGG,                     // Hacer eclosionar X huevos

    // ════════════════════════════════════
    // COBBLEMON — INTERACCIÓN (MANUAL)
    // ════════════════════════════════════
    REVIVE_FOSSIL,                 // Revivir X fósiles
    TRADE_POKEMON,                 // Intercambiar X Pokémon con otros jugadores
    RELEASE_POKEMON,               // Liberar X Pokémon
    NICKNAME_POKEMON,              // Poner apodo a X Pokémon

    // ════════════════════════════════════
    // COBBLEMON — COLECCIÓN (PASSIVE)
    // ════════════════════════════════════
    COLLECT_POKEDEX_ENTRIES,       // Registrar X Pokémon nuevos en la Pokédex

    // ════════════════════════════════════
    // COBBLEMON — NUEVOS TIPOS COBBLEVERSE
    // ════════════════════════════════════
    DEFEAT_PARADOX_COBBLEMON,      // Derrotar X Pokémon paradoja en batalla
    CAPTURE_FULL_ODDS_SHINY,       // Capturar X shiny sin usar items de suerte
    WIN_BATTLE_NO_LEGENDARY,       // Ganar X batallas sin usar legendarios en el equipo
    HATCH_SPECIFIC_SPECIES,        // Eclosionar huevo de especie [desc="magikarp"]
    CATCH_USING_ONLY_POKEBALL,     // Capturar X Pokémon usando solo Poké Ball normal
    DEFEAT_COBBLEMON_TYPE_COMBO,   // Derrotar X Pokémon que sean doble tipo [desc="fire flying"]
    CAPTURE_COBBLEMON_LEVEL_1,     // Capturar X Pokémon exactamente en nivel 1
    CAPTURE_IN_WEATHER,            // Capturar X Pokémon durante tormenta
    WIN_BATTLE_UNDERLEVELED,       // Ganar batalla con todos tus Pokémon de nivel inferior al rival
    EVOLVE_USING_STONE,            // Evolucionar X Pokémon usando piedra evolutiva
    NICKNAME_SPECIFIC_POKEMON,     // Poner apodo a especie concreta [desc="pikachu"]
    RELEASE_SHINY,                 // Liberar X Pokémon shiny
    CATCH_RADIANT_SPECIFIC_TYPE,   // Capturar radiante de tipo [desc="dragon"]

    // ════════════════════════════════════
    // RANKED — requiere CobbleMaiaRanked
    // ════════════════════════════════════
    WIN_RANKED_BATTLE,             // Ganar X batallas ranked (cualquiera)
    WIN_RANKED_CONSECUTIVE,        // Ganar X batallas ranked consecutivas (racha) — se resetea al perder
    WIN_RANKED_WITH_TYPE,          // Ganar X ranked usando equipo del tipo indicado [desc="FIRE"]
    WIN_RANKED_NO_ITEMS,           // Ganar X ranked sin usar objetos en la batalla
    WIN_RANKED_SPECIFIC_RANK,      // Ganar ranked contra jugador del rango indicado [desc="campeon"|"alto mando"|"lider"]
    REACH_ELO,                     // Alcanzar X puntos de ELO ranked (PASSIVE — se completa al lograr el ELO)
    PLAY_RANKED_BATTLES,           // Jugar X batallas ranked (ganes o pierdas)
    WIN_RANKED_FLAWLESS,           // Ganar X ranked sin que derroten un solo Pokémon tuyo
}

