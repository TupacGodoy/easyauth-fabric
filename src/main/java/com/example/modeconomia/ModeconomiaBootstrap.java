package com.cobblemania.economia;

import com.cobblemania.economia.command.ModeconomiaCommands;
import com.cobblemania.economia.event.ChatInputListener;
import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.event.ModeconomiaEvents;
import com.cobblemania.economia.gui.ModeconomiaScreenHandlers;
import com.cobblemania.economia.item.ModItems;
import com.cobblemania.economia.main.CobbleMania;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class ModeconomiaBootstrap {
    private ModeconomiaBootstrap() {}

    public static void initialize() {
        ModItems.register();          // registrar CobbleCoin item
        ModeconomiaConfig.load();
        EconomyStorage.load();
        ModeconomiaScreenHandlers.init();
        ModeconomiaCommands.register();
        ModeconomiaEvents.register();
        ChatInputListener.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            com.cobblemania.economia.shop.ShopDefaults.populate(server);
            CobbleMania.LOGGER.info("✦ CobbleMania Economia — Inicializado correctamente.");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            EconomyStorage.save();
            ModeconomiaConfig.save();
        });
    }
}
