package com.cobblemania.economia.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Item físico CobbleCoin.
 * 1 CobbleCoin item = 1.00 CC de balance.
 *
 * Canjeables via /ccoins → botón "Canjear" en el panel del jugador.
 * Se otorgan como recompensa de misiones si la config lo indica.
 */
public class CobbleCoinItem extends Item {

    public static final int CC_PER_ITEM = 1; // 1 item = 1.00 CC

    public CobbleCoinItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                               List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("§6✦ 1 CobbleCoin = 1.00 CC"));
        tooltip.add(Text.literal("§7Usa §e/ccoins §7→ Canjear para convertirlo."));
        tooltip.add(Text.literal("§8Se obtiene como recompensa de misiones."));
    }
}
