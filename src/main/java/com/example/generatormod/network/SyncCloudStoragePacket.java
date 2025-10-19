package com.example.generatormod.network;

import com.example.generatormod.client.ClientCloudStorageState;
import com.example.generatormod.client.ClientGeneratorState;
import com.example.generatormod.client.CloudStorageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncCloudStoragePacket {
    private final boolean openScreen;
    private final Map<ResourceLocation, Long> storedItems;

    public SyncCloudStoragePacket(boolean openScreen, Map<ResourceLocation, Long> storedItems) {
        this.openScreen = openScreen;
        this.storedItems = Map.copyOf(storedItems);
    }

    public static SyncCloudStoragePacket decode(FriendlyByteBuf buf) {
        boolean open = buf.readBoolean();
        int size = buf.readVarInt();
        Map<ResourceLocation, Long> items = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            long amount = buf.readVarLong();
            items.put(id, amount);
        }
        return new SyncCloudStoragePacket(open, items);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(openScreen);
        buf.writeVarInt(storedItems.size());
        for (Map.Entry<ResourceLocation, Long> entry : storedItems.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarLong(entry.getValue());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ClientCloudStorageState.INSTANCE.apply(storedItems);
            ClientGeneratorState.INSTANCE.updateCloudStorage(storedItems);
            if (openScreen) {
                minecraft.setScreen(new CloudStorageScreen());
            } else if (minecraft.screen instanceof CloudStorageScreen cloudScreen) {
                cloudScreen.refreshFromNetwork();
            }
        });
        context.setPacketHandled(true);
    }
}
