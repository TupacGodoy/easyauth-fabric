package com.cobblemania.economia.data;

public class ShopItem {
	public int slot;
	public String itemNbt;
	public double price;
	public String sellerUuid;
	public long expiresAt;
	public int durationMinutes;
	public String category = "general"; // pokeballs, bayas, vitaminas, pp, bonguris, brotes, general

	public ShopItem() {
	}

	public ShopItem(int slot, String itemNbt, double price) {
		this.slot = slot;
		this.itemNbt = itemNbt;
		this.price = price;
	}

	public boolean isExpired() {
		return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
	}
}
