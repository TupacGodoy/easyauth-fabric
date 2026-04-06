package com.cobblemania.economia.shop;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Detects the category of a Cobblemon item from its registry ID.
 */
public final class CategoryDetector {

    private CategoryDetector() {}

    public static String detect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "general";
        String id = Registries.ITEM.getId(stack.getItem()).toString().toLowerCase();
        return detectFromId(id);
    }

    public static String detectFromId(String id) {
        if (!id.startsWith("cobblemon:")) return "general";

        // ── Pokébolas ──
        if (id.endsWith("_ball")) return "pokeballs";

        // ── Bayas ──
        if (id.endsWith("_berry")) return "bayas";

        // ── PP ──
        if (id.equals("cobblemon:pp_up") || id.equals("cobblemon:pp_max") ||
            id.equals("cobblemon:ether")  || id.equals("cobblemon:max_ether") ||
            id.equals("cobblemon:elixir") || id.equals("cobblemon:max_elixir"))
            return "pp";

        // ── Bonguris ──
        if (id.endsWith("_apricorn")) return "bonguris";

        // ── Brotes / Inciensos ──
        if (id.endsWith("_incense") || id.contains("incense")) return "brotes";

        // ── Caramelos Exp ──
        if (id.startsWith("cobblemon:exp_candy") || id.equals("cobblemon:rare_candy"))
            return "caramelos";

        // ── Mentas (Mints) ──
        if (id.endsWith("_mint")) return "mentas";

        // ── MTs / TMs ──
        // Cobblemon 1.7.3 uses cobblemon:technical_machine OR cobblemon:tm_<type>
        if (id.equals("cobblemon:technical_machine") ||
            id.startsWith("cobblemon:tm_") ||
            id.startsWith("cobblemon:hm_") ||
            id.contains("technical_machine"))
            return "tms";

        // ── Items de Batalla ──
        if (id.startsWith("cobblemon:x_") ||
            id.equals("cobblemon:dire_hit")     || id.equals("cobblemon:guard_spec") ||
            id.equals("cobblemon:full_heal")    || id.equals("cobblemon:full_restore") ||
            id.equals("cobblemon:max_restore")  || id.equals("cobblemon:hyper_potion") ||
            id.equals("cobblemon:super_potion") || id.equals("cobblemon:potion") ||
            id.equals("cobblemon:antidote")     || id.equals("cobblemon:awakening") ||
            id.equals("cobblemon:burn_heal")    || id.equals("cobblemon:ice_heal") ||
            id.equals("cobblemon:paralyze_heal")|| id.equals("cobblemon:revive") ||
            id.equals("cobblemon:max_revive"))
            return "batalla";

        // ── Gemas ──
        if (id.endsWith("_gem")) return "gemas";

        // ── Currys ──
        if (id.endsWith("_curry") || id.contains("curry")) return "currys";

        // ── Invocadores ──
        if (id.contains("_flute") || id.contains("_chain") ||
            id.contains("_bell")  || id.contains("_orb") ||
            id.equals("cobblemon:prison_bottle") || id.equals("cobblemon:reveal_glass") ||
            id.equals("cobblemon:rusted_sword")  || id.equals("cobblemon:rusted_shield") ||
            id.contains("lunarizer") || id.contains("solarizer"))
            return "invocadores";

        // ── Objetos de Evolución ──
        if (id.endsWith("_stone") || id.equals("cobblemon:linking_cord") ||
            id.equals("cobblemon:kings_rock")    || id.equals("cobblemon:metal_coat") ||
            id.equals("cobblemon:dragon_scale")  || id.equals("cobblemon:upgrade") ||
            id.equals("cobblemon:dubious_disc")  || id.equals("cobblemon:protector") ||
            id.equals("cobblemon:electirizer")   || id.equals("cobblemon:magmarizer") ||
            id.equals("cobblemon:razor_fang")    || id.equals("cobblemon:razor_claw") ||
            id.equals("cobblemon:oval_stone")    || id.equals("cobblemon:prism_scale") ||
            id.equals("cobblemon:deep_sea_tooth")|| id.equals("cobblemon:deep_sea_scale") ||
            id.equals("cobblemon:reaper_cloth")  || id.equals("cobblemon:sachet") ||
            id.equals("cobblemon:whipped_dream") || id.equals("cobblemon:cracked_pot") ||
            id.equals("cobblemon:chipped_pot")   || id.equals("cobblemon:sweet_apple") ||
            id.equals("cobblemon:tart_apple")    || id.equals("cobblemon:black_augurite") ||
            id.equals("cobblemon:peat_block")    || id.equals("cobblemon:auspicious_armor") ||
            id.equals("cobblemon:malicious_armor"))
            return "evoluciones";

        // ── Vitaminas y EV ──
        if (id.equals("cobblemon:hp_up")     || id.equals("cobblemon:protein") ||
            id.equals("cobblemon:iron")       || id.equals("cobblemon:calcium") ||
            id.equals("cobblemon:zinc")       || id.equals("cobblemon:carbos") ||
            id.endsWith("_feather")           || id.startsWith("cobblemon:power_") ||
            id.equals("cobblemon:macho_brace")|| id.equals("cobblemon:exp_share") ||
            id.equals("cobblemon:lucky_egg"))
            return "vitaminas";

        return "general";
    }
}
