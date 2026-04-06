package com.cobblemania.economia.client;

import com.cobblemania.economia.gui.MenuScreenHandler;
import com.cobblemania.economia.gui.ModeconomiaScreenHandlers;
import com.cobblemania.economia.client.MenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.Text;

public class ModeconomiaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HandledScreens.register(ModeconomiaScreenHandlers.MENU,
			(MenuScreenHandler handler, net.minecraft.entity.player.PlayerInventory inventory, Text title) ->
				new MenuScreen(handler, inventory, title));
	}
}
