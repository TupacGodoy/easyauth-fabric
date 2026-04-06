package com.cobblemania.economia.shop;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.ShopItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Items predeterminados de la tienda del servidor.
 * Se cargan automáticamente si la tienda está vacía al inicializar el servidor.
 * El admin puede modificar precios y eliminar items desde el panel de Admin.
 *
 * IDs de Cobblemon 1.7.3 verificados.
 */
public final class DefaultShopItems {

    private DefaultShopItems() {}

    /**
     * Carga los items por defecto si la tienda del servidor está vacía.
     */
    public static void loadIfEmpty() {
        if (ModeconomiaConfig.DATA.shop.items != null
                && !ModeconomiaConfig.DATA.shop.items.isEmpty()) {
            return; // ya tiene items
        }
        ModeconomiaConfig.DATA.shop.items = buildDefaults();
        ModeconomiaConfig.save();
    }

    private static List<ShopItem> buildDefaults() {
        List<ShopItem> list = new ArrayList<>();
        int slot = 0;

        // ── Pokébolas ──
        slot = addItems(list, slot, "pokeballs", new String[][]{
            // Pokébolas básicas
            {"cobblemon:poke_ball",       "0.50"},
            {"cobblemon:great_ball",      "1.25"},
            {"cobblemon:ultra_ball",      "2.75"},
            {"cobblemon:master_ball",   "500.00"},
            // Balls de Apricorn
            {"cobblemon:fast_ball",       "2.50"},
            {"cobblemon:level_ball",      "2.50"},
            {"cobblemon:lure_ball",       "2.50"},
            {"cobblemon:heavy_ball",      "2.50"},
            {"cobblemon:love_ball",       "2.50"},
            {"cobblemon:friend_ball",     "2.50"},
            {"cobblemon:moon_ball",       "2.50"},
            {"cobblemon:sport_ball",      "2.50"},
            // Balls especiales
            {"cobblemon:net_ball",        "1.75"},
            {"cobblemon:dive_ball",       "1.75"},
            {"cobblemon:nest_ball",       "1.75"},
            {"cobblemon:repeat_ball",     "1.75"},
            {"cobblemon:timer_ball",      "1.75"},
            {"cobblemon:luxury_ball",     "4.50"},
            {"cobblemon:premier_ball",    "0.75"},
            {"cobblemon:dusk_ball",       "1.75"},
            {"cobblemon:heal_ball",       "1.75"},
            {"cobblemon:quick_ball",      "1.75"},
            {"cobblemon:beast_ball",     "15.00"},
            {"cobblemon:dream_ball",      "8.00"},
            {"cobblemon:cherish_ball",   "75.00"},
            {"cobblemon:safari_ball",     "4.00"},
            {"cobblemon:slate_ball",      "1.75"},
            {"cobblemon:azure_ball",      "1.75"},
            {"cobblemon:verdant_ball",    "1.75"},
            {"cobblemon:citrine_ball",    "1.75"},
            {"cobblemon:rose_ball",       "1.75"},
            {"cobblemon:strange_ball",   "12.00"},
            {"cobblemon:park_ball",      "10.00"},
            {"cobblemon:origin_ball",    "25.00"},
        });

        // ── Vitaminas & Pociones ──
        slot = addItems(list, slot, "vitaminas", new String[][]{
            // Vitaminas de stat
            {"cobblemon:hp_up",           "4.50"},
            {"cobblemon:protein",         "4.50"},
            {"cobblemon:iron",            "4.50"},
            {"cobblemon:calcium",         "4.50"},
            {"cobblemon:zinc",            "4.50"},
            {"cobblemon:carbos",          "4.50"},
            // Plumas de stat
            {"cobblemon:health_feather",  "2.00"},
            {"cobblemon:muscle_feather",  "2.00"},
            {"cobblemon:resist_feather",  "2.00"},
            {"cobblemon:genius_feather",  "2.00"},
            {"cobblemon:clever_feather",  "2.00"},
            {"cobblemon:swift_feather",   "2.00"},
            // Items de EV
            {"cobblemon:power_weight",    "3.00"},
            {"cobblemon:power_bracer",    "3.00"},
            {"cobblemon:power_belt",      "3.00"},
            {"cobblemon:power_lens",      "3.00"},
            {"cobblemon:power_band",      "3.00"},
            {"cobblemon:power_anklet",    "3.00"},
            {"cobblemon:macho_brace",     "2.50"},
            // PP
            {"cobblemon:pp_up",           "4.00"},
            {"cobblemon:pp_max",          "8.00"},
            {"cobblemon:ether",           "2.00"},
            {"cobblemon:max_ether",       "4.00"},
            {"cobblemon:elixir",          "3.00"},
            {"cobblemon:max_elixir",      "6.00"},
            // Pociones
            {"cobblemon:potion",          "0.50"},
            {"cobblemon:super_potion",    "1.25"},
            {"cobblemon:hyper_potion",    "2.50"},
            {"cobblemon:max_potion",      "5.00"},
            {"cobblemon:full_restore",    "7.50"},
            // Revivir
            {"cobblemon:revive",          "1.50"},
            {"cobblemon:max_revive",      "4.00"},
            // Curas de estado
            {"cobblemon:antidote",        "0.40"},
            {"cobblemon:awakening",       "0.40"},
            {"cobblemon:burn_heal",       "0.40"},
            {"cobblemon:ice_heal",        "0.40"},
            {"cobblemon:paralyze_heal",   "0.40"},
            {"cobblemon:full_heal",       "1.75"},
        });

        // ── Mentas ──
        slot = addItems(list, slot, "mentas", new String[][]{
            // Menta de naturaleza
            {"cobblemon:lonely_mint",     "3.50"},
            {"cobblemon:adamant_mint",    "3.50"},
            {"cobblemon:naughty_mint",    "3.50"},
            {"cobblemon:brave_mint",      "3.50"},
            {"cobblemon:bold_mint",       "3.50"},
            {"cobblemon:impish_mint",     "3.50"},
            {"cobblemon:lax_mint",        "3.50"},
            {"cobblemon:relaxed_mint",    "3.50"},
            {"cobblemon:modest_mint",     "3.50"},
            {"cobblemon:mild_mint",       "3.50"},
            {"cobblemon:rash_mint",       "3.50"},
            {"cobblemon:quiet_mint",      "3.50"},
            {"cobblemon:calm_mint",       "3.50"},
            {"cobblemon:gentle_mint",     "3.50"},
            {"cobblemon:careful_mint",    "3.50"},
            {"cobblemon:sassy_mint",      "3.50"},
            {"cobblemon:timid_mint",      "3.50"},
            {"cobblemon:hasty_mint",      "3.50"},
            {"cobblemon:jolly_mint",      "3.50"},
            {"cobblemon:naive_mint",      "3.50"},
            {"cobblemon:bashful_mint",    "3.50"},
            {"cobblemon:docile_mint",     "3.50"},
            {"cobblemon:hardy_mint",      "3.50"},
            {"cobblemon:quirky_mint",     "3.50"},
            {"cobblemon:serious_mint",    "3.50"},
        });

        // ── Caramelos Exp ──
        slot = addItems(list, slot, "caramelos", new String[][]{
            {"cobblemon:exp_candy_xs",  "0.75"},
            {"cobblemon:exp_candy_s",   "1.50"},
            {"cobblemon:exp_candy_m",   "3.50"},
            {"cobblemon:exp_candy_l",   "7.00"},
            {"cobblemon:exp_candy_xl", "12.00"},
            {"cobblemon:rare_candy",   "15.00"},
        });

        // ── Bonguris ──
        slot = addItems(list, slot, "bonguris", new String[][]{
            {"cobblemon:green_apricorn",   "0.30"},
            {"cobblemon:red_apricorn",     "0.30"},
            {"cobblemon:blue_apricorn",    "0.30"},
            {"cobblemon:yellow_apricorn",  "0.30"},
            {"cobblemon:white_apricorn",   "0.30"},
            {"cobblemon:black_apricorn",   "0.30"},
            {"cobblemon:pink_apricorn",    "0.30"},
        });

        // ── Bayas ──
        slot = addItems(list, slot, "bayas", new String[][]{
            // Bayas básicas de curación
            {"cobblemon:oran_berry",       "0.50"},
            {"cobblemon:sitrus_berry",     "0.75"},
            {"cobblemon:lum_berry",        "2.00"},
            {"cobblemon:leppa_berry",      "0.75"},
            // Bayas de estado
            {"cobblemon:cheri_berry",      "0.40"},
            {"cobblemon:chesto_berry",     "0.40"},
            {"cobblemon:pecha_berry",      "0.40"},
            {"cobblemon:rawst_berry",      "0.40"},
            {"cobblemon:aspear_berry",     "0.40"},
            {"cobblemon:persim_berry",     "0.40"},
            // Bayas de stat
            {"cobblemon:figy_berry",       "0.60"},
            {"cobblemon:wiki_berry",       "0.60"},
            {"cobblemon:mago_berry",       "0.60"},
            {"cobblemon:aguav_berry",      "0.60"},
            {"cobblemon:iapapa_berry",     "0.60"},
            // Bayas de captura
            {"cobblemon:razz_berry",       "0.50"},
            {"cobblemon:bluk_berry",       "0.50"},
            {"cobblemon:nanab_berry",      "0.50"},
            {"cobblemon:wepear_berry",     "0.50"},
            {"cobblemon:pinap_berry",      "0.50"},
            // Bayas competitivas
            {"cobblemon:liechi_berry",     "1.50"},
            {"cobblemon:ganlon_berry",     "1.50"},
            {"cobblemon:salac_berry",      "1.50"},
            {"cobblemon:petaya_berry",     "1.50"},
            {"cobblemon:apicot_berry",     "1.50"},
            {"cobblemon:lansat_berry",     "3.00"},
            {"cobblemon:starf_berry",      "3.00"},
            {"cobblemon:jaboca_berry",     "1.50"},
            {"cobblemon:rowap_berry",      "1.50"},
            {"cobblemon:enigma_berry",     "1.50"},
            {"cobblemon:micle_berry",      "1.50"},
            {"cobblemon:custap_berry",     "1.50"},
        });

        // ── Brotes e inciensos ──
        slot = addItems(list, slot, "brotes", new String[][]{
            {"cobblemon:big_root",        "2.50"},
            {"cobblemon:mental_herb",     "4.00"},
            {"cobblemon:power_herb",      "4.00"},
            {"cobblemon:white_herb",      "4.00"},
            {"cobblemon:miracle_seed",    "4.00"},
            {"cobblemon:black_sludge",    "4.00"},
            {"cobblemon:sticky_barb",     "4.00"},
            {"cobblemon:iron_ball",       "4.00"},
            {"cobblemon:flame_orb",       "4.00"},
            {"cobblemon:toxic_orb",       "4.00"},
            // Inciensos
            {"cobblemon:odd_incense",     "3.50"},
            {"cobblemon:full_incense",    "3.50"},
            {"cobblemon:luck_incense",    "3.50"},
            {"cobblemon:lax_incense",     "3.50"},
            {"cobblemon:pure_incense",    "3.50"},
            {"cobblemon:rock_incense",    "3.50"},
            {"cobblemon:rose_incense",    "3.50"},
            {"cobblemon:sea_incense",     "3.50"},
            {"cobblemon:wave_incense",    "3.50"},
        });

        // ── Objetos de evolución ──
        slot = addItems(list, slot, "evoluciones", new String[][]{
            // Piedras elementales
            {"cobblemon:fire_stone",      "6.00"},
            {"cobblemon:water_stone",     "6.00"},
            {"cobblemon:thunder_stone",   "6.00"},
            {"cobblemon:leaf_stone",      "6.00"},
            {"cobblemon:moon_stone",      "6.00"},
            {"cobblemon:sun_stone",       "6.00"},
            {"cobblemon:dawn_stone",      "8.00"},
            {"cobblemon:dusk_stone",      "8.00"},
            {"cobblemon:shiny_stone",     "8.00"},
            {"cobblemon:ice_stone",       "6.00"},
            {"cobblemon:oval_stone",      "4.00"},
            // Cuerda enlace y objetos de intercambio
            {"cobblemon:linking_cord",    "5.00"},
            {"cobblemon:kings_rock",      "8.00"},
            {"cobblemon:metal_coat",      "8.00"},
            {"cobblemon:dragon_scale",   "10.00"},
            {"cobblemon:upgrade",        "10.00"},
            {"cobblemon:dubious_disc",   "10.00"},
            {"cobblemon:protector",      "10.00"},
            {"cobblemon:electirizer",    "10.00"},
            {"cobblemon:magmarizer",     "10.00"},
            {"cobblemon:reaper_cloth",   "10.00"},
            {"cobblemon:razor_claw",     "10.00"},
            {"cobblemon:razor_fang",     "10.00"},
            {"cobblemon:prism_scale",     "8.00"},
            {"cobblemon:deep_sea_scale",  "8.00"},
            {"cobblemon:deep_sea_tooth",  "8.00"},
            {"cobblemon:sachet",          "8.00"},
            {"cobblemon:whipped_dream",   "8.00"},
            // Objetos de evolución especiales
            {"cobblemon:cracked_pot",     "5.00"},
            {"cobblemon:chipped_pot",     "5.00"},
            {"cobblemon:sweet_apple",     "6.00"},
            {"cobblemon:tart_apple",      "6.00"},
            {"cobblemon:black_augurite",  "8.00"},
            {"cobblemon:peat_block",      "8.00"},
            {"cobblemon:auspicious_armor","15.00"},
            {"cobblemon:malicious_armor", "15.00"},
            {"cobblemon:galarica_cuff",   "8.00"},
            {"cobblemon:galarica_wreath", "8.00"},
        });

        // ── Invocadores / Cebo / Repelentes ──
        slot = addItems(list, slot, "invocadores", new String[][]{
            // Repelentes
            {"cobblemon:repel",           "1.00"},
            {"cobblemon:super_repel",     "2.00"},
            {"cobblemon:max_repel",       "3.50"},
            // Lures
            {"cobblemon:lure",            "2.50"},
            {"cobblemon:super_lure",      "4.50"},
            {"cobblemon:max_lure",        "8.00"},
            // Otros
            {"cobblemon:honey",           "1.50"},
            {"cobblemon:ability_charm",  "15.00"},
            {"cobblemon:shiny_charm",    "50.00"},
        });

        // ── Items de Batalla (Hold Items) ──
        slot = addItems(list, slot, "hold_items", new String[][]{
            // Objetos de elección
            {"cobblemon:choice_band",    "12.00"},
            {"cobblemon:choice_scarf",   "12.00"},
            {"cobblemon:choice_specs",   "12.00"},
            // Objetos competitivos
            {"cobblemon:life_orb",       "15.00"},
            {"cobblemon:leftovers",      "12.00"},
            {"cobblemon:rocky_helmet",    "8.00"},
            {"cobblemon:assault_vest",   "12.00"},
            {"cobblemon:focus_sash",      "8.00"},
            {"cobblemon:scope_lens",      "6.00"},
            {"cobblemon:wide_lens",       "6.00"},
            {"cobblemon:zoom_lens",       "6.00"},
            {"cobblemon:bright_powder",   "6.00"},
            {"cobblemon:quick_claw",      "8.00"},
            {"cobblemon:eject_button",    "8.00"},
            {"cobblemon:red_card",        "8.00"},
            {"cobblemon:ring_target",     "8.00"},
            {"cobblemon:binding_band",    "8.00"},
            {"cobblemon:expert_belt",     "8.00"},
            {"cobblemon:light_clay",      "8.00"},
            {"cobblemon:big_root",        "8.00"},
            // Objetos tipo
            {"cobblemon:black_belt",      "4.00"},
            {"cobblemon:black_glasses",   "4.00"},
            {"cobblemon:charcoal",        "4.00"},
            {"cobblemon:dragon_fang",     "4.00"},
            {"cobblemon:hard_stone",      "4.00"},
            {"cobblemon:magnet",          "4.00"},
            {"cobblemon:mystic_water",    "4.00"},
            {"cobblemon:never_melt_ice",  "4.00"},
            {"cobblemon:poison_barb",     "4.00"},
            {"cobblemon:sharp_beak",      "4.00"},
            {"cobblemon:silk_scarf",      "4.00"},
            {"cobblemon:silver_powder",   "4.00"},
            {"cobblemon:soft_sand",       "4.00"},
            {"cobblemon:spell_tag",       "4.00"},
            {"cobblemon:twisted_spoon",   "4.00"},
            {"cobblemon:metronome",       "6.00"},
            {"cobblemon:muscle_band",     "6.00"},
            {"cobblemon:wise_glasses",    "6.00"},
        });

        // ── TMs (Máquinas Técnicas) ──
        slot = addItems(list, slot, "tms", new String[][]{
            // TMs básicos (precio por tipo)
            {"cobblemon:tm_normal",        "5.00"},
            {"cobblemon:tm_fire",          "8.00"},
            {"cobblemon:tm_water",         "8.00"},
            {"cobblemon:tm_grass",         "8.00"},
            {"cobblemon:tm_electric",      "8.00"},
            {"cobblemon:tm_ice",           "8.00"},
            {"cobblemon:tm_fighting",      "8.00"},
            {"cobblemon:tm_poison",        "8.00"},
            {"cobblemon:tm_ground",        "8.00"},
            {"cobblemon:tm_flying",        "8.00"},
            {"cobblemon:tm_psychic",       "8.00"},
            {"cobblemon:tm_bug",           "8.00"},
            {"cobblemon:tm_rock",          "8.00"},
            {"cobblemon:tm_ghost",         "8.00"},
            {"cobblemon:tm_dragon",       "10.00"},
            {"cobblemon:tm_dark",          "8.00"},
            {"cobblemon:tm_steel",         "8.00"},
            {"cobblemon:tm_fairy",         "8.00"},
            // TMs de movimientos específicos populares
            {"cobblemon:tm_swords_dance", "10.00"},
            {"cobblemon:tm_dragon_dance", "12.00"},
            {"cobblemon:tm_nasty_plot",   "10.00"},
            {"cobblemon:tm_calm_mind",    "10.00"},
            {"cobblemon:tm_bulk_up",      "10.00"},
            {"cobblemon:tm_stealth_rock", "12.00"},
            {"cobblemon:tm_spikes",       "10.00"},
            {"cobblemon:tm_toxic_spikes", "10.00"},
            {"cobblemon:tm_sticky_web",   "12.00"},
            {"cobblemon:tm_trick_room",   "10.00"},
            {"cobblemon:tm_earthquake",   "12.00"},
            {"cobblemon:tm_ice_beam",     "10.00"},
            {"cobblemon:tm_flamethrower", "10.00"},
            {"cobblemon:tm_thunderbolt",  "10.00"},
            {"cobblemon:tm_surf",         "10.00"},
            {"cobblemon:tm_energy_ball",  "10.00"},
            {"cobblemon:tm_psychic",      "10.00"},
            {"cobblemon:tm_shadow_ball",  "10.00"},
            {"cobblemon:tm_focus_blast",  "12.00"},
            {"cobblemon:tm_sludge_bomb",  "10.00"},
            {"cobblemon:tm_stone_edge",   "10.00"},
            {"cobblemon:tm_u_turn",       "10.00"},
            {"cobblemon:tm_volt_switch",  "10.00"},
        });

        // ── Gemas ──
        slot = addItems(list, slot, "gemas", new String[][]{
            {"cobblemon:normal_gem",    "2.50"},
            {"cobblemon:fire_gem",      "2.50"},
            {"cobblemon:water_gem",     "2.50"},
            {"cobblemon:grass_gem",     "2.50"},
            {"cobblemon:electric_gem",  "2.50"},
            {"cobblemon:ice_gem",       "2.50"},
            {"cobblemon:fighting_gem",  "2.50"},
            {"cobblemon:poison_gem",    "2.50"},
            {"cobblemon:ground_gem",    "2.50"},
            {"cobblemon:flying_gem",    "2.50"},
            {"cobblemon:psychic_gem",   "2.50"},
            {"cobblemon:bug_gem",       "2.50"},
            {"cobblemon:rock_gem",      "2.50"},
            {"cobblemon:ghost_gem",     "2.50"},
            {"cobblemon:dragon_gem",    "2.50"},
            {"cobblemon:dark_gem",      "2.50"},
            {"cobblemon:steel_gem",     "2.50"},
            {"cobblemon:fairy_gem",     "2.50"},
        });

        // ── Z-Cristales ──
        slot = addItems(list, slot, "z_cristales", new String[][]{
            {"cobblemon:normalium_z",   "20.00"},
            {"cobblemon:fightinium_z",  "20.00"},
            {"cobblemon:firium_z",      "20.00"},
            {"cobblemon:waterium_z",    "20.00"},
            {"cobblemon:grassium_z",    "20.00"},
            {"cobblemon:electrium_z",   "20.00"},
            {"cobblemon:glacium_z",     "20.00"},
            {"cobblemon:groundium_z",   "20.00"},
            {"cobblemon:psychium_z",    "20.00"},
            {"cobblemon:buginium_z",    "20.00"},
            {"cobblemon:rockium_z",     "20.00"},
            {"cobblemon:ghostium_z",    "20.00"},
            {"cobblemon:dragonium_z",   "20.00"},
            {"cobblemon:darkinium_z",   "20.00"},
            {"cobblemon:steelium_z",    "20.00"},
            {"cobblemon:fairium_z",     "20.00"},
            {"cobblemon:pikanium_z",    "25.00"},
            {"cobblemon:eevium_z",      "25.00"},
            {"cobblemon:soul_heart",    "30.00"},
        });

        // ── Mega Piedras ──
        slot = addItems(list, slot, "mega_piedras", new String[][]{
            {"cobblemon:venusaurite",       "50.00"},
            {"cobblemon:charizardite_x",    "50.00"},
            {"cobblemon:charizardite_y",    "50.00"},
            {"cobblemon:blastoisinite",     "50.00"},
            {"cobblemon:alakazite",         "50.00"},
            {"cobblemon:gengarite",         "50.00"},
            {"cobblemon:kangaskhanite",     "50.00"},
            {"cobblemon:pinsirite",         "50.00"},
            {"cobblemon:gyaradosite",       "50.00"},
            {"cobblemon:aerodactylite",     "50.00"},
            {"cobblemon:mewtwonite_x",      "75.00"},
            {"cobblemon:mewtwonite_y",      "75.00"},
            {"cobblemon:ampharosite",       "50.00"},
            {"cobblemon:scizorite",         "50.00"},
            {"cobblemon:heracronite",       "50.00"},
            {"cobblemon:houndoominite",     "50.00"},
            {"cobblemon:tyranitarite",      "50.00"},
            {"cobblemon:blazikenite",       "50.00"},
            {"cobblemon:gardevoirite",      "50.00"},
            {"cobblemon:mawilite",          "50.00"},
            {"cobblemon:aggronite",         "50.00"},
            {"cobblemon:medichamite",       "50.00"},
            {"cobblemon:manectite",         "50.00"},
            {"cobblemon:banettite",         "50.00"},
            {"cobblemon:absolite",          "50.00"},
            {"cobblemon:garchompite",       "50.00"},
            {"cobblemon:lucarionite",       "50.00"},
            {"cobblemon:gyaradosite",       "50.00"},
        });

        // ── Objetos de Raid ──
        slot = addItems(list, slot, "raids", new String[][]{
            {"cobblemon:dynamax_crystal",  "30.00"},
            {"cobblemon:max_mushroom",      "5.00"},
            {"cobblemon:max_honey",         "3.00"},
            {"cobblemon:wishing_piece",    "10.00"},
            {"cobblemon:star_piece",       "15.00"},
        });

        return list;
    }

    private static int addItems(List<ShopItem> list, int startSlot,
                                  String category, String[][] items) {
        int slot = startSlot;
        for (String[] entry : items) {
            String itemId = entry[0];
            double price  = Double.parseDouble(entry[1]);
            // Build minimal NBT string for Cobblemon items
            String nbt = "{id:\"" + itemId + "\",count:1}";
            ShopItem si = new ShopItem(slot, nbt, price);
            si.category = category;
            list.add(si);
            slot++;
        }
        return slot;
    }
}
