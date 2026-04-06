package com.cobblemania.economia.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerMissionData {
    // Progreso diario de misiones
    public Map<String, Integer> progress  = new HashMap<>();
    public Set<String>          completed = new HashSet<>();

    // FIX 1: ID de la misión activa que el jugador eligió hacer
    // Solo puede tener UNA activa a la vez. null = ninguna seleccionada.
    public String activeMissionId = null;

    // Contadores históricos
    public int    totalDailyCompleted  = 0;
    public int    weeklyCompleted      = 0;
    public int    monthlyCompleted     = 0;
    public long   totalAfkMinutes      = 0;
    public double totalEarned          = 0.0;
    public String lastSeenDate         = "";
    public String playerName           = "";
}
