package com.example.generatormod.network;

import com.example.generatormod.generator.GeneratorDataManager;
import com.example.generatormod.generator.GeneratorState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestOpenCloudStoragePacket() {
    public static RequestOpenCloudStoragePacket decode(FriendlyByteBuf buf) {
        return new RequestOpenCloudStoragePacket();
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            GeneratorState state = GeneratorDataManager.get(player);
            state.tick(System.currentTimeMillis());
            GeneratorDataManager.save(player);
            GeneratorNetwork.syncCloudStorage(player, state, true);
        });
        context.setPacketHandled(true);
    }
}
