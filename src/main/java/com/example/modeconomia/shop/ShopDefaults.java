package com.cobblemania.economia.shop;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.ShopItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

public final class ShopDefaults {
    private ShopDefaults() {}

    private static final Map<String, Object[]> DEFAULTS = new LinkedHashMap<>();

    static {
        // ══ POKÉBOLAS ══
        p("cobblemon:poke_ball",        "pokeballs",  1.0);
        p("cobblemon:great_ball",       "pokeballs",  1.0);
        p("cobblemon:ultra_ball",       "pokeballs",  1.0);
        p("cobblemon:master_ball",      "pokeballs",  1.0);
        p("cobblemon:safari_ball",      "pokeballs",  1.0);
        p("cobblemon:fast_ball",        "pokeballs",  1.0);
        p("cobblemon:level_ball",       "pokeballs",  1.0);
        p("cobblemon:lure_ball",        "pokeballs",  1.0);
        p("cobblemon:heavy_ball",       "pokeballs",  1.0);
        p("cobblemon:love_ball",        "pokeballs",  1.0);
        p("cobblemon:friend_ball",      "pokeballs",  1.0);
        p("cobblemon:moon_ball",        "pokeballs",  1.0);
        p("cobblemon:sport_ball",       "pokeballs",  1.0);
        p("cobblemon:net_ball",         "pokeballs",  1.0);
        p("cobblemon:nest_ball",        "pokeballs",  1.0);
        p("cobblemon:repeat_ball",      "pokeballs",  1.0);
        p("cobblemon:timer_ball",       "pokeballs",  1.0);
        p("cobblemon:luxury_ball",      "pokeballs",  1.0);
        p("cobblemon:premier_ball",     "pokeballs",  1.0);
        p("cobblemon:dusk_ball",        "pokeballs",  1.0);
        p("cobblemon:heal_ball",        "pokeballs",  1.0);
        p("cobblemon:quick_ball",       "pokeballs",  1.0);
        p("cobblemon:cherish_ball",     "pokeballs",  1.0);
        p("cobblemon:dive_ball",        "pokeballs",  1.0);
        p("cobblemon:dream_ball",       "pokeballs",  1.0);
        p("cobblemon:beast_ball",       "pokeballs",  1.0);
        p("cobblemon:park_ball",        "pokeballs",  1.0);
        p("cobblemon:strange_ball",     "pokeballs",  1.0);

        // ══ BAYAS ══
        p("cobblemon:oran_berry",       "bayas",  1.0);
        p("cobblemon:sitrus_berry",     "bayas",  1.0);
        p("cobblemon:lum_berry",        "bayas",  1.0);
        p("cobblemon:leppa_berry",      "bayas",  1.0);
        p("cobblemon:cheri_berry",      "bayas",  1.0);
        p("cobblemon:chesto_berry",     "bayas",  1.0);
        p("cobblemon:pecha_berry",      "bayas",  1.0);
        p("cobblemon:rawst_berry",      "bayas",  1.0);
        p("cobblemon:aspear_berry",     "bayas",  1.0);
        p("cobblemon:persim_berry",     "bayas",  1.0);
        p("cobblemon:razz_berry",       "bayas",  1.0);
        p("cobblemon:bluk_berry",       "bayas",  1.0);
        p("cobblemon:nanab_berry",      "bayas",  1.0);
        p("cobblemon:wepear_berry",     "bayas",  1.0);
        p("cobblemon:pinap_berry",      "bayas",  1.0);
        p("cobblemon:figy_berry",       "bayas",  1.0);
        p("cobblemon:wiki_berry",       "bayas",  1.0);
        p("cobblemon:mago_berry",       "bayas",  1.0);
        p("cobblemon:aguav_berry",      "bayas",  1.0);
        p("cobblemon:iapapa_berry",     "bayas",  1.0);
        p("cobblemon:liechi_berry",     "bayas",  1.0);
        p("cobblemon:ganlon_berry",     "bayas",  1.0);
        p("cobblemon:salac_berry",      "bayas",  1.0);
        p("cobblemon:petaya_berry",     "bayas",  1.0);
        p("cobblemon:apicot_berry",     "bayas",  1.0);
        p("cobblemon:lansat_berry",     "bayas",  1.0);
        p("cobblemon:starf_berry",      "bayas",  1.0);
        p("cobblemon:jaboca_berry",     "bayas",  1.0);
        p("cobblemon:rowap_berry",      "bayas",  1.0);
        p("cobblemon:enigma_berry",     "bayas",  1.0);
        p("cobblemon:micle_berry",      "bayas",  1.0);
        p("cobblemon:custap_berry",     "bayas",  1.0);

        // ══ VITAMINAS ══
        p("cobblemon:hp_up",            "vitaminas",  1.0);
        p("cobblemon:protein",          "vitaminas",  1.0);
        p("cobblemon:iron",             "vitaminas",  1.0);
        p("cobblemon:calcium",          "vitaminas",  1.0);
        p("cobblemon:zinc",             "vitaminas",  1.0);
        p("cobblemon:carbos",           "vitaminas",  1.0);
        p("cobblemon:health_feather",   "vitaminas",  1.0);
        p("cobblemon:muscle_feather",   "vitaminas",  1.0);
        p("cobblemon:resist_feather",   "vitaminas",  1.0);
        p("cobblemon:genius_feather",   "vitaminas",  1.0);
        p("cobblemon:clever_feather",   "vitaminas",  1.0);
        p("cobblemon:swift_feather",    "vitaminas",  1.0);
        p("cobblemon:power_weight",     "vitaminas",  1.0);
        p("cobblemon:power_bracer",     "vitaminas",  1.0);
        p("cobblemon:power_belt",       "vitaminas",  1.0);
        p("cobblemon:power_lens",       "vitaminas",  1.0);
        p("cobblemon:power_band",       "vitaminas",  1.0);
        p("cobblemon:power_anklet",     "vitaminas",  1.0);
        p("cobblemon:macho_brace",      "vitaminas",  1.0);
        p("cobblemon:exp_share",        "vitaminas",  1.0);
        p("cobblemon:lucky_egg",        "vitaminas",  1.0);

        // ══ PP ══
        p("cobblemon:pp_up",            "pp",  1.0);
        p("cobblemon:pp_max",           "pp",  1.0);
        p("cobblemon:ether",            "pp",  1.0);
        p("cobblemon:max_ether",        "pp",  1.0);
        p("cobblemon:elixir",           "pp",  1.0);
        p("cobblemon:max_elixir",       "pp",  1.0);

        // ══ BONGURIS ══
        p("cobblemon:green_apricorn",   "bonguris",  1.0);
        p("cobblemon:red_apricorn",     "bonguris",  1.0);
        p("cobblemon:blue_apricorn",    "bonguris",  1.0);
        p("cobblemon:yellow_apricorn",  "bonguris",  1.0);
        p("cobblemon:white_apricorn",   "bonguris",  1.0);
        p("cobblemon:black_apricorn",   "bonguris",  1.0);
        p("cobblemon:pink_apricorn",    "bonguris",  1.0);

        // ══ BROTES / INCIENSOS ══
        p("cobblemon:full_incense",     "brotes",  1.0);
        p("cobblemon:lax_incense",      "brotes",  1.0);
        p("cobblemon:luck_incense",     "brotes",  1.0);
        p("cobblemon:odd_incense",      "brotes",  1.0);
        p("cobblemon:pure_incense",     "brotes",  1.0);
        p("cobblemon:rock_incense",     "brotes",  1.0);
        p("cobblemon:rose_incense",     "brotes",  1.0);
        p("cobblemon:sea_incense",      "brotes",  1.0);
        p("cobblemon:wave_incense",     "brotes",  1.0);

        // ══ EVOLUCIONES ══
        p("cobblemon:fire_stone",       "evoluciones",  1.0);
        p("cobblemon:water_stone",      "evoluciones",  1.0);
        p("cobblemon:thunder_stone",    "evoluciones",  1.0);
        p("cobblemon:leaf_stone",       "evoluciones",  1.0);
        p("cobblemon:moon_stone",       "evoluciones",  1.0);
        p("cobblemon:sun_stone",        "evoluciones",  1.0);
        p("cobblemon:shiny_stone",      "evoluciones",  1.0);
        p("cobblemon:dusk_stone",       "evoluciones",  1.0);
        p("cobblemon:dawn_stone",       "evoluciones",  1.0);
        p("cobblemon:ice_stone",        "evoluciones",  1.0);
        p("cobblemon:linking_cord",     "evoluciones",  1.0);
        p("cobblemon:kings_rock",       "evoluciones",  1.0);
        p("cobblemon:metal_coat",       "evoluciones",  1.0);
        p("cobblemon:dragon_scale",     "evoluciones",  1.0);
        p("cobblemon:upgrade",          "evoluciones",  1.0);
        p("cobblemon:dubious_disc",     "evoluciones",  1.0);
        p("cobblemon:protector",        "evoluciones",  1.0);
        p("cobblemon:electirizer",      "evoluciones",  1.0);
        p("cobblemon:magmarizer",       "evoluciones",  1.0);
        p("cobblemon:razor_fang",       "evoluciones",  1.0);
        p("cobblemon:razor_claw",       "evoluciones",  1.0);
        p("cobblemon:oval_stone",       "evoluciones",  1.0);
        p("cobblemon:prism_scale",      "evoluciones",  1.0);
        p("cobblemon:deep_sea_tooth",   "evoluciones",  1.0);
        p("cobblemon:deep_sea_scale",   "evoluciones",  1.0);
        p("cobblemon:reaper_cloth",     "evoluciones",  1.0);
        p("cobblemon:sachet",           "evoluciones",  1.0);
        p("cobblemon:whipped_dream",    "evoluciones",  1.0);
        p("cobblemon:cracked_pot",      "evoluciones",  1.0);
        p("cobblemon:chipped_pot",      "evoluciones",  1.0);
        p("cobblemon:sweet_apple",      "evoluciones",  1.0);
        p("cobblemon:tart_apple",       "evoluciones",  1.0);
        p("cobblemon:black_augurite",   "evoluciones",  1.0);
        p("cobblemon:peat_block",       "evoluciones",  1.0);
        p("cobblemon:auspicious_armor", "evoluciones",  1.0);
        p("cobblemon:malicious_armor",  "evoluciones",  1.0);

        // ══ CARAMELOS EXP ══
        p("cobblemon:exp_candy_xs",     "caramelos",  1.0);
        p("cobblemon:exp_candy_s",      "caramelos",  1.0);
        p("cobblemon:exp_candy_m",      "caramelos",  1.0);
        p("cobblemon:exp_candy_l",      "caramelos",  1.0);
        p("cobblemon:exp_candy_xl",     "caramelos",  1.0);
        p("cobblemon:rare_candy",       "caramelos",  1.0);

        // ══ MENTAS (MINTS) ══
        p("cobblemon:lonely_mint",      "mentas",  1.0);
        p("cobblemon:adamant_mint",     "mentas",  1.0);
        p("cobblemon:naughty_mint",     "mentas",  1.0);
        p("cobblemon:brave_mint",       "mentas",  1.0);
        p("cobblemon:bold_mint",        "mentas",  1.0);
        p("cobblemon:impish_mint",      "mentas",  1.0);
        p("cobblemon:lax_mint",         "mentas",  1.0);
        p("cobblemon:relaxed_mint",     "mentas",  1.0);
        p("cobblemon:modest_mint",      "mentas",  1.0);
        p("cobblemon:mild_mint",        "mentas",  1.0);
        p("cobblemon:rash_mint",        "mentas",  1.0);
        p("cobblemon:quiet_mint",       "mentas",  1.0);
        p("cobblemon:calm_mint",        "mentas",  1.0);
        p("cobblemon:gentle_mint",      "mentas",  1.0);
        p("cobblemon:careful_mint",     "mentas",  1.0);
        p("cobblemon:sassy_mint",       "mentas",  1.0);
        p("cobblemon:timid_mint",       "mentas",  1.0);
        p("cobblemon:hasty_mint",       "mentas",  1.0);
        p("cobblemon:jolly_mint",       "mentas",  1.0);
        p("cobblemon:naive_mint",       "mentas",  1.0);
        p("cobblemon:bashful_mint",     "mentas",  1.0);
        p("cobblemon:docile_mint",      "mentas",  1.0);
        p("cobblemon:hardy_mint",       "mentas",  1.0);
        p("cobblemon:quirky_mint",      "mentas",  1.0);
        p("cobblemon:serious_mint",     "mentas",  1.0);

        // ══ ITEMS DE BATALLA ══
        p("cobblemon:x_attack",         "batalla",  1.0);
        p("cobblemon:x_defense",        "batalla",  1.0);
        p("cobblemon:x_speed",          "batalla",  1.0);
        p("cobblemon:x_sp_atk",         "batalla",  1.0);
        p("cobblemon:x_sp_def",         "batalla",  1.0);
        p("cobblemon:x_accuracy",       "batalla",  1.0);
        p("cobblemon:dire_hit",         "batalla",  1.0);
        p("cobblemon:guard_spec",       "batalla",  1.0);
        p("cobblemon:full_heal",        "batalla",  1.0);
        p("cobblemon:full_restore",     "batalla",  1.0);
        p("cobblemon:max_restore",      "batalla",  1.0);
        p("cobblemon:hyper_potion",     "batalla",  1.0);
        p("cobblemon:super_potion",     "batalla",  1.0);
        p("cobblemon:potion",           "batalla",  1.0);
        p("cobblemon:antidote",         "batalla",  1.0);
        p("cobblemon:awakening",        "batalla",  1.0);
        p("cobblemon:burn_heal",        "batalla",  1.0);
        p("cobblemon:ice_heal",         "batalla",  1.0);
        p("cobblemon:paralyze_heal",    "batalla",  1.0);
        p("cobblemon:revive",           "batalla",  1.0);
        p("cobblemon:max_revive",       "batalla",  1.0);

        // ══ GEMAS ══
        p("cobblemon:fire_gem",         "gemas",  1.0);
        p("cobblemon:water_gem",        "gemas",  1.0);
        p("cobblemon:grass_gem",        "gemas",  1.0);
        p("cobblemon:electric_gem",     "gemas",  1.0);
        p("cobblemon:ice_gem",          "gemas",  1.0);
        p("cobblemon:fighting_gem",     "gemas",  1.0);
        p("cobblemon:poison_gem",       "gemas",  1.0);
        p("cobblemon:ground_gem",       "gemas",  1.0);
        p("cobblemon:flying_gem",       "gemas",  1.0);
        p("cobblemon:psychic_gem",      "gemas",  1.0);
        p("cobblemon:bug_gem",          "gemas",  1.0);
        p("cobblemon:rock_gem",         "gemas",  1.0);
        p("cobblemon:ghost_gem",        "gemas",  1.0);
        p("cobblemon:dragon_gem",       "gemas",  1.0);
        p("cobblemon:dark_gem",         "gemas",  1.0);
        p("cobblemon:steel_gem",        "gemas",  1.0);
        p("cobblemon:normal_gem",       "gemas",  1.0);
        p("cobblemon:fairy_gem",        "gemas",  1.0);

        // ══ INVOCADORES ══
        p("cobblemon:azure_flute",      "invocadores",  1.0);
        p("cobblemon:red_chain",        "invocadores",  1.0);
        p("cobblemon:clear_bell",       "invocadores",  1.0);
        p("cobblemon:tidal_bell",       "invocadores",  1.0);
        p("cobblemon:blue_orb",         "invocadores",  1.0);
        p("cobblemon:red_orb",          "invocadores",  1.0);
        p("cobblemon:jade_orb",         "invocadores",  1.0);
        p("cobblemon:adamant_orb",      "invocadores",  1.0);
        p("cobblemon:lustrous_orb",     "invocadores",  1.0);
        p("cobblemon:griseous_orb",     "invocadores",  1.0);
        p("cobblemon:prison_bottle",    "invocadores",  1.0);
        p("cobblemon:reveal_glass",     "invocadores",  1.0);
        p("cobblemon:rusted_sword",     "invocadores",  1.0);
        p("cobblemon:rusted_shield",    "invocadores",  1.0);

        // ══ MTs / TMs ══
        // Cobblemon 1.7.3 uses cobblemon:technical_machine as the single TM item
        // (with move data stored in the item's NBT/components)
        // The type-based tm_* items are kept as fallback for other versions
        p("cobblemon:technical_machine", "tms",  1.0);
        p("cobblemon:tm_normal",         "tms",  1.0);
        p("cobblemon:tm_fire",           "tms",  1.0);
        p("cobblemon:tm_water",          "tms",  1.0);
        p("cobblemon:tm_grass",          "tms",  1.0);
        p("cobblemon:tm_electric",       "tms",  1.0);
        p("cobblemon:tm_ice",            "tms",  1.0);
        p("cobblemon:tm_fighting",       "tms",  1.0);
        p("cobblemon:tm_poison",         "tms",  1.0);
        p("cobblemon:tm_ground",         "tms",  1.0);
        p("cobblemon:tm_flying",         "tms",  1.0);
        p("cobblemon:tm_psychic",        "tms",  1.0);
        p("cobblemon:tm_bug",            "tms",  1.0);
        p("cobblemon:tm_rock",           "tms",  1.0);
        p("cobblemon:tm_ghost",          "tms",  1.0);
        p("cobblemon:tm_dragon",         "tms",  1.0);
        p("cobblemon:tm_dark",           "tms",  1.0);
        p("cobblemon:tm_steel",          "tms",  1.0);
        p("cobblemon:tm_fairy",          "tms",  1.0);
    }

