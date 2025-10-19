package com.example.generatormod.generator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GeneratorItems {
    public static final double SPEED_REDUCTION_PER_LEVEL = 0.85D;
    private static final int DEFAULT_UNLOCK_COST = 4;
    private static final ItemConfig DEFAULT_CONFIG = new ItemConfig(minutes(5), DEFAULT_UNLOCK_COST);
    private static final Map<Item, ItemConfig> ITEM_CONFIGS = new LinkedHashMap<>();

    public record ItemConfig(long interval, int unlockCost) {
    }

    static {
        // Common building materials (abundant resources demand higher unlock bundles)
        register(Items.COBBLESTONE, seconds(30), 128);
        register(Items.STONE, seconds(45), 112);
        register(Items.OAK_LOG, seconds(45), 96);
        register(Items.SPRUCE_LOG, seconds(45), 96);
        register(Items.BIRCH_LOG, seconds(45), 96);
        register(Items.JUNGLE_LOG, seconds(45), 96);
        register(Items.ACACIA_LOG, seconds(45), 96);
        register(Items.DARK_OAK_LOG, seconds(45), 96);
        register(Items.MANGROVE_LOG, seconds(45), 96);
        register(Items.CHERRY_LOG, seconds(45), 96);
        register(Items.BAMBOO, seconds(30), 80);

        // Overworld minerals and metals (unlock costs scale with acquisition difficulty)
        register(Items.COAL, minutes(1), 96);
        register(Items.LAPIS_LAZULI, minutes(2), 72);
        register(Items.REDSTONE, minutes(2) + seconds(30), 88);
        register(Items.COPPER_INGOT, minutes(2) + seconds(30), 88);
        register(Items.IRON_INGOT, minutes(3), 72);
        register(Items.GOLD_INGOT, minutes(4), 64);
        register(Items.AMETHYST_SHARD, minutes(5), 56);
        register(Items.EMERALD, minutes(6), 40);
        register(Items.DIAMOND, minutes(8), 32);

        // Nether resources (mix of farmable and mob drops)
        register(Items.NETHERRACK, minutes(1), 96);
        register(Items.QUARTZ, minutes(2), 72);
        register(Items.GLOWSTONE_DUST, minutes(3) + seconds(30), 64);
        register(Items.MAGMA_CREAM, minutes(5), 48);
        register(Items.BLAZE_ROD, minutes(6) + seconds(30), 40);
        register(Items.ANCIENT_DEBRIS, minutes(20), 8); // Rare ore: long cooldown keeps it prestigious.
        register(Items.NETHERITE_SCRAP, minutes(25), 6); // Processed debris remains intentionally slower than ingots.
        register(Items.NETHERITE_INGOT, minutes(30), 4); // Crafted alloy - longest among resource tiers.

        // The End and other exotic drops (rarer trophies ask for fewer unlock items)
        register(Items.END_STONE, minutes(2), 72);
        register(Items.CHORUS_FRUIT, minutes(3), 64);
        register(Items.SHULKER_SHELL, minutes(12), 16); // Shells are farm-limited; slow cadence protects progression pacing.
        register(Items.DRAGON_BREATH, minutes(15), 12); // Bottled dragon breath is semi-renewable but intentionally scarce.
        register(Items.TOTEM_OF_UNDYING, minutes(20), 8); // Raid reward emulates raid effort before automation.
        register(Items.NETHER_STAR, minutes(40), 4); // Boss trophy: extremely long interval reflects wither fight rarity.
    }

    private GeneratorItems() {
    }

    private static void register(Item item, long interval, int unlockCost) {
        ITEM_CONFIGS.put(item, new ItemConfig(interval, unlockCost));
    }

    private static long minutes(int value) {
        return seconds(value * 60L);
    }

    private static long seconds(long value) {
        return value * 1000L;
    }

    public static List<Item> getAvailableItems() {
        return Collections.unmodifiableList(ITEM_CONFIGS.keySet().stream().toList());
    }

    public static long getBaseInterval(Item item) {
        return ITEM_CONFIGS.getOrDefault(item, DEFAULT_CONFIG).interval();
    }

    public static int getUnlockCost(Item item) {
        return ITEM_CONFIGS.getOrDefault(item, DEFAULT_CONFIG).unlockCost();
    }

    public static int getUnlockCost(ResourceLocation id) {
        if (id == null) {
            return DEFAULT_CONFIG.unlockCost();
        }
        return resolve(id).map(GeneratorItems::getUnlockCost).orElse(DEFAULT_CONFIG.unlockCost());
    }

    public static long computeIntervalMillis(Item item, int speedLevel) {
        long base = getBaseInterval(item);
        double factor = Math.pow(SPEED_REDUCTION_PER_LEVEL, Math.max(0, speedLevel));
        long computed = Math.max(1000L, Math.round(base * factor));
        return computed;
    }

    public static int computeAmountPerCycle(int quantityLevel) {
        return Math.max(1, 1 + quantityLevel);
    }

    public static Optional<Item> resolve(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BuiltInRegistries.ITEM.get(id));
    }

    public static ResourceLocation getKey(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
