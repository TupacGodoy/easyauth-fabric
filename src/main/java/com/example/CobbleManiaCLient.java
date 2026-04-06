package com.cobblemania.economia.main;

import com.cobblemania.economia.client.MenuScreen;
import com.cobblemania.economia.gui.MenuScreenHandler;
import com.cobblemania.economia.gui.ModeconomiaScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class CobbleManiaCLient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(
            ModeconomiaScreenHandlers.MENU,
            (MenuScreenHandler handler, PlayerInventory inventory, Text title) ->
                new MenuScreen(handler, inventory, title)
        );
    }
}