    private static void p(String id, String cat, double price) {
        DEFAULTS.put(id, new Object[]{cat, price});
    }

    public static void populate(MinecraftServer server) {
        if (ModeconomiaConfig.DATA.shop.items == null)
            ModeconomiaConfig.DATA.shop.items = new ArrayList<>();

        // Build set of already-present item IDs
        Set<String> presentIds = ModeconomiaConfig.DATA.shop.items.stream()
            .map(si -> {
                try {
                    ItemStack s = com.cobblemania.economia.util.ItemStackUtil
                        .fromNbtString(si.itemNbt, server.getRegistryManager());
                    return Registries.ITEM.getId(s.getItem()).toString();
                } catch (Exception e) { return ""; }
            })
            .collect(Collectors.toSet());

        // Migrate existing items with wrong/null category
        boolean migrated = false;
        for (ShopItem si : ModeconomiaConfig.DATA.shop.items) {
            try {
                ItemStack s = com.cobblemania.economia.util.ItemStackUtil
                    .fromNbtString(si.itemNbt, server.getRegistryManager());
                String detected = CategoryDetector.detect(s);
                if (!detected.equals(si.category)) {
                    si.category = detected;
                    migrated = true;
                }
            } catch (Exception ignored) {}
        }

        // Add missing items
        int added = 0;
        for (Map.Entry<String, Object[]> entry : DEFAULTS.entrySet()) {
            String itemId   = entry.getKey();
            String category = (String) entry.getValue()[0];
            double price    = (double) entry.getValue()[1];
            if (presentIds.contains(itemId)) continue;
            try {
                var item = Registries.ITEM.get(Identifier.of(itemId));
                if (item == Items.AIR) continue;
                ItemStack stack = new ItemStack(item, 1);
                String nbt = com.cobblemania.economia.util.ItemStackUtil
                    .toNbtString(stack, server.getRegistryManager());
                int slot = findFreeSlot();
                ShopItem si = new ShopItem(slot, nbt, price);
                si.category = category;
                ModeconomiaConfig.DATA.shop.items.add(si);
                presentIds.add(itemId);
                added++;
            } catch (Exception ignored) {}
        }

        if (added > 0 || migrated) {
            ModeconomiaConfig.save();
            System.out.println("[CobbleMania] ✦ Tienda: +" + added + " items"
                + (migrated ? ", categorías actualizadas." : "."));
        }
    }

    private static int findFreeSlot() {
        Set<Integer> used = ModeconomiaConfig.DATA.shop.items.stream()
            .map(si -> si.slot).collect(Collectors.toSet());
        int max = used.isEmpty() ? -1 : used.stream().mapToInt(i -> i).max().getAsInt();
        for (int i = 0; i <= max + 1; i++) if (!used.contains(i)) return i;
        return max + 1;
    }
}
