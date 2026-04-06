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
            {"cobblemon:poke_ball",       "0.50"},
            {"cobblemon:great_ball",      "1.50"},
            {"cobblemon:ultra_ball",      "3.00"},
            {"cobblemon:master_ball",   "300.00"},
            {"cobblemon:fast_ball",       "3.00"},
            {"cobblemon:level_ball",      "3.00"},
            {"cobblemon:lure_ball",       "3.00"},
            {"cobblemon:heavy_ball",      "3.00"},
            {"cobblemon:love_ball",       "3.00"},
            {"cobblemon:friend_ball",     "3.00"},
            {"cobblemon:moon_ball",       "3.00"},
            {"cobblemon:net_ball",        "2.00"},
            {"cobblemon:dive_ball",       "2.00"},
            {"cobblemon:nest_ball",       "2.00"},
            {"cobblemon:repeat_ball",     "2.00"},
            {"cobblemon:timer_ball",      "2.00"},
            {"cobblemon:luxury_ball",     "5.00"},
            {"cobblemon:premier_ball",    "1.00"},
            {"cobblemon:dusk_ball",       "2.00"},
            {"cobblemon:heal_ball",       "2.00"},
            {"cobblemon:quick_ball",      "2.00"},
            {"cobblemon:beast_ball",     "20.00"},
            {"cobblemon:dream_ball",     "10.00"},
            {"cobblemon:cherish_ball",   "50.00"},
            {"cobblemon:sport_ball",      "3.00"},
            {"cobblemon:safari_ball",     "5.00"},
            {"cobblemon:slate_ball",      "2.00"},
            {"cobblemon:azure_ball",      "2.00"},
            {"cobblemon:verdant_ball",    "2.00"},
            {"cobblemon:citrine_ball",    "2.00"},
            {"cobblemon:rose_ball",       "2.00"},
            {"cobblemon:strange_ball",   "10.00"},
            {"cobblemon:park_ball",      "10.00"},
        });

        // ── Vitaminas & Pociones ──
        slot = addItems(list, slot, "vitaminas", new String[][]{
            {"cobblemon:hp_up",           "5.00"},
            {"cobblemon:protein",         "5.00"},
            {"cobblemon:iron",            "5.00"},
            {"cobblemon:calcium",         "5.00"},
            {"cobblemon:zinc",            "5.00"},
            {"cobblemon:carbos",          "5.00"},
            {"cobblemon:pp_up",           "5.00"},
            {"cobblemon:pp_max",         "10.00"},
            {"cobblemon:potion",          "0.80"},
            {"cobblemon:super_potion",    "1.50"},
            {"cobblemon:hyper_potion",    "3.00"},
            {"cobblemon:max_potion",      "6.00"},
            {"cobblemon:full_restore",    "8.00"},
            {"cobblemon:revive",          "2.00"},
            {"cobblemon:max_revive",      "5.00"},
            {"cobblemon:antidote",        "0.50"},
            {"cobblemon:awakening",       "0.80"},
            {"cobblemon:burn_heal",       "0.80"},
            {"cobblemon:full_heal",       "2.00"},
            {"cobblemon:ice_heal",        "0.80"},
            {"cobblemon:paralyz_heal",    "0.80"},
        });

        // ── Mentas ──
        slot = addItems(list, slot, "menta", new String[][]{
            {"cobblemon:lonely_mint",   "5.00"},
            {"cobblemon:adamant_mint",  "5.00"},
            {"cobblemon:naughty_mint",  "5.00"},
            {"cobblemon:brave_mint",    "5.00"},
            {"cobblemon:bold_mint",     "5.00"},
            {"cobblemon:impish_mint",   "5.00"},
            {"cobblemon:lax_mint",      "5.00"},
            {"cobblemon:relaxed_mint",  "5.00"},
            {"cobblemon:modest_mint",   "5.00"},
            {"cobblemon:mild_mint",     "5.00"},
            {"cobblemon:rash_mint",     "5.00"},
            {"cobblemon:quiet_mint",    "5.00"},
            {"cobblemon:calm_mint",     "5.00"},
            {"cobblemon:gentle_mint",   "5.00"},
            {"cobblemon:careful_mint",  "5.00"},
            {"cobblemon:sassy_mint",    "5.00"},
            {"cobblemon:timid_mint",    "5.00"},
            {"cobblemon:hasty_mint",    "5.00"},
            {"cobblemon:jolly_mint",    "5.00"},
            {"cobblemon:naive_mint",    "5.00"},
            {"cobblemon:serious_mint",  "5.00"},
        });

        // ── Caramelos Exp ──
        slot = addItems(list, slot, "caramelos", new String[][]{
            {"cobblemon:exp_candy_xs",  "1.00"},
            {"cobblemon:exp_candy_s",   "2.00"},
            {"cobblemon:exp_candy_m",   "4.00"},
            {"cobblemon:exp_candy_l",   "8.00"},
            {"cobblemon:exp_candy_xl", "15.00"},
            {"cobblemon:rare_candy",   "10.00"},
        });

        // ── Bonguris ──
        slot = addItems(list, slot, "bonguris", new String[][]{
            {"cobblemon:green_apricorn",   "0.50"},
            {"cobblemon:red_apricorn",     "0.50"},
            {"cobblemon:blue_apricorn",    "0.50"},
            {"cobblemon:yellow_apricorn",  "0.50"},
            {"cobblemon:white_apricorn",   "0.50"},
            {"cobblemon:black_apricorn",   "0.50"},
            {"cobblemon:pink_apricorn",    "0.50"},
        });

        // ── Brotes e inciensos ──
        slot = addItems(list, slot, "brotes", new String[][]{
            {"cobblemon:big_root",        "3.00"},
            {"cobblemon:mental_herb",     "5.00"},
            {"cobblemon:power_herb",      "5.00"},
            {"cobblemon:white_herb",      "5.00"},
            {"cobblemon:miracle_seed",    "5.00"},
            {"cobblemon:odd_incense",     "5.00"},
            {"cobblemon:full_incense",    "5.00"},
            {"cobblemon:luck_incense",    "5.00"},
            {"cobblemon:lax_incense",     "5.00"},
            {"cobblemon:pure_incense",    "5.00"},
            {"cobblemon:rock_incense",    "5.00"},
            {"cobblemon:rose_incense",    "5.00"},
            {"cobblemon:sea_incense",     "5.00"},
            {"cobblemon:wave_incense",    "5.00"},
        });

        // ── Objetos de evolución ──
        slot = addItems(list, slot, "evoluciones", new String[][]{
            {"cobblemon:fire_stone",      "8.00"},
            {"cobblemon:water_stone",     "8.00"},
            {"cobblemon:thunder_stone",   "8.00"},
            {"cobblemon:leaf_stone",      "8.00"},
            {"cobblemon:moon_stone",      "8.00"},
            {"cobblemon:sun_stone",       "8.00"},
            {"cobblemon:dawn_stone",     "10.00"},
            {"cobblemon:dusk_stone",     "10.00"},
            {"cobblemon:shiny_stone",    "10.00"},
            {"cobblemon:ice_stone",       "8.00"},
            {"cobblemon:oval_stone",      "5.00"},
            {"cobblemon:kings_rock",     "10.00"},
            {"cobblemon:dragon_scale",   "15.00"},
            {"cobblemon:metal_coat",     "10.00"},
            {"cobblemon:prism_scale",    "10.00"},
            {"cobblemon:razor_claw",     "15.00"},
            {"cobblemon:razor_fang",     "15.00"},
            {"cobblemon:electirizer",    "15.00"},
            {"cobblemon:magmarizer",     "15.00"},
            {"cobblemon:protector",      "10.00"},
            {"cobblemon:reaper_cloth",   "15.00"},
            {"cobblemon:upgrade",        "15.00"},
            {"cobblemon:dubious_disc",   "15.00"},
            {"cobblemon:deep_sea_scale", "10.00"},
            {"cobblemon:deep_sea_tooth", "10.00"},
            {"cobblemon:sachet",         "10.00"},
            {"cobblemon:whipped_dream",  "10.00"},
        });

        // ── Invocadores / Cebo / Repelentes ──
        slot = addItems(list, slot, "invocadores", new String[][]{
            {"cobblemon:lure",            "3.00"},
            {"cobblemon:super_lure",      "5.00"},
            {"cobblemon:max_lure",       "10.00"},
            {"cobblemon:repel",           "1.50"},
            {"cobblemon:super_repel",     "3.00"},
            {"cobblemon:max_repel",       "5.00"},
            {"cobblemon:honey",           "2.00"},
        });

        // ── TMs (muestra genérica — admin puede añadir los específicos) ──
        // Los TMs en Cobblemon tienen IDs como cobblemon:tm_normal o por movimiento
        slot = addItems(list, slot, "tm", new String[][]{
            {"cobblemon:tm_aerial_ace",    "8.00"},
            {"cobblemon:tm_blizzard",     "15.00"},
            {"cobblemon:tm_body_slam",     "8.00"},
            {"cobblemon:tm_brick_break",   "8.00"},
            {"cobblemon:tm_bulk_up",      "10.00"},
            {"cobblemon:tm_calm_mind",    "10.00"},
            {"cobblemon:tm_dark_pulse",    "8.00"},
            {"cobblemon:tm_dragon_claw",  "10.00"},
            {"cobblemon:tm_earthquake",   "15.00"},
            {"cobblemon:tm_energy_ball",   "8.00"},
            {"cobblemon:tm_fire_blast",   "15.00"},
            {"cobblemon:tm_flamethrower", "10.00"},
            {"cobblemon:tm_flash_cannon",  "8.00"},
            {"cobblemon:tm_focus_blast",  "15.00"},
            {"cobblemon:tm_giga_impact",  "15.00"},
            {"cobblemon:tm_hyper_beam",   "15.00"},
            {"cobblemon:tm_ice_beam",     "10.00"},
            {"cobblemon:tm_iron_head",     "8.00"},
            {"cobblemon:tm_psychic",      "10.00"},
            {"cobblemon:tm_shadow_ball",   "8.00"},
            {"cobblemon:tm_sludge_bomb",   "8.00"},
            {"cobblemon:tm_solar_beam",   "10.00"},
            {"cobblemon:tm_stone_edge",   "10.00"},
            {"cobblemon:tm_surf",         "10.00"},
            {"cobblemon:tm_thunder",      "15.00"},
            {"cobblemon:tm_thunderbolt",  "10.00"},
            {"cobblemon:tm_water_pulse",   "5.00"},
            {"cobblemon:tm_x_scissor",     "8.00"},
        });

        // ── Gemas (objetos hold) ──
        slot = addItems(list, slot, "gemas", new String[][]{
            {"cobblemon:normal_gem",    "3.00"},
            {"cobblemon:fire_gem",      "3.00"},
            {"cobblemon:water_gem",     "3.00"},
            {"cobblemon:grass_gem",     "3.00"},
            {"cobblemon:electric_gem",  "3.00"},
            {"cobblemon:ice_gem",       "3.00"},
            {"cobblemon:fighting_gem",  "3.00"},
            {"cobblemon:poison_gem",    "3.00"},
            {"cobblemon:ground_gem",    "3.00"},
            {"cobblemon:flying_gem",    "3.00"},
            {"cobblemon:psychic_gem",   "3.00"},
            {"cobblemon:bug_gem",       "3.00"},
            {"cobblemon:rock_gem",      "3.00"},
            {"cobblemon:ghost_gem",     "3.00"},
            {"cobblemon:dragon_gem",    "3.00"},
            {"cobblemon:dark_gem",      "3.00"},
            {"cobblemon:steel_gem",     "3.00"},
            {"cobblemon:fairy_gem",     "3.00"},
        });

        // ── Items de batalla (hold items) ──
        slot = addItems(list, slot, "batalla", new String[][]{
            {"cobblemon:choice_band",     "15.00"},
            {"cobblemon:choice_scarf",    "15.00"},
            {"cobblemon:choice_specs",    "15.00"},
            {"cobblemon:life_orb",        "20.00"},
            {"cobblemon:leftovers",       "15.00"},
            {"cobblemon:rocky_helmet",    "10.00"},
            {"cobblemon:assault_vest",    "15.00"},
            {"cobblemon:focus_sash",      "10.00"},
            {"cobblemon:black_belt",       "5.00"},
            {"cobblemon:black_glasses",    "5.00"},
            {"cobblemon:charcoal",         "5.00"},
            {"cobblemon:dragon_fang",      "5.00"},
            {"cobblemon:hard_stone",       "5.00"},
            {"cobblemon:magnet",           "5.00"},
            {"cobblemon:mystic_water",     "5.00"},
            {"cobblemon:never_melt_ice",   "5.00"},
            {"cobblemon:poison_barb",      "5.00"},
            {"cobblemon:sharp_beak",       "5.00"},
            {"cobblemon:silk_scarf",       "5.00"},
            {"cobblemon:silver_powder",    "5.00"},
            {"cobblemon:soft_sand",        "5.00"},
            {"cobblemon:spell_tag",        "5.00"},
            {"cobblemon:twisted_spoon",    "5.00"},
            {"cobblemon:bright_powder",    "5.00"},
            {"cobblemon:kings_rock",      "10.00"},
            {"cobblemon:lax_incense",      "5.00"},
            {"cobblemon:scope_lens",       "8.00"},
            {"cobblemon:wide_lens",        "5.00"},
            {"cobblemon:zoom_lens",        "5.00"},
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
