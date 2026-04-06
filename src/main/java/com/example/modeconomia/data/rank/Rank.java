package com.cobblemania.economia.data.rank;

public enum Rank {
    TRAINER("Trainer"),          // Rango base — todos los jugadores, sin multiplicador
    TRAINER_PLUS("Trainer+"),    // x1.5 misiones, x1.3 AFK
    ELITE("Elite"),              // x1.9 misiones, x1.8 AFK
    LEGENDARY("Legendary"),      // x2.4 misiones, x2.0 AFK
    MYTHICAL("Mythical");        // x2.9 misiones, x2.7 AFK

    private final String displayName;

    Rank(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
