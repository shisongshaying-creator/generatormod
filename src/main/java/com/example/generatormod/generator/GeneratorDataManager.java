package com.example.generatormod.generator;

import com.example.generatormod.GeneratorMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GeneratorDataManager {
    private static final String ROOT_TAG = GeneratorMod.MODID + "_generator";
    private static final Map<UUID, GeneratorState> CACHE = new HashMap<>();

    private GeneratorDataManager() {
    }

    public static GeneratorState get(ServerPlayer player) {
        return CACHE.computeIfAbsent(player.getUUID(), uuid -> {
            GeneratorState state = new GeneratorState();
            CompoundTag tag = getOrCreateTag(player);
            state.load(tag.copy());
            if (state.getLastRealTime() == 0L) {
                state.setLastRealTime(System.currentTimeMillis());
            }
            return state;
        });
    }

    private static CompoundTag getOrCreateTag(Player player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag forgeData;
        if (persistent.contains(Player.PERSISTED_NBT_TAG)) {
            forgeData = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        } else {
            forgeData = new CompoundTag();
            persistent.put(Player.PERSISTED_NBT_TAG, forgeData);
        }
        if (!forgeData.contains(ROOT_TAG)) {
            forgeData.put(ROOT_TAG, new CompoundTag());
            persistent.put(Player.PERSISTED_NBT_TAG, forgeData);
        }
        return forgeData.getCompound(ROOT_TAG);
    }

    public static void save(ServerPlayer player) {
        GeneratorState state = get(player);
        write(player, state);
        state.markSaved();
    }

    public static void saveIfDirty(ServerPlayer player) {
        GeneratorState state = get(player);
        if (state.isDirty()) {
            write(player, state);
            state.markSaved();
        }
    }

    private static void write(Player player, GeneratorState state) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag forgeData = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag generatorTag = forgeData.getCompound(ROOT_TAG);
        state.save(generatorTag);
        forgeData.put(ROOT_TAG, generatorTag);
        persistent.put(Player.PERSISTED_NBT_TAG, forgeData);
    }

    public static void handlePlayerClone(ServerPlayer original, ServerPlayer clone) {
        CompoundTag originalTag = getOrCreateTag(original).copy();
        CompoundTag persistent = clone.getPersistentData();
        CompoundTag forgeData = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        forgeData.put(ROOT_TAG, originalTag);
        persistent.put(Player.PERSISTED_NBT_TAG, forgeData);
        CACHE.remove(original.getUUID());
    }

    public static void remove(ServerPlayer player) {
        CACHE.remove(player.getUUID());
    }
}
