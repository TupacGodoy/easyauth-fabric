package com.cobblemania.economia.command;

import com.cobblemania.economia.afk.AfkManager;
import com.cobblemania.economia.config.ModeconomiaConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

public final class BlockTeleportUtil {
    private BlockTeleportUtil() {}

    public static void teleportToAfk(ServerPlayerEntity player) {
        String worldId = ModeconomiaConfig.DATA.afk.world;
        if (worldId == null || worldId.isEmpty()) {
            player.sendMessage(Text.literal("§c✘ La zona AFK no está configurada."), false);
            return;
        }
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
        ServerWorld world = player.getServer().getWorld(key);
        if (world == null) {
            player.sendMessage(Text.literal("§c✘ El mundo de la zona AFK no existe: §f" + worldId), false);
            return;
        }
        ModeconomiaConfig.AfkConfig cfg = ModeconomiaConfig.DATA.afk;
        double destX = cfg.pos1X + 0.5;
        double destY = cfg.pos1Y;
        double destZ = cfg.pos1Z + 0.5;
        AfkManager.forceCheck(player);
        player.teleport(world, destX, destY, destZ, player.getYaw(), player.getPitch());
        player.sendMessage(Text.literal("§a✔ Teleportado a la §e§lZona AFK§a."), false);
    }

    /**
     * Teleporta al jugador junto al NPC guardado (tienda o misiones).
     * Si el mundo o las coordenadas no están guardadas, abre el GUI directamente.
     */
    public static void teleportToNpc(ServerPlayerEntity player,
                                     String npcUuid,
                                     String worldId,
                                     double npcX, double npcY, double npcZ,
                                     String npcLabel) {
        if (worldId == null || worldId.isEmpty()) {
            player.sendMessage(Text.literal("§c✘ La posición del NPC " + npcLabel + " §cno está registrada aún."), false);
            return;
        }
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
        ServerWorld world = player.getServer().getWorld(key);
        if (world == null) {
            player.sendMessage(Text.literal("§c✘ El mundo del NPC no existe: §f" + worldId), false);
            return;
        }
        // Teletransportar 1 bloque al frente para que quede mirando al NPC
        player.teleport(world, npcX + 1.5, npcY, npcZ + 0.5, player.getYaw(), player.getPitch());
        player.sendMessage(Text.literal("§a✔ Teleportado al NPC " + npcLabel + "§a."), false);
    }
}
