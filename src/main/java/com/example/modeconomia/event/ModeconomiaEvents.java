package com.cobblemania.economia.event;

import com.cobblemania.economia.afk.AfkManager;
import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.data.MissionType;
import com.cobblemania.economia.gui.EconomiaScreenHandler;
import com.cobblemania.economia.mission.MissionManager;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.item.FishingRodItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

public final class ModeconomiaEvents {

    private static int tickCounter = 0;
    // Store server reference for use in Pokédex event
    public static MinecraftServer SERVER;

    private ModeconomiaEvents() {}

    public static void register() {

        // ── Store server reference ──
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SERVER = null);

        // ── JOIN ──
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            EconomyStorage.updatePlayerName(p.getUuid(), p.getName().getString());
            MissionManager.onPlayerJoin(p);
        });

        // ── BREAK_BLOCKS + BREAK_SPECIFIC_BLOCK ──
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity sp)) return;
            String blockId = net.minecraft.registry.Registries.BLOCK
                .getId(state.getBlock()).toString();
            MissionManager.onBlockBreak(sp, blockId);
        });

        // ── PLACE_BLOCKS — UseItemCallback with BlockItem ──
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!(player instanceof ServerPlayerEntity sp)) return TypedActionResult.pass(player.getStackInHand(hand));
            var stack = sp.getStackInHand(hand);
            if (stack.getItem() instanceof net.minecraft.item.BlockItem)
                MissionManager.onBlockPlace(sp);
            return TypedActionResult.pass(stack);
        });

        // ── FISH_ITEMS ──
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!(player instanceof ServerPlayerEntity sp)) return TypedActionResult.pass(player.getStackInHand(hand));
            var stack = sp.getStackInHand(hand);
            if (stack.getItem() instanceof FishingRodItem && sp.fishHook != null)
                MissionManager.onFish(sp);
            return TypedActionResult.pass(stack);
        });

        // ── CATCH_WITHOUT_DAMAGE — track player damage ──
        // Fabric 0.102.0+1.21.1: ServerLivingEntityEvents.ALLOW_DAMAGE
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && amount > 0) {
                MissionManager.onPlayerDamaged(player);
            }
            return true; // always allow damage — we just track it
        });

        // ── NPC Misionero ──
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (ModeconomiaConfig.DATA.questNpcUuid == null) return ActionResult.PASS;
            if (entity.getUuid().toString().equals(ModeconomiaConfig.DATA.questNpcUuid)) {
                EconomiaScreenHandler.openMissionsView(sp);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // ── NPC Tendero ──
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (ModeconomiaConfig.DATA.shopNpcUuid == null) return ActionResult.PASS;
            if (entity.getUuid().toString().equals(ModeconomiaConfig.DATA.shopNpcUuid)) {
                EconomiaScreenHandler.openShopSelector(sp);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // ── Cobblemon events ──
        CobblemonEventAdapter.register();

        // ── Tick (cada segundo) ──
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter % 20 != 0) return;
            MissionManager.tick(server);
            AfkManager.tick(server);
        });
    }
}
