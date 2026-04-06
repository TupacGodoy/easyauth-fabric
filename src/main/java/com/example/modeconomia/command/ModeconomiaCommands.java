package com.cobblemania.economia.command;

import com.cobblemania.economia.config.ModeconomiaConfig;
import com.cobblemania.economia.data.EconomyStorage;
import com.cobblemania.economia.data.ShopItem;
import com.cobblemania.economia.gui.EconomiaScreenHandler;
import com.cobblemania.economia.mission.MissionManager;
import com.cobblemania.economia.shop.ShopManager;
import com.cobblemania.economia.util.ItemStackUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

public final class ModeconomiaCommands {
    private ModeconomiaCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // ── /ccoins — GUI del jugador ──
            dispatcher.register(CommandManager.literal("ccoins")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    EconomiaScreenHandler.openPlayerMenu(player);
                    return 1;
                })
                // /ccoins owner — panel admin (solo OP 4)
                .then(CommandManager.literal("owner")
                    .requires(src -> src.hasPermissionLevel(4))
                    .executes(ctx -> openAdmin(ctx.getSource().getPlayer())))
            );

            // ── /pay <jugador> <monto> — para todos ──
            dispatcher.register(CommandManager.literal("pay")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> {
                            ServerPlayerEntity sender = ctx.getSource().getPlayer();
                            if (sender == null) {
                                ctx.getSource().sendError(Text.literal("§c✘ Solo jugadores pueden usar /pay."));
                                return 0;
                            }
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                            double amount = DoubleArgumentType.getDouble(ctx, "amount");
                            if (sender.getUuid().equals(target.getUuid())) {
                                sender.sendMessage(Text.literal("§c✘ No puedes pagarte a ti mismo."), false);
                                return 1;
                            }
                            if (!EconomyStorage.takeBalance(sender.getUuid(), amount)) {
                                sender.sendMessage(Text.literal(
                                    "§c✘ No tienes suficientes §6CobbleCoins§c. Necesitas §6"
                                    + MissionManager.format(amount) + " CC§c."), false);
                                return 1;
                            }
                            EconomyStorage.addBalance(target.getUuid(), amount);
                            sender.sendMessage(Text.literal(
                                "§a✔ Pagaste §6" + MissionManager.format(amount)
                                + " CC §aa §e" + target.getName().getString() + "§a."), false);
                            target.sendMessage(Text.literal(
                                "§a✔ Recibiste §6" + MissionManager.format(amount)
                                + " CC §ade §e" + sender.getName().getString() + "§a."), false);
                            return 1;
                        }))));

            // ── /zonafk — ir a zona AFK (renombrado para evitar conflicto con plugin AFK del sv) ──
            dispatcher.register(CommandManager.literal("zonafk")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (ModeconomiaConfig.DATA.afk.world == null) {
                        player.sendMessage(Text.literal("§c✘ La zona AFK no está configurada aún."), false);
                        return 1;
                    }
                    BlockTeleportUtil.teleportToAfk(player);
                    return 1;
                }));

            // ── /tienda — teleportar al NPC Tienda ──
            dispatcher.register(CommandManager.literal("tienda")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (ModeconomiaConfig.DATA.shopNpcUuid == null) {
                        // Sin NPC configurado → abrir GUI directamente
                        EconomiaScreenHandler.openShopSelector(player);
                        return 1;
                    }
                    BlockTeleportUtil.teleportToNpc(player,
                        ModeconomiaConfig.DATA.shopNpcUuid,
                        ModeconomiaConfig.DATA.shopNpcWorld,
                        ModeconomiaConfig.DATA.shopNpcX,
                        ModeconomiaConfig.DATA.shopNpcY,
                        ModeconomiaConfig.DATA.shopNpcZ,
                        "§b§lTienda");
                    return 1;
                }));

            // ── /misiones — teleportar al NPC Misiones ──
            dispatcher.register(CommandManager.literal("misiones")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (ModeconomiaConfig.DATA.questNpcUuid == null) {
                        EconomiaScreenHandler.openMissionsView(player);
                        return 1;
                    }
                    BlockTeleportUtil.teleportToNpc(player,
                        ModeconomiaConfig.DATA.questNpcUuid,
                        ModeconomiaConfig.DATA.questNpcWorld,
                        ModeconomiaConfig.DATA.questNpcX,
                        ModeconomiaConfig.DATA.questNpcY,
                        ModeconomiaConfig.DATA.questNpcZ,
                        "§6§lMisiones");
                    return 1;
                }));

            // ── /shopitem — administrar items de la tienda con tags (solo OP 4) ──
            dispatcher.register(CommandManager.literal("shopitem")
                .requires(src -> src.hasPermissionLevel(4))
                // /shopitem add <categoria> <precio> [duracion] — agrega item en la mano
                .then(CommandManager.literal("add")
                    .then(CommandManager.argument("category", StringArgumentType.word())
                        .then(CommandManager.argument("price", DoubleArgumentType.doubleArg(0.01))
                            .then(CommandManager.argument("duration", IntegerArgumentType.integer(1, 10080))
                                .executes(ctx -> shopItemAdd(
                                    ctx.getSource().getPlayer(),
                                    StringArgumentType.getString(ctx, "category"),
                                    DoubleArgumentType.getDouble(ctx, "price"),
                                    IntegerArgumentType.getInteger(ctx, "duration")
                                )))
                            .executes(ctx -> shopItemAdd(
                                ctx.getSource().getPlayer(),
                                StringArgumentType.getString(ctx, "category"),
                                DoubleArgumentType.getDouble(ctx, "price"),
                                60
                            ))))
                    .executes(ctx -> shopItemAdd(
                        ctx.getSource().getPlayer(),
                        "general",
                        1.0,
                        60
                    )))
                // /shopitem remove <slot> — elimina item por slot
                .then(CommandManager.literal("remove")
                    .then(CommandManager.argument("slot", IntegerArgumentType.integer(0, 10000))
                        .executes(ctx -> shopItemRemove(
                            ctx.getSource().getPlayer(),
                            IntegerArgumentType.getInteger(ctx, "slot")
                        ))))
                // /shopitem list [categoria] — lista items
                .then(CommandManager.literal("list")
                    .then(CommandManager.argument("category", StringArgumentType.word())
                        .executes(ctx -> shopItemList(
                            ctx.getSource().getPlayer(),
                            StringArgumentType.getString(ctx, "category")
                        )))
                    .executes(ctx -> shopItemList(ctx.getSource().getPlayer(), null)))
                // /shopitem price <slot> <nuevo_precio>
                .then(CommandManager.literal("price")
                    .then(CommandManager.argument("slot", IntegerArgumentType.integer(0, 10000))
                        .then(CommandManager.argument("price", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> shopItemPrice(
                                ctx.getSource().getPlayer(),
                                IntegerArgumentType.getInteger(ctx, "slot"),
                                DoubleArgumentType.getDouble(ctx, "price")
                            )))))
                // /shopitem clear — limpia toda la tienda
                .then(CommandManager.literal("clear")
                    .executes(ctx -> shopItemClear(ctx.getSource().getPlayer())))
                // /shopitem defaults — agrega items default de Cobblemon
                .then(CommandManager.literal("defaults")
                    .executes(ctx -> shopItemDefaults(ctx.getSource().getPlayer())))
            );
        });
    }

    private static int openAdmin(ServerPlayerEntity player) {
        if (player == null) return 0;
        EconomiaScreenHandler.openAdminMain(player);
        return 1;
    }

    // ════════════════════════════════════════════════════
    // /shopitem — COMANDOS DE TIENDA CON TAGS
    // ════════════════════════════════════════════════════

    /**
     * /shopitem add [categoria] [precio] [duracion]
     * Agrega el item en la mano del jugador a la tienda.
     * Tags soportados en categoria: pokeballs, bayas, vitaminas, mentas, caramelos,
     * bonguris, brotes, evoluciones, hold_items, tms, gemas, z_cristales,
     * mega_piedras, raids, entrenamiento, crianza, materiales, fosiles,
     * shards, legendarios, tm_especiales, accesorios, varios, general
     */
    private static int shopItemAdd(ServerPlayerEntity player, String category, double price, int durationMinutes) {
        if (player == null) return 0;
        ItemStack hand = player.getMainHandStack();
        if (hand.isEmpty()) {
            player.sendMessage(Text.literal("§c✘ Debes sostener un item en la mano."), false);
            return 1;
        }

        // Validar categoría
        String validCat = normalizeCategory(category);
        if (validCat == null) {
            player.sendMessage(Text.literal("§c✘ Categoría inválida: §f" + category), false);
            player.sendMessage(Text.literal("§7Categorías válidas: §epokeballs, bayas, vitaminas, mentas, caramelos, bonguris, brotes, evoluciones, hold_items, tms, gemas, z_cristales, mega_piedras, raids, entrenamiento, crianza, materiales, fosiles, shards, legendarios, tm_especiales, accesorios, varios, general"), false);
            return 1;
        }

        // Encontrar slot libre
        int slot = findFreeShopSlot();
        String nbt = ItemStackUtil.toNbtString(hand.copy(), player.getServer().getRegistryManager());

        ShopItem newItem = new ShopItem();
        newItem.slot = slot;
        newItem.itemNbt = nbt;
        newItem.price = price;
        newItem.category = validCat;
        newItem.durationMinutes = durationMinutes;
        newItem.expiresAt = 0; // 0 = permanente (tienda del servidor)
        newItem.sellerUuid = null;

        ModeconomiaConfig.DATA.shop.items.add(newItem);
        ModeconomiaConfig.save();

        player.sendMessage(Text.literal("§a✔ Item agregado a la tienda:"), false);
        player.sendMessage(Text.literal("  §7• Item: §f" + hand.getName().getString()), false);
        player.sendMessage(Text.literal("  §7• Categoría: §e" + validCat), false);
        player.sendMessage(Text.literal("  §7• Precio: §6" + MissionManager.format(price) + " CC"), false);
        player.sendMessage(Text.literal("  §7• Duración: §e" + durationMinutes + " min §7(§fpermanente si es tienda del servidor§7)"), false);
        player.sendMessage(Text.literal("  §7• Slot: §f" + slot), false);

        return 1;
    }

    /**
     * /shopitem remove <slot>
     * Elimina un item de la tienda por su slot.
     */
    private static int shopItemRemove(ServerPlayerEntity player, int slot) {
        if (player == null) return 0;

        boolean removed = ModeconomiaConfig.DATA.shop.items.removeIf(item -> item.slot == slot);
        if (removed) {
            ModeconomiaConfig.save();
            player.sendMessage(Text.literal("§a✔ Item del slot §f" + slot + " §aeliminado."), false);
        } else {
            player.sendMessage(Text.literal("§c✘ No se encontró un item en el slot §f" + slot + "§c."), false);
        }
        return 1;
    }

    /**
     * /shopitem list [categoria]
     * Lista los items de la tienda, opcionalmente filtrados por categoría.
     */
    private static int shopItemList(ServerPlayerEntity player, String category) {
        if (player == null) return 0;

        java.util.List<ShopItem> items = new java.util.ArrayList<>(ModeconomiaConfig.DATA.shop.items);
        items.sort(java.util.Comparator.comparingInt(i -> i.slot));

        if (category != null) {
            String normalized = normalizeCategory(category);
            if (normalized != null) {
                items.removeIf(i -> !i.category.equals(normalized));
            }
        }

        if (items.isEmpty()) {
            player.sendMessage(Text.literal("§e✦ No hay items en la tienda" +
                (category != null ? " en categoría §f" + category : "") + "§e."), false);
            return 1;
        }

        player.sendMessage(Text.literal("§6§l✦ Items en la tienda §8(" + items.size() + " total):"), false);

        int maxDisplay = Math.min(items.size(), 15);
        for (int i = 0; i < maxDisplay; i++) {
            ShopItem item = items.get(i);
            ItemStack stack = ItemStackUtil.fromNbtString(item.itemNbt, player.getServer().getRegistryManager());
            String name = stack.isEmpty() ? "§7[Item desconocido]" : stack.getName().getString();
            player.sendMessage(Text.literal(
                "  §8" + (i+1) + "§7. §f" + name +
                " §8| Slot: §f" + item.slot +
                " §8| Precio: §6" + MissionManager.format(item.price) + " CC" +
                " §8| Cat: §e" + item.category
            ), false);
        }

        if (items.size() > maxDisplay) {
            player.sendMessage(Text.literal("  §7... y " + (items.size() - maxDisplay) + " más. Usa §e/shopitem list <categoria> §7para filtrar."), false);
        }

        return 1;
    }

    /**
     * /shopitem price <slot> <nuevo_precio>
     * Cambia el precio de un item existente.
     */
    private static int shopItemPrice(ServerPlayerEntity player, int slot, double newPrice) {
        if (player == null) return 0;

        for (ShopItem item : ModeconomiaConfig.DATA.shop.items) {
            if (item.slot == slot) {
                item.price = newPrice;
                ModeconomiaConfig.save();
                player.sendMessage(Text.literal("§a✔ Precio actualizado para el slot §f" + slot + "§a:"), false);
                player.sendMessage(Text.literal("  §7Nuevo precio: §6" + MissionManager.format(newPrice) + " CC"), false);
                return 1;
            }
        }

        player.sendMessage(Text.literal("§c✘ No se encontró un item en el slot §f" + slot + "§c."), false);
        return 1;
    }

    /**
     * /shopitem clear
     * Elimina todos los items de la tienda.
     */
    private static int shopItemClear(ServerPlayerEntity player) {
        if (player == null) return 0;

        ModeconomiaConfig.DATA.shop.items.clear();
        ModeconomiaConfig.save();

        player.sendMessage(Text.literal("§a✔ Tienda limpiada completamente."), false);
        return 1;
    }

    /**
     * /shopitem defaults
     * Agrega los items por defecto de Cobblemon.
     */
    private static int shopItemDefaults(ServerPlayerEntity player) {
        if (player == null) return 0;

        com.cobblemania.economia.shop.ShopDefaults.populate(player.getServer());
        player.sendMessage(Text.literal("§a✔ Items por defecto de Cobblemon agregados."), false);
        return 1;
    }

    /**
     * Normaliza el nombre de categoría a un valor válido.
     * Retorna null si la categoría no es válida.
     */
    private static String normalizeCategory(String category) {
        if (category == null) return "general";

        String lower = category.toLowerCase().replace(" ", "_").replace("-", "_");

        // Categorías válidas
        java.util.Set<String> valid = java.util.Set.of(
            "pokeballs", "bayas", "vitaminas", "mentas", "caramelos",
            "bonguris", "brotes", "evoluciones", "hold_items", "tms",
            "gemas", "z_cristales", "mega_piedras", "raids",
            "entrenamiento", "crianza", "materiales", "fosiles",
            "shards", "legendarios", "tm_especiales", "accesorios",
            "varios", "general", "pp", "batalla", "currys"
        );

        return valid.contains(lower) ? lower : null;
    }

    /**
     * Encuentra un slot libre para la tienda.
     */
    private static int findFreeShopSlot() {
        java.util.Set<Integer> used = ModeconomiaConfig.DATA.shop.items.stream()
            .map(i -> i.slot).collect(java.util.stream.Collectors.toSet());
        int max = used.isEmpty() ? -1 : used.stream().mapToInt(Integer::intValue).max().getAsInt();
        for (int i = 0; i <= max + 1; i++) {
            if (!used.contains(i)) return i;
        }
        return max + 1;
    }
}
