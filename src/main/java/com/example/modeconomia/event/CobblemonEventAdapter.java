package com.cobblemania.economia.event;

import com.cobblemania.economia.mission.MissionManager;
import com.cobblemania.economia.data.MissionType;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.FossilRevivedEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Integración con Cobblemon 1.7.3 — verified from bytecode of cobblemon-economy-0.0.16.
 *
 * NOTA: Requiere modCompileOnly en build.gradle para que Loom remapee
 * intermediary (class_3222) → yarn (ServerPlayerEntity) automáticamente.
 *
 * Eventos que NO usan imports directos de Cobblemon (para evitar errores de API):
 * - EvolutionCompleteEvent: usa reflection (getPlayer() no existe en 1.7.3)
 * - PokedexDataChangedEvent: usa getPlayerUUID() + server lookup
 * - EGG_HATCH, TRADE, RELEASE, NICKNAME: todos via reflection
 */
public final class CobblemonEventAdapter {

    private CobblemonEventAdapter() {}

    private static final Map<UUID, Integer> winStreak = new HashMap<>();

    public static void register() {
        registerCapture();
        registerBattleVictory();
        registerFossil();
        registerPokedex();
        registerEvolveViaReflection();
        registerSecondaryEvents();
    }

    // ════════════════════════════════════════════════════
    // CAPTURA — verified from bytecode
    // ════════════════════════════════════════════════════
    private static void registerCapture() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            try {
                ServerPlayerEntity player = event.getPlayer();
                Pokemon pokemon = event.getPokemon();
                if (player == null || pokemon == null) return kotlin.Unit.INSTANCE;

                Species species   = pokemon.getSpecies();
                boolean isShiny   = pokemon.getShiny();
                HashSet<?> labels = species.getLabels();
                boolean isLegendary = labels != null && (labels.contains("legendary") || labels.contains("mythical"));
                boolean isRadiant   = labels != null && labels.contains("radiant");
                boolean isParadox   = labels != null && labels.contains("paradox");

                String speciesName = species.getName().toLowerCase();
                String typesStr    = safeGetTypes(pokemon);
                String ballId      = safeGetBall(event);
                int    level       = pokemon.getLevel();
                String biomeId     = getBiomeKey(player);
                boolean isNight    = isNighttime(player);
                boolean isRaining  = isRaining(player);
                boolean noDmg      = !MissionManager.wasDamagedInBattle(player);

                MissionManager.addProgress(player, MissionType.CAPTURE_COBBLEMON, 1);
                if (isShiny)     MissionManager.addProgress(player, MissionType.CAPTURE_SHINY_COBBLEMON, 1);
                if (isLegendary) MissionManager.addProgress(player, MissionType.CAPTURE_LEGENDARY_COBBLEMON, 1);
                if (isRadiant)   MissionManager.addProgress(player, MissionType.CAPTURE_RADIANT_COBBLEMON, 1);
                if (isParadox)   MissionManager.addProgress(player, MissionType.CAPTURE_PARADOX_COBBLEMON, 1);

                // Nuevos tipos Cobbleverse
                if (isParadox)   MissionManager.addProgressFiltered(player, MissionType.CATCH_RADIANT_SPECIFIC_TYPE, 0, typesStr); // solo si radiant
                if (isRadiant)   MissionManager.addProgressFiltered(player, MissionType.CATCH_RADIANT_SPECIFIC_TYPE, 1, typesStr);

                // Captura con solo Poké Ball normal
                if ("poke_ball".equals(ballId) || "pokeball".equals(ballId))
                    MissionManager.addProgress(player, MissionType.CATCH_USING_ONLY_POKEBALL, 1);

                // Captura nivel 1
                if (level == 1)
                    MissionManager.addProgress(player, MissionType.CAPTURE_COBBLEMON_LEVEL_1, 1);

                MissionManager.addProgressFiltered(player, MissionType.CAPTURE_SPECIFIC_SPECIES, 1, speciesName);
                MissionManager.addProgressFiltered(player, MissionType.CAPTURE_SPECIFIC_TYPE,    1, typesStr);
                MissionManager.addProgressFiltered(player, MissionType.CAPTURE_WITH_BALL,        1, ballId);
                MissionManager.addProgressDoubleFilter(player, MissionType.CAPTURE_TYPE_WITH_BALL, 1, typesStr, ballId);
                MissionManager.addProgressRangeFilter(player, MissionType.CAPTURE_LEVEL_RANGE,   1, level);
                MissionManager.addProgressFiltered(player, MissionType.CAPTURE_IN_BIOME,         1, biomeId);

                if (isShiny) {
                    MissionManager.addProgressFiltered(player, MissionType.CAPTURE_SHINY_SPECIFIC_TYPE, 1, typesStr);
                    MissionManager.addProgressFiltered(player, MissionType.CAPTURE_SHINY_WITH_BALL,     1, ballId);
                }

                if (isNight)   MissionManager.addProgress(player, MissionType.CATCH_AT_NIGHT,      1);
                if (isRaining) MissionManager.addProgress(player, MissionType.CATCH_DURING_RAIN,   1);
                if (noDmg)     MissionManager.addProgress(player, MissionType.CATCH_WITHOUT_DAMAGE, 1);

            } catch (Exception ignored) {}
            return kotlin.Unit.INSTANCE;
        });
    }

    // ════════════════════════════════════════════════════
    // BATALLA — verified from bytecode
    // ════════════════════════════════════════════════════
    private static void registerBattleVictory() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            try {
                boolean isPvP = event.getBattle().isPvP();
                boolean isPvW = event.getBattle().isPvW();

                // Collect winner and loser players for ranked check
                ServerPlayerEntity winnerPlayer = null;
                ServerPlayerEntity loserPlayer  = null;

                for (BattleActor actor : event.getWinners()) {
                    if (!(actor instanceof PlayerBattleActor pActor)) continue;
                    ServerPlayerEntity player = pActor.getEntity();
                    if (player == null) continue;
                    winnerPlayer = player;

                    String typeUsed = safeGetPartyType(player);
                    MissionManager.addProgress(player, MissionType.DEFEAT_COBBLEMON, 1);
                    if (isPvW) MissionManager.addProgress(player, MissionType.DEFEAT_WILD_COBBLEMON, 1);
                    if (isPvP) {
                        MissionManager.addProgress(player, MissionType.WIN_TRAINER_BATTLE, 1);
                        int streak = winStreak.merge(player.getUuid(), 1, Integer::sum);
                        handleWinStreak(player, streak);
                    }
                    MissionManager.addProgressFiltered(player, MissionType.WIN_BATTLE_WITH_TYPE, 1, typeUsed);
                }

                // DEFEAT_SPECIFIC from losers' pokemon
                for (BattleActor loser : event.getLosers()) {
                    try {
                        for (Pokemon poke : getActorPokemons(loser)) {
                            if (poke == null) continue;
                            String loserSpecies = poke.getSpecies().getName().toLowerCase();
                            String loserTypes   = safeGetTypes(poke);
                            for (BattleActor winner : event.getWinners()) {
                                if (!(winner instanceof PlayerBattleActor wActor)) continue;
                                ServerPlayerEntity wp = wActor.getEntity();
                                if (wp == null) continue;
                                MissionManager.addProgressFiltered(wp, MissionType.DEFEAT_SPECIFIC_SPECIES, 1, loserSpecies);
                                MissionManager.addProgressFiltered(wp, MissionType.DEFEAT_SPECIFIC_TYPE,    1, loserTypes);
                            }
                        }
                    } catch (Exception ignored) {}

                    if (loser instanceof PlayerBattleActor lActor) {
                        ServerPlayerEntity lp = lActor.getEntity();
                        if (lp != null) {
                            loserPlayer = lp;
                            if (isPvP) winStreak.put(lp.getUuid(), 0);
                        }
                    }
                }

                // ── Misiones Ranked (requiere CobbleMaiaRanked) ──
                if (isPvP && winnerPlayer != null && loserPlayer != null) {
                    try {
                        if (RankedMissionAdapter.areBothInRankedMatch(winnerPlayer, loserPlayer)) {
                            String winnerType = safeGetPartyType(winnerPlayer);
                            RankedMissionAdapter.onRankedMatchEnd(winnerPlayer, loserPlayer, winnerType);
                        }
                    } catch (Exception ignored) {}
                }

            } catch (Exception ignored) {}
            return kotlin.Unit.INSTANCE;
        });
    }

    // ════════════════════════════════════════════════════
    // FÓSIL — verified from bytecode
    // ════════════════════════════════════════════════════
    private static void registerFossil() {
        try {
            CobblemonEvents.FOSSIL_REVIVED.subscribe(Priority.NORMAL, event -> {
                try {
                    ServerPlayerEntity player = event.getPlayer();
                    if (player != null && event.getPokemon() != null)
                        MissionManager.addProgress(player, MissionType.REVIVE_FOSSIL, 1);
                } catch (Exception ignored) {}
                return kotlin.Unit.INSTANCE;
            });
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════════════
    // POKÉDEX — verified: uses getPlayerUUID() NOT getPlayer()
    // ════════════════════════════════════════════════════
    private static void registerPokedex() {
        try {
            CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Priority.NORMAL, event -> {
                try {
                    // PokedexDataChangedEvent$Post has getPlayerUUID(), not getPlayer()
                    UUID playerUuid = event.getPlayerUUID();
                    if (playerUuid == null) return kotlin.Unit.INSTANCE;

                    // Only count when new knowledge = CAUGHT
                    Object knowledge = event.getKnowledge();
                    try {
                        Class<?> prog = Class.forName("com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress");
                        Object caught = prog.getField("CAUGHT").get(null);
                        if (!caught.equals(knowledge)) return kotlin.Unit.INSTANCE;
                    } catch (Exception ignored) {} // if check fails, count anyway

                    // Look up online player via stored server reference
                    ServerPlayerEntity player = getPlayerByUuid(playerUuid);
                    if (player != null)
                        MissionManager.addProgressAuto(player, MissionType.COLLECT_POKEDEX_ENTRIES, 1);
                } catch (Exception ignored) {}
                return kotlin.Unit.INSTANCE;
            });
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════════════
    // EVOLUCIÓN — via reflection (getPlayer() not in 1.7.3 API)
    // ════════════════════════════════════════════════════
    private static void registerEvolveViaReflection() {
        // Cobblemon 1.7.x: EVOLUTION_COMPLETE event tiene "pokemon" (post-evolución)
        // y "result" o "preEvolution" para la especie pre-evolución.
        // BUG FIX: La misión EVOLVE_SPECIFIC_SPECIES usaba SOLO la especie post-evolución.
        // Ahora también se dispara con la especie PRE-evolución (la que el jugador tenía).
        for (String evName : new String[]{"EVOLUTION_COMPLETE","POKEMON_EVOLVED","EVOLUTION_COMPLETED"}) {
            tryReflective(evName, ev -> {
                try {
                    ServerPlayerEntity player = reflectPlayerField(ev, "player","owner");

                    // Pokémon POST-evolución
                    Object pokePost = reflectField(ev, "pokemon","result","evolved","evolution");
                    // Pokémon PRE-evolución (puede estar como "preEvolution", "original", "previousPokemon")
                    Object pokePre  = reflectField(ev, "preEvolution","original","previousPokemon","source");

                    if (player == null && pokePost instanceof Pokemon p) {
                        player = getOwnerPlayer(p);
                    }
                    if (player == null && pokePre instanceof Pokemon p) {
                        player = getOwnerPlayer(p);
                    }
                    if (player == null) return;

                    String speciesPost = (pokePost instanceof Pokemon p)
                        ? p.getSpecies().getName().toLowerCase() : "";
                    String speciesPre  = (pokePre instanceof Pokemon p)
                        ? p.getSpecies().getName().toLowerCase() : "";

                    MissionManager.addProgress(player, MissionType.EVOLVE_COBBLEMON, 1);

                    // Disparar con especie post-evolución (ej: "flareon")
                    if (!speciesPost.isEmpty())
                        MissionManager.addProgressFiltered(player, MissionType.EVOLVE_SPECIFIC_SPECIES, 1, speciesPost);
                    // Disparar TAMBIÉN con especie pre-evolución (ej: "eevee") — fix principal
                    if (!speciesPre.isEmpty() && !speciesPre.equals(speciesPost))
                        MissionManager.addProgressFiltered(player, MissionType.EVOLVE_SPECIFIC_SPECIES, 1, speciesPre);

                    // Evolución con piedra: verificar si se usó item de evolución
                    boolean usedStone = false;
                    try {
                        Object reason = reflectField(ev, "reason","trigger","evolutionTrigger","context");
                        if (reason != null) {
                            String reasonStr = reason.toString().toLowerCase();
                            usedStone = reasonStr.contains("stone") || reasonStr.contains("item");
                        }
                    } catch (Exception ignored2) {}
                    if (usedStone) {
                        MissionManager.addProgress(player, MissionType.EVOLVE_USING_STONE, 1);
                    }

                } catch (Exception ignored) {}
            });
        }
    }

    // ════════════════════════════════════════════════════
    // EVENTOS SECUNDARIOS
    // ════════════════════════════════════════════════════
    private static void registerSecondaryEvents() {
        // EGG_HATCH — Cobblemon 1.6+
        for (String evName : new String[]{"EGG_HATCH","POKEMON_EGG_HATCH","HATCH_EGG"}) {
            tryReflective(evName, ev -> {
                ServerPlayerEntity p = reflectPlayerField(ev, "player","owner","context");
                if (p == null) {
                    Object poke = reflectField(ev, "pokemon","hatched");
                    if (poke instanceof Pokemon pk) p = getOwnerPlayer(pk);
                }
                if (p != null) MissionManager.addProgress(p, MissionType.HATCH_EGG, 1);
            });
        }
        // TRADE_COMPLETED
        for (String evName : new String[]{"TRADE_COMPLETED","POKEMON_TRADE","TRADE"}) {
            tryReflective(evName, ev -> {
                ServerPlayerEntity p1 = reflectPlayerField(ev, "player1","trader1","player");
                ServerPlayerEntity p2 = reflectPlayerField(ev, "player2","trader2");
                if (p1 != null) MissionManager.addProgress(p1, MissionType.TRADE_POKEMON, 1);
                if (p2 != null) MissionManager.addProgress(p2, MissionType.TRADE_POKEMON, 1);
            });
        }
        // POKEMON_RELEASED — Cobblemon 1.6+: event has player field
        for (String evName : new String[]{"POKEMON_RELEASED","RELEASE_POKEMON","POKEMON_RELEASE"}) {
            tryReflective(evName, ev -> {
                ServerPlayerEntity p = reflectPlayerField(ev, "player","releaser","owner");
                if (p == null) {
                    Object poke = reflectField(ev, "pokemon","released");
                    if (poke instanceof Pokemon pk) p = getOwnerPlayer(pk);
                }
                if (p != null) MissionManager.addProgress(p, MissionType.RELEASE_POKEMON, 1);
            });
        }
        // POKEMON_NICKNAMED — nombre puede variar; en 1.7.x puede ser NICKNAME_POKEMON
        for (String evName : new String[]{"POKEMON_NICKNAMED","NICKNAME_POKEMON","POKEMON_NICKNAME"}) {
            tryReflective(evName, ev -> {
                ServerPlayerEntity p = reflectPlayerField(ev, "player","owner");
                if (p == null) {
                    Object poke = reflectField(ev, "pokemon");
                    if (poke instanceof Pokemon pk) p = getOwnerPlayer(pk);
                }
                if (p != null) MissionManager.addProgress(p, MissionType.NICKNAME_POKEMON, 1);
            });
        }
        tryReflective("BATTLE_STARTED", ev -> {
            try {
                ServerPlayerEntity p = reflectPlayerField(ev, "player");
                if (p != null) { MissionManager.onBattleStart(p); return; }
                Object battle = reflectField(ev, "battle","pokemonBattle");
                if (battle == null) return;
                Object actors = battle.getClass().getMethod("getActors").invoke(battle);
                if (!(actors instanceof Iterable<?> iter)) return;
                for (Object actor : iter) {
                    if (actor instanceof PlayerBattleActor pa) {
                        ServerPlayerEntity bp = pa.getEntity();
                        if (bp != null) MissionManager.onBattleStart(bp);
                    }
                }
            } catch (Exception ignored) {}
        });
        tryReflective("POKEMON_FAINTED", ev -> {
            try {
                Object poke = reflectField(ev, "pokemon","fainted");
                if (!(poke instanceof Pokemon p)) return;
                String species    = p.getSpecies().getName().toLowerCase();
                String types      = safeGetTypes(p);
                HashSet<?> labels = p.getSpecies().getLabels();
                boolean isParadox = labels != null && labels.contains("paradox");
                ServerPlayerEntity attacker = reflectPlayerField(ev, "killer","attacker","player");
                if (attacker != null) {
                    MissionManager.addProgressFiltered(attacker, MissionType.DEFEAT_SPECIFIC_SPECIES, 1, species);
                    MissionManager.addProgressFiltered(attacker, MissionType.DEFEAT_SPECIFIC_TYPE,    1, types);
                    if (isParadox) MissionManager.addProgress(attacker, MissionType.DEFEAT_PARADOX_COBBLEMON, 1);
                }
            } catch (Exception ignored) {}
        });

        // ── HATCH_SPECIFIC_SPECIES — eclosionar especie concreta ──
        for (String evName : new String[]{"EGG_HATCH","POKEMON_EGG_HATCH","HATCH_EGG"}) {
            tryReflective(evName, ev -> {
                try {
                    ServerPlayerEntity p = reflectPlayerField(ev, "player","owner","context");
                    Object poke = reflectField(ev, "pokemon","hatched");
                    if (p == null && poke instanceof Pokemon pk) p = getOwnerPlayer(pk);
                    if (p == null) return;
                    MissionManager.addProgress(p, MissionType.HATCH_EGG, 1);
                    if (poke instanceof Pokemon pk) {
                        String species = pk.getSpecies().getName().toLowerCase();
                        MissionManager.addProgressFiltered(p, MissionType.HATCH_SPECIFIC_SPECIES, 1, species);
                    }
                } catch (Exception ignored) {}
            });
        }

        // ── NICKNAME_SPECIFIC_POKEMON — apodo a especie concreta ──
        for (String evName : new String[]{"POKEMON_NICKNAMED","NICKNAME_POKEMON","POKEMON_NICKNAME"}) {
            tryReflective(evName, ev -> {
                try {
                    ServerPlayerEntity p = reflectPlayerField(ev, "player","owner");
                    Object poke = reflectField(ev, "pokemon");
                    if (p == null && poke instanceof Pokemon pk) p = getOwnerPlayer(pk);
                    if (p == null) return;
                    MissionManager.addProgress(p, MissionType.NICKNAME_POKEMON, 1);
                    if (poke instanceof Pokemon pk) {
                        String species = pk.getSpecies().getName().toLowerCase();
                        MissionManager.addProgressFiltered(p, MissionType.NICKNAME_SPECIFIC_POKEMON, 1, species);
                    }
                } catch (Exception ignored) {}
            });
        }

        // ── RELEASE_SHINY — liberar pokémon shiny ──
        for (String evName : new String[]{"POKEMON_RELEASED","RELEASE_POKEMON","POKEMON_RELEASE"}) {
            tryReflective(evName, ev -> {
                try {
                    ServerPlayerEntity p = reflectPlayerField(ev, "player","releaser","owner");
                    Object poke = reflectField(ev, "pokemon","released");
                    if (p == null && poke instanceof Pokemon pk) p = getOwnerPlayer(pk);
                    if (p == null) return;
                    MissionManager.addProgress(p, MissionType.RELEASE_POKEMON, 1);
                    if (poke instanceof Pokemon pk && pk.getShiny()) {
                        MissionManager.addProgress(p, MissionType.RELEASE_SHINY, 1);
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    // ════════════════════════════════════════════════════
    // WIN STREAK
    // ════════════════════════════════════════════════════
    private static void handleWinStreak(ServerPlayerEntity player, int streak) {
        var data = com.cobblemania.economia.data.EconomyStorage.getMissionData(player.getUuid());
        if (data.activeMissionId == null) return;
        var def = MissionManager.findDef(data.activeMissionId);
        if (def == null || def.type != MissionType.WIN_STREAK) return;
        if (data.completed.contains(def.id)) return;
        data.progress.put(def.id, streak);
        com.cobblemania.economia.data.EconomyStorage.save();
        if (streak >= def.requiredAmount) {
            data.completed.add(def.id);
            data.activeMissionId = null;
            com.cobblemania.economia.data.EconomyStorage.recordMissionComplete(player.getUuid());
            MissionManager.rewardMission(player, def);
            player.sendMessage(net.minecraft.text.Text.literal(
                "§6§l✦ §eMisión completada: §f§l" + def.displayName), false);
            player.sendMessage(net.minecraft.text.Text.literal(
                "§6+" + MissionManager.format(MissionManager.calculateReward(player, def)) + " CC"), false);
        }
    }

    // ════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════
    /**
     * Devuelve los tipos del Pokémon como string space-separated en minúsculas.
     * Ej: "fire", "fire flying", "water", "psychic"
     *
     * ESTRATEGIA DE FALLBACK MÚLTIPLE para máxima robustez:
     * 1. Iterar getTypes() y extraer nombre con extractTypeName()
     * 2. Si ningún tipo reconocido → intentar getPrimaryType() solo
     * 3. Último recurso: parsear toString() de la colección completa
     */
    private static String safeGetTypes(Pokemon pokemon) {
        try {
            // ── Intento 1: getTypes() estándar ──
            Object typesCollection = pokemon.getTypes();
            if (typesCollection instanceof Iterable<?> iter) {
                StringBuilder sb = new StringBuilder();
                for (Object type : iter) {
                    String name = extractTypeName(type);
                    if (!name.isEmpty()) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(name);
                    }
                }
                String result = sb.toString().trim();
                if (!result.isEmpty()) return result;
            }

            // ── Intento 2: getPrimaryType() si getTypes() falló ──
            for (String m : new String[]{"getPrimaryType","getType1","getFirstType"}) {
                try {
                    Object t = pokemon.getClass().getMethod(m).invoke(pokemon);
                    if (t == null) continue;
                    String name = extractTypeName(t);
                    if (!name.isEmpty()) return name;
                } catch (Exception ignored2) {}
            }

            // ── Intento 3: parseo bruto de toString() de la colección ──
            // Puede ser "[fire, flying]", "[FIRE]", etc.
            String raw = typesCollection != null
                ? typesCollection.toString().toLowerCase()
                : pokemon.getTypes().toString().toLowerCase();
            // Extraer palabras que sean tipos conocidos
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[a-z]+").matcher(raw);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String word = m.group();
                if (isValidTypeName(word)) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(word);
                }
            }
            return sb.toString().trim();

        } catch (Exception ignored) { return ""; }
    }

    /**
     * Extrae el nombre limpio del tipo (ej: "fire", "water", "grass").
     * Cobblemon guarda ElementalType como objeto de registro con nombre en minúsculas.
     *
     * ESTRATEGIA MULTI-CAPA (de más a menos fiable):
     * 1. getShowdownId() — método oficial en Cobblemon 1.x, retorna "fire", "water"
     * 2. getIdentifier() → getPath() — retorna el path del ResourceLocation/Identifier
     * 3. getName().lowercase — fallback general
     * 4. Reflection sobre campos "name"/"id"/"identifier" del objeto
     * 5. Parseo de toString() con limpieza agresiva
     */
    private static String extractTypeName(Object type) {
        if (type == null) return "";

        // ── Capa 1: métodos que devuelven directamente el nombre del tipo ──
        for (String m : new String[]{"getShowdownId", "getName", "name"}) {
            try {
                Object result = type.getClass().getMethod(m).invoke(type);
                if (result == null) continue;
                String s = cleanTypeName(result.toString());
                if (isValidTypeName(s)) return s;
            } catch (Exception ignored) {}
        }

        // ── Capa 2: getIdentifier() devuelve un Identifier/ResourceLocation — extraer .getPath() ──
        for (String m : new String[]{"getIdentifier", "getId", "getRegistryName", "getKey"}) {
            try {
                Object result = type.getClass().getMethod(m).invoke(type);
                if (result == null) continue;
                // Intentar getPath() sobre el Identifier
                try {
                    String path = (String) result.getClass().getMethod("getPath").invoke(result);
                    String s = cleanTypeName(path);
                    if (isValidTypeName(s)) return s;
                } catch (Exception ignored2) {}
                // Intentar toString() del Identifier — puede ser "cobblemon:fire"
                String s = cleanTypeName(result.toString());
                if (isValidTypeName(s)) return s;
            } catch (Exception ignored) {}
        }

        // ── Capa 3: reflection directa sobre campos del objeto ──
        for (String fieldName : new String[]{"name", "id", "identifier", "typeName", "showdownId"}) {
            try {
                java.lang.reflect.Field f = findField(type.getClass(), fieldName);
                if (f == null) continue;
                f.setAccessible(true);
                Object v = f.get(type);
                if (v == null) continue;
                // Si el campo es un Identifier, extraer path
                try {
                    String path = (String) v.getClass().getMethod("getPath").invoke(v);
                    String s = cleanTypeName(path);
                    if (isValidTypeName(s)) return s;
                } catch (Exception ignored2) {}
                String s = cleanTypeName(v.toString());
                if (isValidTypeName(s)) return s;
            } catch (Exception ignored) {}
        }

        // ── Capa 4: parseo agresivo de toString() ──
        // Formatos posibles: "FIRE", "[FIRE]", "ElementalType.FIRE", "cobblemon:fire",
        //                    "ElementalType(name=fire)", "type=fire", "fire"
        String raw = type.toString().toLowerCase().trim();
        // Quitar namespace "cobblemon:"
        if (raw.contains(":")) raw = raw.substring(raw.lastIndexOf(':') + 1);
        // Quitar prefijos de clase "elementaltype.", "elementaltype("
        raw = raw.replaceAll("elementaltype[.(\\[{]*", "");
        // Extraer valor de name= si existe: "name=fire)" → "fire"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:name|id|type)=([a-z]+)").matcher(raw);
        if (m.find()) return m.group(1);
        // Quitar todo excepto letras
        raw = raw.replaceAll("[^a-z]", "").trim();
        return isValidTypeName(raw) ? raw : "";
    }

    /** Normaliza un string de tipo: minúsculas, quita namespace, quita chars inválidos */
    private static String cleanTypeName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String s = raw.toLowerCase().trim();
        if (s.contains(":")) s = s.substring(s.lastIndexOf(':') + 1);
        s = s.replaceAll("[^a-z]", "").trim();
        return s;
    }

    /** Valida que el string sea realmente un nombre de tipo Pokémon conocido */
    private static final java.util.Set<String> KNOWN_TYPES = new java.util.HashSet<>(java.util.Arrays.asList(
        "normal","fire","water","grass","electric","ice","fighting","poison","ground",
        "flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"
    ));
    private static boolean isValidTypeName(String s) {
        return s != null && !s.isEmpty() && KNOWN_TYPES.contains(s);
    }

    /** Busca un campo por nombre en la clase y sus superclases */
    private static java.lang.reflect.Field findField(Class<?> cls, String name) {
        while (cls != null && cls != Object.class) {
            try { return cls.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
            cls = cls.getSuperclass();
        }
        return null;
    }

    /**
     * Extrae el ID de la pokébola del evento de captura.
     * En Cobblemon 1.x, PokemonCapturedEvent tiene getCaughtBall() → PokeBall
     * PokeBall tiene getIdentifier() → Identifier, cuyo getPath() retorna "fast_ball", etc.
     *
     * También intenta obtener la ball directamente del Pokémon capturado
     * (Pokemon.getCaughtBall() si existe) como fallback.
     */
    private static String safeGetBall(PokemonCapturedEvent event) {
        // ── Capa 1: métodos directos del evento ──
        for (String m : new String[]{"getCaughtBall","getPokeBall","getBall","getCapturedWith"}) {
            try {
                Object b = event.getClass().getMethod(m).invoke(event);
                if (b == null) continue;
                String ballId = extractBallId(b);
                if (!ballId.isEmpty()) return ballId;
            } catch (Exception ignored) {}
        }

        // ── Capa 2: campos del evento por reflection ──
        Class<?> cls = event.getClass();
        while (cls != null && cls != Object.class) {
            for (java.lang.reflect.Field field : cls.getDeclaredFields()) {
                String fieldName = field.getName().toLowerCase();
                if (!fieldName.contains("ball") && !fieldName.contains("poke")) continue;
                try {
                    field.setAccessible(true);
                    Object b = field.get(event);
                    if (b == null) continue;
                    String ballId = extractBallId(b);
                    if (!ballId.isEmpty()) return ballId;
                } catch (Exception ignored) {}
            }
            cls = cls.getSuperclass();
        }

        // ── Capa 3: obtener la ball del Pokémon capturado ──
        try {
            Pokemon poke = event.getPokemon();
            if (poke != null) {
                for (String m : new String[]{"getCaughtBall","getPokeBall","getCatchingBall"}) {
                    try {
                        Object b = poke.getClass().getMethod(m).invoke(poke);
                        if (b == null) continue;
                        String ballId = extractBallId(b);
                        if (!ballId.isEmpty()) return ballId;
                    } catch (Exception ignored2) {}
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    /** Extracts clean ball ID (e.g. "fast_ball") from a PokeBall object.
     *  Returns lowercase with underscores, no namespace (e.g. "fast_ball", "master_ball"). */
    private static String extractBallId(Object ball) {
        // Cobblemon PokeBall tiene un Identifier; intentar getIdentifier(), getName(), etc.
        for (String m : new String[]{"getIdentifier","getName","getPath","name","getId","getShowdownId"}) {
            try {
                Object result = ball.getClass().getMethod(m).invoke(ball);
                if (result == null) continue;
                String s = cleanBallString(result.toString());
                if (!s.isEmpty()) return s;
            } catch (Exception ignored) {}
        }
        // Intentar acceder al campo "name" o "identifier" directamente
        for (String f : new String[]{"identifier","name","id"}) {
            try {
                java.lang.reflect.Field field = ball.getClass().getDeclaredField(f);
                field.setAccessible(true);
                Object v = field.get(ball);
                if (v == null) continue;
                // El identifier puede ser un net.minecraft.util.Identifier — intentar getPath()
                try {
                    String path = (String) v.getClass().getMethod("getPath").invoke(v);
                    String s = cleanBallString(path);
                    if (!s.isEmpty()) return s;
                } catch (Exception ignored2) {}
                String s = cleanBallString(v.toString());
                if (!s.isEmpty()) return s;
            } catch (Exception ignored) {}
        }
        // Fallback: toString() del objeto entero
        return cleanBallString(ball.toString());
    }

    /**
     * Limpia un string de ball: minúsculas, quita namespace, normaliza guiones.
     * Ejemplos de entrada → salida:
     *   "cobblemon:fast_ball"     → "fast_ball"
     *   "PokeBall(fast_ball)"     → "fast_ball"
     *   "FAST_BALL"               → "fast_ball"
     *   "cobblemon:poke_ball"     → "poke_ball"   (NO filtrado — poke_ball es válida)
     *   "net.minecraft.item.Item" → ""
     */
    private static String cleanBallString(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String s = raw.toLowerCase().trim();
        // Quitar namespace (ej: "cobblemon:fast_ball" → "fast_ball")
        if (s.contains(":")) s = s.substring(s.lastIndexOf(':') + 1);
        // Si el formato es "PokeBall(fast_ball)" → extraer el contenido del paréntesis
        java.util.regex.Matcher pm = java.util.regex.Pattern.compile("\\(([a-z0-9_]+)\\)").matcher(s);
        if (pm.find()) s = pm.group(1);
        // Quitar paréntesis, corchetes, espacios y chars inválidos — conservar guión bajo
        s = s.replaceAll("[^a-z0-9_]", "").trim();
        // Filtrar strings que claramente no son balls (nombres de clase Java, etc.)
        if (s.isEmpty() || s.equals("null") || s.equals("class") ||
            s.contains("minecraft") || s.contains("cobblemon") ||
            s.length() > 40) return "";
        // Normalizar: si no tiene guión bajo y termina en "ball" → puede ser válido
        // No filtrar "pokeball" — puede ser la poke_ball sin guión en algún formato
        return s;
    }

    private static String getBiomeKey(ServerPlayerEntity player) {
        try {
            return player.getServerWorld().getBiome(player.getBlockPos())
                .getKey().map(k -> k.getValue().toString()).orElse("");
        } catch (Exception ignored) { return ""; }
    }

    private static boolean isNighttime(ServerPlayerEntity player) {
        try {
            // getLevelProperties().getTimeOfDay() = tiempo del ciclo día/noche (0-23999)
            // Noche en MC: 13000 (anochecer) a 23000 (amanecer)
            long t = player.getServerWorld().getLevelProperties().getTimeOfDay() % 24000;
            return t >= 13000 && t <= 23000;
        } catch (Exception e1) {
            try {
                // Fallback: getTimeOfDay() directo
                long t = player.getServerWorld().getTimeOfDay() % 24000;
                return t >= 13000 && t <= 23000;
            } catch (Exception ignored) { return false; }
        }
    }

    private static boolean isRaining(ServerPlayerEntity player) {
        try { return player.getServerWorld().isRaining(); }
        catch (Exception ignored) { return false; }
    }

    private static String safeGetPartyType(ServerPlayerEntity player) {
        try {
            Object storage = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
            Object lead = storage.getClass().getMethod("get", int.class).invoke(storage, 0);
            if (lead instanceof Pokemon p) return safeGetTypes(p);
        } catch (Exception ignored) {}
        return "";
    }

    private static ServerPlayerEntity getOwnerPlayer(Pokemon pokemon) {
        try {
            Object owner = pokemon.getClass().getMethod("getOwnerPlayer").invoke(pokemon);
            if (owner instanceof ServerPlayerEntity sp) return sp;
        } catch (Exception ignored) {}
        return null;
    }

    private static ServerPlayerEntity getPlayerByUuid(UUID uuid) {
        try {
            net.minecraft.server.MinecraftServer srv = ModeconomiaEvents.SERVER;
            if (srv == null) return null;
            return srv.getPlayerManager().getPlayer(uuid);
        } catch (Exception ignored) {}
        return null;
    }

    private static Iterable<Pokemon> getActorPokemons(Object actor) {
        // Cobblemon BattleActor.getPokemonList() returns Collection<BattlePokemon>
        // BattlePokemon is a wrapper — need to unwrap to get the actual Pokemon
        for (String m : new String[]{"getPokemonList","getActivePokemon","getPokemonList"}) {
            try {
                Object result = actor.getClass().getMethod(m).invoke(actor);
                if (!(result instanceof Iterable<?> it)) continue;
                java.util.List<Pokemon> pokemons = new java.util.ArrayList<>();
                for (Object slot : it) {
                    Pokemon p = unwrapBattlePokemon(slot);
                    if (p != null) pokemons.add(p);
                }
                if (!pokemons.isEmpty()) return pokemons;
            } catch (Exception ignored) {}
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Unwrap a BattlePokemon (or Pokemon directly) to get the actual Pokemon instance.
     * Cobblemon wraps Pokemon in BattlePokemon during battles.
     */
    private static Pokemon unwrapBattlePokemon(Object slot) {
        if (slot instanceof Pokemon p) return p;
        // BattlePokemon has: effectedPokemon field or getPokemon() method
        for (String m : new String[]{"getPokemon","getEffectedPokemon","effectedPokemon"}) {
            try {
                Object result;
                try {
                    result = slot.getClass().getMethod(m).invoke(slot);
                } catch (NoSuchMethodException e) {
                    java.lang.reflect.Field f = slot.getClass().getDeclaredField(m);
                    f.setAccessible(true);
                    result = f.get(slot);
                }
                if (result instanceof Pokemon p) return p;
            } catch (Exception ignored) {}
        }
        return null;
    }

    @FunctionalInterface interface EvConsumer { void accept(Object ev); }

    private static void tryReflective(String eventName, EvConsumer consumer) {
        try {
            Class<?> cls = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents");
            java.lang.reflect.Field f;
            try { f = cls.getField(eventName); } catch (NoSuchFieldException e) { return; }
            Object evObj = f.get(null);
            for (java.lang.reflect.Method m : evObj.getClass().getMethods()) {
                if (!m.getName().equals("subscribe") || m.getParameterCount() != 2) continue;
                Class<?> p0 = m.getParameterTypes()[0];
                if (!p0.getName().contains("Priority")) continue;
                Object normal = p0.getField("NORMAL").get(null);
                m.invoke(evObj, normal, (kotlin.jvm.functions.Function1<Object, kotlin.Unit>) ev -> {
                    try { consumer.accept(ev); } catch (Exception ignored) {}
                    return kotlin.Unit.INSTANCE;
                });
                return;
            }
        } catch (Exception ignored) {}
    }

    private static ServerPlayerEntity reflectPlayerField(Object ev, String... names) {
        for (String name : names) {
            try {
                java.lang.reflect.Field f = ev.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(ev);
                if (v instanceof ServerPlayerEntity sp) return sp;
            } catch (Exception ignored) {}
            try {
                String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                Object v = ev.getClass().getMethod(getter).invoke(ev);
                if (v instanceof ServerPlayerEntity sp) return sp;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Object reflectField(Object obj, String... names) {
        for (String name : names) {
            try {
                java.lang.reflect.Field f = obj.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v != null) return v;
            } catch (Exception ignored) {}
            try {
                Object v = obj.getClass().getMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1)).invoke(obj);
                if (v != null) return v;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
