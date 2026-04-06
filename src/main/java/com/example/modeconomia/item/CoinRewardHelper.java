package com.cobblemania.economia.item;

import com.cobblemania.economia.data.EconomyStorage;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Helper para dar y canjear CobbleCoins físicos.
 *
 * DAR:    giveCoins(player, amount)   → da items al jugador
 * CANJEAR: redeemCoins(player)        → convierte todos los items del inventario a balance CC
 */
public final class CoinRewardHelper {

    private CoinRewardHelper() {}

    /**
     * Da monedas físicas al jugador como recompensa.
     * Si el inventario está lleno, las suelta al piso.
     */
    public static void giveCoins(ServerPlayerEntity player, int amount) {
        if (amount <= 0) return;
        int remaining = amount;

        while (remaining > 0) {
            int stackSize = Math.min(remaining, 64);
            ItemStack stack = new ItemStack(ModItems.COBBLE_COIN, stackSize);

            if (!player.getInventory().insertStack(stack)) {
                // Inventario lleno — dropar al mundo
                player.dropItem(new ItemStack(ModItems.COBBLE_COIN, stackSize), false);
                player.sendMessage(Text.literal(
                    "§e⚠ §7Inventario lleno — §e" + stackSize + " CobbleCoin(s) §7dropeadas al suelo."), false);
            }
            remaining -= stackSize;
        }
    }

    /**
     * Canjea TODAS las CobbleCoins físicas del inventario del jugador.
     * Las elimina del inventario y las suma al balance CC.
     * Retorna la cantidad canjeada (0 si no tenía ninguna).
     */
    public static int redeemCoins(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        int totalCoins = 0;

        // Contar e ir eliminando todas las monedas del inventario
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == ModItems.COBBLE_COIN) {
                totalCoins += stack.getCount();
                inventory.removeStack(i);
            }
        }

        if (totalCoins <= 0) return 0;

        // Convertir: 1 moneda = 1.00 CC
        double ccAmount = totalCoins * CobbleCoinItem.CC_PER_ITEM;
        EconomyStorage.addBalance(player.getUuid(), ccAmount);

        player.sendMessage(Text.literal(
            "§6§l✦ §aCanje exitoso: §e" + totalCoins + " CobbleCoin(s) §a→ §6+" +
            String.format("%.2f", ccAmount) + " CC"), false);
        player.sendMessage(Text.literal(
            "§7Nuevo balance: §6" +
            String.format("%.2f", EconomyStorage.getBalance(player.getUuid())) + " CC"), false);

        return totalCoins;
    }

    /**
     * ¿Cuántas CobbleCoins físicas tiene el jugador en inventario?
     */
    public static int countCoins(ServerPlayerEntity player) {
        int total = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == ModItems.COBBLE_COIN) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
