package com.cobblemania.economia.data;

public class MissionDefinition {
    public String id;
    public String displayName;
    public String description    = "";      // Texto visible para el jugador en el NPC
    public String filter1        = "";      // Filtro interno 1 (especie, tipo, ball, bioma, elo...)
    public String filter2        = "";      // Filtro interno 2 (segunda ball, rango, etc.)
    public MissionType type;
    public int    requiredAmount;
    public double reward         = 0.4;
    public long   expiresAt      = 0;
    public boolean active        = true;

    public MissionDefinition() {}

    public MissionDefinition(String id, String displayName, MissionType type, int requiredAmount) {
        this.id             = id;
        this.displayName    = displayName;
        this.type           = type;
        this.requiredAmount = requiredAmount;
    }

    public MissionDefinition(String id, String displayName, MissionType type, int requiredAmount, String filter1) {
        this(id, displayName, type, requiredAmount);
        this.filter1 = filter1 != null ? filter1 : "";
    }

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }

    public String timeRemaining() {
        if (expiresAt <= 0) return "Sin límite";
        long ms = expiresAt - System.currentTimeMillis();
        if (ms <= 0) return "§cEXPIRADA";
        long mins  = ms / 60_000;
        long hours = mins / 60;
        long days  = hours / 24;
        if (days > 0)  return "§a" + days + "d " + (hours % 24) + "h";
        if (hours > 0) return "§e" + hours + "h " + (mins % 60) + "m";
        return "§6" + mins + " min";
    }
}
