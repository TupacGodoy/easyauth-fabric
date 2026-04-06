package com.cobblemania.economia.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registro de items del mod.
 * Llamado desde el mod initializer.
 */
public final class ModItems {

    public static final Item COBBLE_COIN = Registry.register(
        Registries.ITEM,
        Identifier.of("cobblemania-economia", "cobblecoin"),
        new CobbleCoinItem(new Item.Settings()
            .maxCount(64)        // apilable hasta 64
        )
    );

    private ModItems() {}

    public static void register() {
        // Añadir al grupo de items de herramientas para que aparezca en modo creativo
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries ->
            entries.add(new ItemStack(COBBLE_COIN))
        );
    }
}
