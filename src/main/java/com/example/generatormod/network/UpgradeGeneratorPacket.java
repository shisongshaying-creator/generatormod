package com.example.generatormod.network;

import com.example.generatormod.generator.GeneratorDataManager;
import com.example.generatormod.generator.GeneratorState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpgradeGeneratorPacket {
    public enum Type {
        SPEED,
        QUANTITY
    }

    private final Type type;
    private final ResourceLocation itemId;

    public UpgradeGeneratorPacket(Type type, ResourceLocation itemId) {
        this.type = type;
        this.itemId = itemId;
    }

    public static UpgradeGeneratorPacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        Type type = Type.values()[Math.max(0, Math.min(Type.values().length - 1, ordinal))];
        ResourceLocation itemId = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new UpgradeGeneratorPacket(type, itemId);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());
        buf.writeBoolean(itemId != null);
        if (itemId != null) {
            buf.writeResourceLocation(itemId);
        }
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
            switch (type) {
                case SPEED -> state.upgradeSpeed(player, itemId);
                case QUANTITY -> state.upgradeQuantity(player, itemId);
            }
            GeneratorDataManager.save(player);
            String message = state.getTransientMessage();
            GeneratorNetwork.syncToClient(player, state, false, message);
            state.clearTransientMessage();
        });
        context.setPacketHandled(true);
    }
}
