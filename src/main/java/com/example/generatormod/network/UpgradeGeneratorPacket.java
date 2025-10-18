package com.example.generatormod.network;

import com.example.generatormod.generator.GeneratorDataManager;
import com.example.generatormod.generator.GeneratorState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpgradeGeneratorPacket {
    public enum Type {
        SPEED,
        QUANTITY
    }

    private final Type type;

    public UpgradeGeneratorPacket(Type type) {
        this.type = type;
    }

    public static UpgradeGeneratorPacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        Type type = Type.values()[Math.max(0, Math.min(Type.values().length - 1, ordinal))];
        return new UpgradeGeneratorPacket(type);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());
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
                case SPEED -> state.upgradeSpeed(player);
                case QUANTITY -> state.upgradeQuantity(player);
            }
            GeneratorDataManager.save(player);
            String message = state.getTransientMessage();
            GeneratorNetwork.syncToClient(player, state, false, message);
            state.clearTransientMessage();
        });
        context.setPacketHandled(true);
    }
}
