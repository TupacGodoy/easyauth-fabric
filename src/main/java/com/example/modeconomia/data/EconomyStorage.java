package com.cobblemania.economia.data;

import com.cobblemania.economia.main.CobbleMania;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EconomyStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "modeconomia-data.json";
    private static Path dataPath;
    private static EconomyData DATA = new EconomyData();

    private EconomyStorage() {}

    public static void load() {
        dataPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.exists(dataPath)) { save(); return; }
        try (Reader reader = Files.newBufferedReader(dataPath)) {
            EconomyData loaded = GSON.fromJson(reader, EconomyData.class);
            if (loaded != null) DATA = loaded;
            if (DATA.balances == null) DATA.balances = new HashMap<>();
            if (DATA.missions  == null) DATA.missions  = new HashMap<>();
        } catch (IOException | JsonParseException e) {
            CobbleMania.LOGGER.error("Failed to load Modeconomia data.", e);
        }
    }

    public static void save() {
        if (dataPath == null)
            dataPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(dataPath)) {
            GSON.toJson(DATA, writer);
        } catch (IOException e) {
            CobbleMania.LOGGER.error("Failed to save Modeconomia data.", e);
        }
    }

    // ── Balance ──
    public static double getBalance(UUID uuid) {
        return DATA.balances.getOrDefault(uuid.toString(), 0.0);
    }
    public static void setBalance(UUID uuid, double amount) {
        DATA.balances.put(uuid.toString(), round(amount));
        save();
    }
    public static void addBalance(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
        // Track total earned
        PlayerMissionData d = getMissionData(uuid);
        if (amount > 0) { d.totalEarned += amount; save(); }
    }
    public static boolean takeBalance(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current + 1e-9 < amount) return false;
        setBalance(uuid, current - amount);
        return true;
    }

    // ── Missions ──
    public static PlayerMissionData getMissionData(UUID uuid) {
        return DATA.missions.computeIfAbsent(uuid.toString(), k -> new PlayerMissionData());
    }

    // Llamar cuando se completa una mision
    public static void recordMissionComplete(UUID uuid) {
        PlayerMissionData d = getMissionData(uuid);
        d.totalDailyCompleted++;
        d.weeklyCompleted++;
        d.monthlyCompleted++;
        save();
    }

    // Actualizar nombre cacheado del jugador
    public static void updatePlayerName(UUID uuid, String name) {
        PlayerMissionData d = getMissionData(uuid);
        d.playerName = name;
        d.lastSeenDate = LocalDate.now().toString();
        save();
    }

    // Añadir tiempo AFK
    public static void addAfkMinutes(UUID uuid, long minutes) {
        PlayerMissionData d = getMissionData(uuid);
        d.totalAfkMinutes += minutes;
        save();
    }

    // Lista de todos los UUIDs con datos
    public static List<String> getAllPlayerUuids() {
        return new ArrayList<>(DATA.missions.keySet());
    }

    // ── Resets ──
    public static void resetMissionsForAll() {
        for (PlayerMissionData data : DATA.missions.values()) {
            data.progress.clear();
            data.completed.clear();
            data.activeMissionId = null; // Critical: clear active mission on daily reset
        }
        DATA.lastMissionResetDate = LocalDate.now().toString();
        save();
    }

    public static void resetWeeklyForAll() {
        for (PlayerMissionData data : DATA.missions.values()) {
            data.weeklyCompleted = 0;
        }
        DATA.lastWeeklyResetDate = LocalDate.now().toString();
        save();
    }

    public static void resetMonthlyForAll() {
        for (PlayerMissionData data : DATA.missions.values()) {
            data.monthlyCompleted = 0;
        }
        DATA.lastMonthlyResetDate = LocalDate.now().toString();
        save();
    }

    public static boolean shouldResetMissions() {
        String today = LocalDate.now().toString();
        return DATA.lastMissionResetDate == null || !DATA.lastMissionResetDate.equals(today);
    }

    public static boolean shouldResetWeekly() {
        if (DATA.lastWeeklyResetDate == null) return true;
        LocalDate last = LocalDate.parse(DATA.lastWeeklyResetDate);
        LocalDate today = LocalDate.now();
        // Reset cada lunes
        return today.getDayOfWeek() == DayOfWeek.MONDAY && !last.equals(today);
    }

    public static boolean shouldResetMonthly() {
        if (DATA.lastMonthlyResetDate == null) return true;
        LocalDate last = LocalDate.parse(DATA.lastMonthlyResetDate);
        LocalDate today = LocalDate.now();
        // Reset el dia 1 de cada mes
        return today.getDayOfMonth() == 1 && last.getMonth() != today.getMonth();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class EconomyData {
        public Map<String, Double>          balances             = new HashMap<>();
        public Map<String, PlayerMissionData> missions           = new HashMap<>();
        public String lastMissionResetDate;
        public String lastWeeklyResetDate;
        public String lastMonthlyResetDate;
    }
}
