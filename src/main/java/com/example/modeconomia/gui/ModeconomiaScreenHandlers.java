package com.cobblemania.economia.gui;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModeconomiaScreenHandlers {

    public static final ScreenHandlerType<MenuScreenHandler> MENU =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("cobblemania-economia", "menu"),
            new ExtendedScreenHandlerType<MenuScreenHandler, Integer>(
                (syncId, inventory, data) -> {
                    MenuKind kind = MenuKind.values()[data];
                    int size = switch (kind) {
                        case SHOP_VIEW, SHOP_CONFIG,
                             ECONOMY_PLAYER_SELECT, RANK_MEMBERS,
                             PLAYER_LIST, PLAYER_DETAIL, PLAYER_MENU -> 54;
                        default -> 27;
                    };
                    return new MenuScreenHandler(syncId, inventory, kind, size);
                },
                PacketCodecs.VAR_INT
            )
        );

    private ModeconomiaScreenHandlers() {}

    public static void init() {}
}
