package com.example.generatormod.network;

import com.example.generatormod.generator.GeneratorDataManager;
import com.example.generatormod.generator.GeneratorItems;
import com.example.generatormod.generator.GeneratorState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DepositCloudStoragePacket(ResourceLocation itemId, long amount) {
    public static DepositCloudStoragePacket decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        long requested = buf.readVarLong();
        return new DepositCloudStoragePacket(id, requested);
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
            long deposited = depositFromInventory(player, state);
            if (deposited > 0L) {
                player.getInventory().setChanged();
            }
            GeneratorDataManager.save(player);
            GeneratorNetwork.syncCloudStorage(player, state, false);
            GeneratorNetwork.syncToClient(player, state, false, state.getTransientMessage());
            state.clearTransientMessage();
        });
        context.setPacketHandled(true);
    }

    private long depositFromInventory(ServerPlayer player, GeneratorState state) {
        if (itemId == null || amount <= 0L) {
            return 0L;
        }
        Item item = GeneratorItems.resolve(itemId).orElse(null);
        if (item == null) {
            return 0L;
        }
        long available = countInventory(player, item);
        if (available <= 0L) {
            return 0L;
        }
        long toRemove = Math.min(amount, available);
        long removed = removeFromInventory(player, item, toRemove);
        if (removed <= 0L) {
            return 0L;
        }
        state.depositToCloud(itemId, removed);
        return removed;
    }

    private long countInventory(ServerPlayer player, Item item) {
        long total = 0L;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private long removeFromInventory(ServerPlayer player, Item item, long amountToRemove) {
        long remaining = amountToRemove;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }
            int toTake = (int) Math.min(stack.getCount(), remaining);
            if (toTake > 0) {
                stack.shrink(toTake);
                remaining -= toTake;
                if (remaining <= 0L) {
                    return amountToRemove;
                }
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }
            int toTake = (int) Math.min(stack.getCount(), remaining);
            if (toTake > 0) {
                stack.shrink(toTake);
                remaining -= toTake;
                if (remaining <= 0L) {
                    break;
                }
            }
        }
        return amountToRemove - remaining;
    }
}
