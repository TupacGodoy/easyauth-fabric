package com.cobblemania.economia.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;

public final class ItemStackUtil {
	private ItemStackUtil() {
	}

	public static String toNbtString(ItemStack stack, RegistryWrapper.WrapperLookup lookup) {
		RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
		NbtElement element = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(new NbtCompound());
		NbtCompound wrapper = new NbtCompound();
		wrapper.put("stack", element);
		return wrapper.toString();
	}

	public static ItemStack fromNbtString(String nbtString, RegistryWrapper.WrapperLookup lookup) {
		try {
			NbtCompound wrapper = StringNbtReader.parse(nbtString);
			NbtElement element = wrapper.get("stack");
			if (element == null) {
				return ItemStack.EMPTY;
			}
			RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
			return ItemStack.CODEC.parse(ops, element).result().orElse(ItemStack.EMPTY);
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}
}
