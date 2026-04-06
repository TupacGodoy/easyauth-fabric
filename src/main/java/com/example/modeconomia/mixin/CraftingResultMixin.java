package com.cobblemania.economia.mixin;

import com.cobblemania.economia.mission.MissionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detecta cuando el jugador toma un item crafteado del slot de resultado.
 * Yarn 1.21.1: CraftingResultSlot.onTakeItem(PlayerEntity, ItemStack): void
 */
@Mixin(net.minecraft.screen.slot.CraftingResultSlot.class)
public class CraftingResultMixin {

    @Inject(method = "onTakeItem", at = @At("HEAD"), require = 0)
    private void onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity sp) {
            MissionManager.onCraft(sp);
        }
    }
}
