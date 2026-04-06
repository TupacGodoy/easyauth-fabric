package com.cobblemania.economia.afk;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.data.rank.Rank;
import com.cobblemania.economia.mission.MissionManager;
import com.cobblemania.economia.rank.RankManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AfkManager {
    private static final Map<UUID, Long>    lastPayout  = new HashMap<>();
    private static final Map<UUID, Long>    entryTime   = new HashMap<>();  // FIX: track when player entered zone
    private static final Map<UUID, Boolean> inside      = new HashMap<>();

    private AfkManager() {}

    public static void tick(MinecraftServer server) {
        ModeconomiaConfig.AfkConfig config = ModeconomiaConfig.DATA.afk;
        if (config.world == null || config.world.isEmpty()) return;

        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(config.world));
        long now = System.currentTimeMillis();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            boolean sameWorld = player.getServerWorld().getRegistryKey().equals(key);
            if (!sameWorld) {
                setInside(player, false);
                continue;
            }

            // FIX 2: Circular zone — only check X/Z distance from center, ignore Y
            // The zone is defined by pos1 and pos2; we use the horizontal span as the radius
            Vec3d pos = player.getPos();
            boolean isInside = isInCircularZone(config, pos);
            boolean wasInside = inside.getOrDefault(player.getUuid(), false);

            if (isInside && !wasInside) {
                entryTime.put(player.getUuid(), now);  // FIX: record when player entered
                player.sendMessage(Text.literal(
                    "§a✔ Entraste a la §e§lZona AFK§a. ¡Comenzarás a ganar §6CobbleCoins§a!"), false);
            }
            if (!isInside && wasInside) {
                player.sendMessage(Text.literal(
                    "§7✦ Saliste de la §e§lZona AFK§7."), false);
            }
            setInside(player, isInside);
            if (!isInside) continue;

            long last       = lastPayout.getOrDefault(player.getUuid(),
                                entryTime.getOrDefault(player.getUuid(), now));
            long intervalMs = config.intervalMinutes * 60L * 1000L;
            if (now - last >= intervalMs) {
                double reward = config.baseReward;
                Rank rank = RankManager.getRank(player);
                Double multiplier = config.multipliers.get(rank);
                if (multiplier != null) reward *= multiplier;
                EconomyStorage.addBalance(player.getUuid(), reward);
                EconomyStorage.addAfkMinutes(player.getUuid(), config.intervalMinutes);
                player.sendMessage(Text.literal(
                    "§6§l✦ §eZona AFK: §a+" + MissionManager.format(reward)
                    + " §6CobbleCoins§e recibidos."), false);
                lastPayout.put(player.getUuid(), now);
            }
        }
    }

    /**
     * Circular zone detection using only X/Z plane.
     * Center = pos1 (where the admin stood when configuring).
     * Radius = distance from pos1 to pos2 (if pos2 is set and different from pos1).
     * If pos2 is not configured, uses a default radius of 15 blocks.
     */
    private static boolean isInCircularZone(ModeconomiaConfig.AfkConfig cfg, Vec3d playerPos) {
        // Center is pos1 — where the admin stood
        double centerX = cfg.pos1X;
        double centerZ = cfg.pos1Z;

        // Radius: distance from pos1 to pos2, or 15 if pos2 not set
        double radius = 15.0;
        if (cfg.pos2X != 0 || cfg.pos2Z != 0) {
            double dx = cfg.pos2X - cfg.pos1X;
            double dz = cfg.pos2Z - cfg.pos1Z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 1.0) radius = dist; // use actual distance if meaningful
        }

        double dx = playerPos.x - centerX;
        double dz = playerPos.z - centerZ;
        return (dx * dx + dz * dz) <= (radius * radius);
    }

    public static BlockPos getCenter() {
        ModeconomiaConfig.AfkConfig config = ModeconomiaConfig.DATA.afk;
        return new BlockPos(config.pos1X, config.pos1Y, config.pos1Z);
    }

    private static void setInside(ServerPlayerEntity player, boolean value) {
        inside.put(player.getUuid(), value);
        if (!value) {
            lastPayout.remove(player.getUuid());
            entryTime.remove(player.getUuid());
        }
    }

    public static void forceCheck(ServerPlayerEntity player) {
        inside.remove(player.getUuid());
        lastPayout.remove(player.getUuid());
        entryTime.remove(player.getUuid());
    }
}
