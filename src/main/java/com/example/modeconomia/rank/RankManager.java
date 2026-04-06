package com.cobblemania.economia.rank;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.rank.Rank;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RankManager {
    private RankManager() {}

    /**
     * Devuelve el rango del jugador.
     * TRAINER es el rango base que todos tienen — no se guarda en config,
     * simplemente se devuelve si el jugador no tiene ningún otro rango.
     */
    public static Rank getRank(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Map<Rank, List<String>> members = ModeconomiaConfig.DATA.ranks.rankMembers;
        if (members == null) return Rank.TRAINER;
        // Comprobar de mayor a menor
        for (Rank rank : new Rank[]{Rank.MYTHICAL, Rank.LEGENDARY, Rank.ELITE, Rank.TRAINER_PLUS}) {
            if (contains(members, rank, uuid)) return rank;
        }
        return Rank.TRAINER; // Rango base — no necesita estar guardado
    }

    /** Igual que getRank pero por UUID (puede ser offline) */
    public static Rank getRankByUuid(UUID uuid) {
        Map<Rank, List<String>> members = ModeconomiaConfig.DATA.ranks.rankMembers;
        if (members == null) return Rank.TRAINER;
        for (Rank rank : new Rank[]{Rank.MYTHICAL, Rank.LEGENDARY, Rank.ELITE, Rank.TRAINER_PLUS}) {
            if (contains(members, rank, uuid)) return rank;
        }
        return Rank.TRAINER;
    }

    public static boolean isInRank(Rank rank, UUID uuid) {
        if (rank == Rank.TRAINER) return true; // Todos son Trainer
        Map<Rank, List<String>> members = ModeconomiaConfig.DATA.ranks.rankMembers;
        return contains(members, rank, uuid);
    }

    public static void addToRank(Rank rank, UUID uuid) {
        if (rank == Rank.TRAINER) return; // No se guarda, es el default
        Map<Rank, List<String>> members = ModeconomiaConfig.DATA.ranks.rankMembers;
        if (members == null) return;
        // Primero remover de todos los rangos elevados
        for (Rank r : new Rank[]{Rank.TRAINER_PLUS, Rank.ELITE, Rank.LEGENDARY, Rank.MYTHICAL})
            removeFromRank(r, uuid);
        List<String> list = members.computeIfAbsent(rank, k -> new java.util.ArrayList<>());
        if (!list.contains(uuid.toString())) list.add(uuid.toString());
        ModeconomiaConfig.save();
    }

    public static void removeFromRank(Rank rank, UUID uuid) {
        if (rank == Rank.TRAINER) return;
        Map<Rank, List<String>> members = ModeconomiaConfig.DATA.ranks.rankMembers;
        if (members == null) return;
        List<String> list = members.get(rank);
        if (list != null) list.remove(uuid.toString());
        ModeconomiaConfig.save();
    }

    public static void removeFromAllRanks(UUID uuid) {
        Map<Rank, List<String>> members = ModeconomiaConfig.DATA.ranks.rankMembers;
        if (members == null) return;
        for (Rank rank : new Rank[]{Rank.TRAINER_PLUS, Rank.ELITE, Rank.LEGENDARY, Rank.MYTHICAL})
            removeFromRank(rank, uuid);
    }

    private static boolean contains(Map<Rank, List<String>> members, Rank rank, UUID uuid) {
        if (members == null) return false;
        List<String> list = members.get(rank);
        return list != null && list.contains(uuid.toString());
    }
}
