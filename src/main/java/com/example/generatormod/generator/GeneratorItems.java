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
        BASE_INTERVALS.put(Items.COAL, minutes(1));
        BASE_INTERVALS.put(Items.IRON_INGOT, minutes(3));
        BASE_INTERVALS.put(Items.GOLD_INGOT, minutes(4));
        BASE_INTERVALS.put(Items.EMERALD, minutes(6));
        BASE_INTERVALS.put(Items.LAPIS_LAZULI, minutes(2));
        BASE_INTERVALS.put(Items.REDSTONE, minutes(2) + seconds(30));
        BASE_INTERVALS.put(Items.DIAMOND, minutes(8));
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
