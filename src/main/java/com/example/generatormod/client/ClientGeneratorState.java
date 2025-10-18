package com.example.generatormod.client;

import net.minecraft.resources.ResourceLocation;

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

    private ClientGeneratorState() {
    }

    public void apply(ResourceLocation selectedItem, boolean running, long storedItems, int speedLevel, int quantityLevel,
                      long intervalMillis, int amountPerCycle, long runningSince, long lastUpdate, long leftoverMillis, String message) {
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

    public int getQuantityLevel() {
        return quantityLevel;
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
}
