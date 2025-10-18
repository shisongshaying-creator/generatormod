package com.example.generatormod.network;

import com.example.generatormod.GeneratorMod;
import com.example.generatormod.generator.GeneratorState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class GeneratorNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    private static int packetId = 0;

    private GeneratorNetwork() {
    }

    public static void init() {
        CHANNEL = ChannelBuilder.named(new ResourceLocation(GeneratorMod.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

        CHANNEL.messageBuilder(RequestOpenGeneratorScreenPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(RequestOpenGeneratorScreenPacket::decode)
            .encoder(RequestOpenGeneratorScreenPacket::encode)
            .consumerMainThread(RequestOpenGeneratorScreenPacket::handle)
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
    }

    private static int nextId() {
        return packetId++;
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
            message
        );
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
