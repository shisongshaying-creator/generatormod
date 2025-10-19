package com.example.generatormod.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ClientGeneratorState {
    public static final ClientGeneratorState INSTANCE = new ClientGeneratorState();

    private ResourceLocation selectedItem;
    private boolean running;
    private long storedItems;
    private int speedLevel;
    private int quantityLevel;
    private long intervalMillis;
    private int amountPerCycle;
    private long runningSince;
    private long lastUpdate;
    private long leftoverMillis;
    private String message = "";
    private final Set<ResourceLocation> unlockedItems = new HashSet<>();
    private final Map<ResourceLocation, Integer> speedLevels = new HashMap<>();
    private final Map<ResourceLocation, Integer> quantityLevels = new HashMap<>();
    private final Map<ResourceLocation, Long> cloudStorage = new HashMap<>();

    private ClientGeneratorState() {
    }

    public void apply(ResourceLocation selectedItem, boolean running, long storedItems, int speedLevel, int quantityLevel,
                      long intervalMillis, int amountPerCycle, long runningSince, long lastUpdate, long leftoverMillis,
                      Set<ResourceLocation> unlockedItems, Map<ResourceLocation, Integer> speedLevels,
                      Map<ResourceLocation, Integer> quantityLevels, String message,
                      Map<ResourceLocation, Long> cloudStorage) {
        this.selectedItem = selectedItem;
        this.running = running;
        this.storedItems = storedItems;
        this.speedLevel = speedLevel;
        this.quantityLevel = quantityLevel;
        this.intervalMillis = intervalMillis;
        this.amountPerCycle = amountPerCycle;
        this.runningSince = runningSince;
        this.lastUpdate = lastUpdate;
        this.leftoverMillis = leftoverMillis;
        this.unlockedItems.clear();
        this.unlockedItems.addAll(unlockedItems);
        this.speedLevels.clear();
        this.speedLevels.putAll(speedLevels);
        this.quantityLevels.clear();
        this.quantityLevels.putAll(quantityLevels);
        this.cloudStorage.clear();
        this.cloudStorage.putAll(cloudStorage);
        this.message = message == null ? "" : message;
    }

    public ResourceLocation getSelectedItem() {
        return selectedItem;
    }

    public boolean isRunning() {
        return running;
    }

    public long getStoredItems() {
        return storedItems;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public int getSpeedLevel(ResourceLocation id) {
        if (id == null) {
            return 0;
        }
        return speedLevels.getOrDefault(id, 0);
    }

    public int getQuantityLevel() {
        return quantityLevel;
    }

    public int getQuantityLevel(ResourceLocation id) {
        if (id == null) {
            return 0;
        }
        return quantityLevels.getOrDefault(id, 0);
    }

    public long getIntervalMillis() {
        return intervalMillis;
    }

    public int getAmountPerCycle() {
        return amountPerCycle;
    }

    public long getRunningSince() {
        return runningSince;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public long getLeftoverMillis() {
        return leftoverMillis;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUnlocked(ResourceLocation id) {
        return id != null && unlockedItems.contains(id);
    }

    public Set<ResourceLocation> getUnlockedItems() {
        return Collections.unmodifiableSet(unlockedItems);
    }

    public Map<ResourceLocation, Long> getCloudStorage() {
        return Collections.unmodifiableMap(cloudStorage);
    }

    public void updateCloudStorage(Map<ResourceLocation, Long> cloudStorage) {
        this.cloudStorage.clear();
        this.cloudStorage.putAll(cloudStorage);
    }

    public long getCloudStored(ResourceLocation id) {
        if (id == null) {
            return 0L;
        }
        return cloudStorage.getOrDefault(id, 0L);
    }

    public DisplayState computeDisplayState(long now) {
        long displayStored = this.storedItems;
        long displayElapsed = this.leftoverMillis;

        if (this.running && this.intervalMillis > 0L) {
            long elapsedSinceUpdate = Math.max(0L, now - this.lastUpdate);
            long totalElapsed = this.leftoverMillis + elapsedSinceUpdate;
            long cycles = totalElapsed / this.intervalMillis;
            displayElapsed = totalElapsed % this.intervalMillis;

            if (this.amountPerCycle > 0 && cycles > 0) {
                displayStored += cycles * (long) this.amountPerCycle;
            }
        }

        return new DisplayState(displayStored, displayElapsed);
    }

    public record DisplayState(long storedItems, long elapsedInCycleMillis) {
    }
}
