package com.example.generatormod.network;

import com.example.generatormod.generator.GeneratorDataManager;
import com.example.generatormod.generator.GeneratorState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record WithdrawCloudStoragePacket(ResourceLocation itemId, long amount) {
    public static WithdrawCloudStoragePacket decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        long requested = buf.readVarLong();
        return new WithdrawCloudStoragePacket(id, requested);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(itemId);
        buf.writeVarLong(amount);
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
            List<ItemStack> stacks = state.withdrawFromCloud(itemId, amount);
            for (ItemStack stack : stacks) {
                boolean added = player.getInventory().add(stack);
                if (!added) {
                    player.drop(stack, false);
                }
            }
            GeneratorDataManager.save(player);
            GeneratorNetwork.syncCloudStorage(player, state, false);
            GeneratorNetwork.syncToClient(player, state, false, state.getTransientMessage());
            state.clearTransientMessage();
        });
        context.setPacketHandled(true);
    }
}
