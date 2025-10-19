package com.example.generatormod.network;

import com.example.generatormod.client.ClientGeneratorState;
import com.example.generatormod.client.GeneratorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class SyncGeneratorStatePacket {
    private final boolean openScreen;
    private final ResourceLocation selectedItem;
    private final boolean running;
    private final long storedItems;
    private final int speedLevel;
    private final int quantityLevel;
    private final long intervalMillis;
    private final int amountPerCycle;
    private final long runningSince;
    private final long lastUpdate;
    private final long leftoverMillis;
    private final Set<ResourceLocation> unlockedItems;
    private final Map<ResourceLocation, Integer> speedLevels;
    private final Map<ResourceLocation, Integer> quantityLevels;
    private final String message;

    public SyncGeneratorStatePacket(boolean openScreen, ResourceLocation selectedItem, boolean running, long storedItems,
                                    int speedLevel, int quantityLevel, long intervalMillis, int amountPerCycle,
                                    long runningSince, long lastUpdate, long leftoverMillis,
                                    Collection<ResourceLocation> unlockedItems,
                                    Map<ResourceLocation, Integer> speedLevels,
                                    Map<ResourceLocation, Integer> quantityLevels,
                                    String message) {
        this.openScreen = openScreen;
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
        this.unlockedItems = Set.copyOf(unlockedItems);
        this.speedLevels = Map.copyOf(speedLevels);
        this.quantityLevels = Map.copyOf(quantityLevels);
        this.message = message == null ? "" : message;
    }

    public static SyncGeneratorStatePacket decode(FriendlyByteBuf buf) {
        boolean open = buf.readBoolean();
        boolean hasItem = buf.readBoolean();
        ResourceLocation item = hasItem ? buf.readResourceLocation() : null;
        boolean running = buf.readBoolean();
        long stored = buf.readVarLong();
        int speed = buf.readVarInt();
        int quantity = buf.readVarInt();
        long interval = buf.readVarLong();
        int amount = buf.readVarInt();
        long runningSince = buf.readVarLong();
        long lastUpdate = buf.readVarLong();
        long leftover = buf.readVarLong();
        int unlockedSize = buf.readVarInt();
        Set<ResourceLocation> unlocked = new HashSet<>();
        for (int i = 0; i < unlockedSize; i++) {
            unlocked.add(buf.readResourceLocation());
        }
        int speedSize = buf.readVarInt();
        Map<ResourceLocation, Integer> speedLevels = new HashMap<>();
        for (int i = 0; i < speedSize; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int level = buf.readVarInt();
            speedLevels.put(id, level);
        }
        int quantitySize = buf.readVarInt();
        Map<ResourceLocation, Integer> quantityLevels = new HashMap<>();
        for (int i = 0; i < quantitySize; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int level = buf.readVarInt();
            quantityLevels.put(id, level);
        }
        String message = buf.readUtf(32767);
        return new SyncGeneratorStatePacket(open, item, running, stored, speed, quantity, interval, amount, runningSince, lastUpdate, leftover, unlocked, speedLevels, quantityLevels, message);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(openScreen);
        buf.writeBoolean(selectedItem != null);
        if (selectedItem != null) {
            buf.writeResourceLocation(selectedItem);
        }
        buf.writeBoolean(running);
        buf.writeVarLong(storedItems);
        buf.writeVarInt(speedLevel);
        buf.writeVarInt(quantityLevel);
        buf.writeVarLong(intervalMillis);
        buf.writeVarInt(amountPerCycle);
        buf.writeVarLong(runningSince);
        buf.writeVarLong(lastUpdate);
        buf.writeVarLong(leftoverMillis);
        buf.writeVarInt(unlockedItems.size());
        for (ResourceLocation id : unlockedItems) {
            buf.writeResourceLocation(id);
        }
        buf.writeVarInt(speedLevels.size());
        for (Map.Entry<ResourceLocation, Integer> entry : speedLevels.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
        buf.writeVarInt(quantityLevels.size());
        for (Map.Entry<ResourceLocation, Integer> entry : quantityLevels.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
        buf.writeUtf(message);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ClientGeneratorState.INSTANCE.apply(selectedItem, running, storedItems, speedLevel, quantityLevel,
                intervalMillis, amountPerCycle, runningSince, lastUpdate, leftoverMillis, unlockedItems,
                speedLevels, quantityLevels, message);
            if (openScreen) {
                minecraft.setScreen(new GeneratorScreen());
            } else if (minecraft.screen instanceof GeneratorScreen screen) {
                screen.refreshSelection(selectedItem);
            }
        });
        context.setPacketHandled(true);
    }
}
