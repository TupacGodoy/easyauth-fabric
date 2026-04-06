package com.cobblemania.economia.event;

import com.cobblemania.economia.gui.EconomiaScreenHandler;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class ChatInputListener {

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!EconomiaScreenHandler.awaitingInput.containsKey(sender.getUuid())) {
                return true; // No está esperando input → dejar pasar
            }
            String text;
            try {
                text = message.getSignedContent();
            } catch (Exception e) {
                text = message.getContent().getString();
            }
            if (text == null || text.isBlank()) return true;
            final String finalText = text;
            sender.getServer().execute(() ->
                EconomiaScreenHandler.handleChatInput(sender, finalText));
            return false; // Cancelar mensaje público
        });
    }
}
