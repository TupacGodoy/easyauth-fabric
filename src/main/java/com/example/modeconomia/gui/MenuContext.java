package com.cobblemania.economia.gui;

import com.cobblemania.economia.data.rank.Rank;

import java.util.UUID;

public class MenuContext {
    public EconomyAction economyAction;
    public UUID targetPlayer;
    public double amount;
    public double shopPrice = 1.0;
    public int shopDurationMinutes = 60;
    public boolean shopSelling;
    public Rank selectedRank;
    public int selectedSlot = -1;
    // Paginacion para lista de jugadores
    public int page = 0;
    // UUID del jugador viendo detalle
    public String detailPlayerUuid;

    public enum EconomyAction {
        GIVE, TAKE, SET, VIEW
    }
}
