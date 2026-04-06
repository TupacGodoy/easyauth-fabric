package com.cobblemania.economia.command;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.gui.EconomiaScreenHandler;
import com.cobblemania.economia.mission.MissionManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ModeconomiaCommands {
    private ModeconomiaCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // ── /ccoins — GUI del jugador ──
            dispatcher.register(CommandManager.literal("ccoins")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    EconomiaScreenHandler.openPlayerMenu(player);
                    return 1;
                })
                // /ccoins owner — panel admin (solo OP 4)
                .then(CommandManager.literal("owner")
                    .requires(src -> src.hasPermissionLevel(4))
                    .executes(ctx -> openAdmin(ctx.getSource().getPlayer())))
            );

            // ── /pay <jugador> <monto> — para todos ──
            dispatcher.register(CommandManager.literal("pay")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> {
                            ServerPlayerEntity sender = ctx.getSource().getPlayer();
                            if (sender == null) {
                                ctx.getSource().sendError(Text.literal("§c✘ Solo jugadores pueden usar /pay."));
                                return 0;
                            }
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                            double amount = DoubleArgumentType.getDouble(ctx, "amount");
                            if (sender.getUuid().equals(target.getUuid())) {
                                sender.sendMessage(Text.literal("§c✘ No puedes pagarte a ti mismo."), false);
                                return 1;
                            }
                            if (!EconomyStorage.takeBalance(sender.getUuid(), amount)) {
                                sender.sendMessage(Text.literal(
                                    "§c✘ No tienes suficientes §6CobbleCoins§c. Necesitas §6"
                                    + MissionManager.format(amount) + " CC§c."), false);
                                return 1;
                            }
                            EconomyStorage.addBalance(target.getUuid(), amount);
                            sender.sendMessage(Text.literal(
                                "§a✔ Pagaste §6" + MissionManager.format(amount)
                                + " CC §aa §e" + target.getName().getString() + "§a."), false);
                            target.sendMessage(Text.literal(
                                "§a✔ Recibiste §6" + MissionManager.format(amount)
                                + " CC §ade §e" + sender.getName().getString() + "§a."), false);
                            return 1;
                        }))));

            // ── /zonafk — ir a zona AFK (renombrado para evitar conflicto con plugin AFK del sv) ──
            dispatcher.register(CommandManager.literal("zonafk")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (ModeconomiaConfig.DATA.afk.world == null) {
                        player.sendMessage(Text.literal("§c✘ La zona AFK no está configurada aún."), false);
                        return 1;
                    }
                    BlockTeleportUtil.teleportToAfk(player);
                    return 1;
                }));

            // ── /tienda — teleportar al NPC Tienda ──
            dispatcher.register(CommandManager.literal("tienda")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (ModeconomiaConfig.DATA.shopNpcUuid == null) {
                        // Sin NPC configurado → abrir GUI directamente
                        EconomiaScreenHandler.openShopSelector(player);
                        return 1;
                    }
                    BlockTeleportUtil.teleportToNpc(player,
                        ModeconomiaConfig.DATA.shopNpcUuid,
                        ModeconomiaConfig.DATA.shopNpcWorld,
                        ModeconomiaConfig.DATA.shopNpcX,
                        ModeconomiaConfig.DATA.shopNpcY,
                        ModeconomiaConfig.DATA.shopNpcZ,
                        "§b§lTienda");
                    return 1;
                }));

            // ── /misiones — teleportar al NPC Misiones ──
            dispatcher.register(CommandManager.literal("misiones")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (ModeconomiaConfig.DATA.questNpcUuid == null) {
                        EconomiaScreenHandler.openMissionsView(player);
                        return 1;
                    }
                    BlockTeleportUtil.teleportToNpc(player,
                        ModeconomiaConfig.DATA.questNpcUuid,
                        ModeconomiaConfig.DATA.questNpcWorld,
                        ModeconomiaConfig.DATA.questNpcX,
                        ModeconomiaConfig.DATA.questNpcY,
                        ModeconomiaConfig.DATA.questNpcZ,
                        "§6§lMisiones");
                    return 1;
                }));
        });
    }

    private static int openAdmin(ServerPlayerEntity player) {
        if (player == null) return 0;
        EconomiaScreenHandler.openAdminMain(player);
        return 1;
    }
}
