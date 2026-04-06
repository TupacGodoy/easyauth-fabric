package com.cobblemania.economia.client;

import com.cobblemania.economia.gui.MenuScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class MenuScreen extends HandledScreen<MenuScreenHandler> {
	private static final Identifier TEXTURE = Identifier.of("textures/gui/container/generic_54.png");
	private final int rows;

	public MenuScreen(MenuScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.rows = handler.getMenuRows();
		this.backgroundHeight = 114 + this.rows * 18;
		this.playerInventoryTitleY = this.backgroundHeight - 94;
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int x = (this.width - this.backgroundWidth) / 2;
		int y = (this.height - this.backgroundHeight) / 2;
		context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.rows * 18 + 17);
		context.drawTexture(TEXTURE, x, y + this.rows * 18 + 17, 0, 126, this.backgroundWidth, 96);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
		if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
			ItemStack stack = this.focusedSlot.getStack();
			int slotIndex = this.handler.slots.indexOf(this.focusedSlot);
			boolean isMenuSlot = slotIndex >= 0 && slotIndex < this.handler.getMenuRows() * 9;

			if (isMenuSlot) {
				// Construir tooltip completo: nombre + lore del item
				List<Text> tooltip = new ArrayList<>();

				// 1. Nombre del item (custom name)
				Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
				if (name != null) {
					tooltip.add(name);
				} else {
					tooltip.add(stack.getName());
				}

				// 2. Lore del item (nuestras descripciones)
				LoreComponent lore = stack.get(DataComponentTypes.LORE);
				if (lore != null && !lore.lines().isEmpty()) {
					tooltip.addAll(lore.lines());
				}

				// 3. Extra lines del handler (si hay)
				List<Text> extra = this.handler.getTooltipLines(slotIndex);
				if (!extra.isEmpty()) {
					tooltip.addAll(extra);
				}

				context.drawTooltip(this.textRenderer, tooltip, x, y);
				return;
			}
		}
		super.drawMouseoverTooltip(context, x, y);
	}
}
