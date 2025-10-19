package com.example.generatormod.network;

import com.example.generatormod.GeneratorMod;
import com.example.generatormod.generator.GeneratorState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;

import java.util.Map;

public final class GeneratorNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    private static int packetId = 0;

    private GeneratorNetwork() {
    }

    public static void init() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(GeneratorMod.MODID, "main"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .simpleChannel();

        CHANNEL.messageBuilder(RequestOpenGeneratorScreenPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(RequestOpenGeneratorScreenPacket::decode)
            .encoder(RequestOpenGeneratorScreenPacket::encode)
            .consumerMainThread(RequestOpenGeneratorScreenPacket::handle)
            .add();

        CHANNEL.messageBuilder(RequestOpenCloudStoragePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(RequestOpenCloudStoragePacket::decode)
            .encoder(RequestOpenCloudStoragePacket::encode)
            .consumerMainThread(RequestOpenCloudStoragePacket::handle)
            .add();

        CHANNEL.messageBuilder(ExecuteGeneratorPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(ExecuteGeneratorPacket::decode)
            .encoder(ExecuteGeneratorPacket::encode)
            .consumerMainThread(ExecuteGeneratorPacket::handle)
            .add();

        CHANNEL.messageBuilder(CollectGeneratorPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(CollectGeneratorPacket::decode)
            .encoder(CollectGeneratorPacket::encode)
            .consumerMainThread(CollectGeneratorPacket::handle)
            .add();

        CHANNEL.messageBuilder(WithdrawCloudStoragePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(WithdrawCloudStoragePacket::decode)
            .encoder(WithdrawCloudStoragePacket::encode)
            .consumerMainThread(WithdrawCloudStoragePacket::handle)
            .add();

        CHANNEL.messageBuilder(UpgradeGeneratorPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(UpgradeGeneratorPacket::decode)
            .encoder(UpgradeGeneratorPacket::encode)
            .consumerMainThread(UpgradeGeneratorPacket::handle)
            .add();

        CHANNEL.messageBuilder(SyncGeneratorStatePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SyncGeneratorStatePacket::decode)
            .encoder(SyncGeneratorStatePacket::encode)
            .consumerMainThread(SyncGeneratorStatePacket::handle)
            .add();

        CHANNEL.messageBuilder(SyncCloudStoragePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SyncCloudStoragePacket::decode)
            .encoder(SyncCloudStoragePacket::encode)
            .consumerMainThread(SyncCloudStoragePacket::handle)
            .add();
    }

    private static int nextId() {
        return packetId++;
    }

    public static void syncCloudStorage(ServerPlayer player, GeneratorState state, boolean openScreen) {
        Map<ResourceLocation, Long> snapshot = state.getCloudStorageSnapshot();
        SyncCloudStoragePacket packet = new SyncCloudStoragePacket(openScreen, snapshot);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void syncToClient(ServerPlayer player, GeneratorState state, boolean openScreen, String message) {
        ResourceLocation itemId = state.getSelectedItem();
        long interval = state.getIntervalMillis();
        int amount = state.getAmountPerCycle();
        SyncGeneratorStatePacket packet = new SyncGeneratorStatePacket(
            openScreen,
            itemId,
            state.isRunning(),
            state.getStoredItems(),
            state.getSpeedLevel(),
            state.getQuantityLevel(),
            interval,
            amount,
            state.getRunningSince(),
            state.getLastRealTime(),
            state.getLeftoverMillis(),
            state.getUnlockedItems(),
            state.getSpeedLevels(),
            state.getQuantityLevels(),
            message,
            state.getCloudStorageSnapshot()
        );
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
