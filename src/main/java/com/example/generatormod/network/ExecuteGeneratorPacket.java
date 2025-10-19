package com.example.generatormod.network;

import com.example.generatormod.generator.GeneratorDataManager;
import com.example.generatormod.generator.GeneratorState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExecuteGeneratorPacket {
    private final ResourceLocation itemId;

    public ExecuteGeneratorPacket(ResourceLocation itemId) {
        this.itemId = itemId;
    }

    public static ExecuteGeneratorPacket decode(FriendlyByteBuf buf) {
        ResourceLocation item = buf.readResourceLocation();
        return new ExecuteGeneratorPacket(item);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(itemId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            GeneratorState state = GeneratorDataManager.get(player);
            long now = System.currentTimeMillis();
            state.tick(now);
            state.start(itemId, player, now);
            GeneratorDataManager.save(player);
            String message = state.getTransientMessage();
            GeneratorNetwork.syncToClient(player, state, false, message);
            state.clearTransientMessage();
        });
        context.setPacketHandled(true);
    }
}
