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
    private static final Map<Item, Long> BASE_INTERVALS = new LinkedHashMap<>();

    static {
        // Early-game building materials (quick unlocks for new players)
        BASE_INTERVALS.put(Items.COBBLESTONE, seconds(30));
        BASE_INTERVALS.put(Items.STONE, seconds(45));
        BASE_INTERVALS.put(Items.OAK_LOG, seconds(45));
        BASE_INTERVALS.put(Items.SPRUCE_LOG, seconds(45));
        BASE_INTERVALS.put(Items.BIRCH_LOG, seconds(45));
        BASE_INTERVALS.put(Items.JUNGLE_LOG, seconds(45));
        BASE_INTERVALS.put(Items.ACACIA_LOG, seconds(45));
        BASE_INTERVALS.put(Items.DARK_OAK_LOG, seconds(45));
        BASE_INTERVALS.put(Items.MANGROVE_LOG, seconds(45));
        BASE_INTERVALS.put(Items.CHERRY_LOG, seconds(45));
        BASE_INTERVALS.put(Items.BAMBOO, seconds(30));

        // Overworld minerals and metals (progressively slower per rarity)
        BASE_INTERVALS.put(Items.COAL, minutes(1));
        BASE_INTERVALS.put(Items.LAPIS_LAZULI, minutes(2));
        BASE_INTERVALS.put(Items.REDSTONE, minutes(2) + seconds(30));
        BASE_INTERVALS.put(Items.COPPER_INGOT, minutes(2) + seconds(30));
        BASE_INTERVALS.put(Items.IRON_INGOT, minutes(3));
        BASE_INTERVALS.put(Items.GOLD_INGOT, minutes(4));
        BASE_INTERVALS.put(Items.AMETHYST_SHARD, minutes(5));
        BASE_INTERVALS.put(Items.EMERALD, minutes(6));
        BASE_INTERVALS.put(Items.DIAMOND, minutes(8));

        // Nether resources (mix of farmable and mob drops)
        BASE_INTERVALS.put(Items.NETHERRACK, minutes(1));
        BASE_INTERVALS.put(Items.QUARTZ, minutes(2));
        BASE_INTERVALS.put(Items.GLOWSTONE_DUST, minutes(3) + seconds(30));
        BASE_INTERVALS.put(Items.MAGMA_CREAM, minutes(5));
        BASE_INTERVALS.put(Items.BLAZE_ROD, minutes(6) + seconds(30));
        BASE_INTERVALS.put(Items.ANCIENT_DEBRIS, minutes(20)); // Rare ore: long cooldown keeps it prestigious.
        BASE_INTERVALS.put(Items.NETHERITE_SCRAP, minutes(25)); // Processed debris remains intentionally slower than ingots.
        BASE_INTERVALS.put(Items.NETHERITE_INGOT, minutes(30)); // Crafted alloy - longest among resource tiers.

        // The End and other exotic drops (longer cadences preserve late-game pacing)
        BASE_INTERVALS.put(Items.END_STONE, minutes(2));
        BASE_INTERVALS.put(Items.CHORUS_FRUIT, minutes(3));
        BASE_INTERVALS.put(Items.SHULKER_SHELL, minutes(12)); // Shells are farm-limited; slow cadence protects progression pacing.
        BASE_INTERVALS.put(Items.DRAGON_BREATH, minutes(15)); // Bottled dragon breath is semi-renewable but intentionally scarce.
        BASE_INTERVALS.put(Items.TOTEM_OF_UNDYING, minutes(20)); // Raid reward emulates raid effort before automation.
        BASE_INTERVALS.put(Items.NETHER_STAR, minutes(40)); // Boss trophy: extremely long interval reflects wither fight rarity.
    }

    private GeneratorItems() {
    }

    private static long minutes(int value) {
        return seconds(value * 60L);
    }

    private static long seconds(long value) {
        return value * 1000L;
    }

    public static List<Item> getAvailableItems() {
        return Collections.unmodifiableList(BASE_INTERVALS.keySet().stream().toList());
    }

    public static long getBaseInterval(Item item) {
        return BASE_INTERVALS.getOrDefault(item, minutes(5));
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
