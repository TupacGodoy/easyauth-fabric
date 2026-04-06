package com.cobblemania.economia.shop;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.data.ShopItem;
import com.cobblemania.economia.mission.MissionManager;
import com.cobblemania.economia.util.ItemStackUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public final class ShopManager {
    private ShopManager() {}

    public static List<ShopItem> getItems() {
        return ModeconomiaConfig.DATA.shop.items;
    }

    public static List<ShopItem> getListings() {
        return ModeconomiaConfig.DATA.shop.listings;
    }

    public static void buy(ServerPlayerEntity player, ShopItem item) {
        if (item == null) return;
        double price = item.price;
        if (!EconomyStorage.takeBalance(player.getUuid(), price)) {
            player.sendMessage(Text.literal(
                "§c✘ No tienes suficientes §6CobbleCoins§c. Necesitas §6"
                + MissionManager.format(price) + " CC§c."), false);
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ItemStack stack = ItemStackUtil.fromNbtString(item.itemNbt, server.getRegistryManager());
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("§c✘ Item no disponible en este momento."), false);
            EconomyStorage.addBalance(player.getUuid(), price);
            return;
        }
        if (!player.getInventory().insertStack(stack.copy())) {
            player.dropItem(stack.copy(), false);
        }
        player.sendMessage(Text.literal(
            "§a✔ Compraste §f" + stack.getName().getString()
            + " §apor §6" + MissionManager.format(price) + " CobbleCoins§a."), false);
        if (item.sellerUuid != null) {
            try {
                UUID seller = UUID.fromString(item.sellerUuid);
                EconomyStorage.addBalance(seller, price);
                // Notificar al vendedor si está online
                ServerPlayerEntity sellerPlayer = server.getPlayerManager().getPlayer(seller);
                if (sellerPlayer != null) {
                    sellerPlayer.sendMessage(Text.literal(
                        "§6§l✦ §eTu item §f" + stack.getName().getString()
                        + " §efue comprado por §a+" + MissionManager.format(price)
                        + " CobbleCoins§e."), false);
                }
            } catch (IllegalArgumentException ignored) {}
            removeListing(item);
        }
    }

    public static void addListing(ShopItem listing) {
        ModeconomiaConfig.DATA.shop.listings.add(listing);
        ModeconomiaConfig.save();
    }

    public static void removeListing(ShopItem listing) {
        if (ModeconomiaConfig.DATA.shop.listings.remove(listing)) {
            ModeconomiaConfig.save();
        }
    }

    public static void cleanupExpiredListings() {
        boolean removed = ModeconomiaConfig.DATA.shop.listings.removeIf(ShopItem::isExpired);
        if (removed) ModeconomiaConfig.save();
    }
}
