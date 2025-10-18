package com.example.generatormod.client;

import com.example.generatormod.network.GeneratorNetwork;
import com.example.generatormod.network.RequestOpenGeneratorScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInitializer {
    private static boolean initialized;

    private ClientInitializer() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(ClientInitializer::onClientSetup);
        modBus.addListener(GeneratorKeyMappings::register);
        MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {});
    }

    private static class ClientTickHandler {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (Minecraft.getInstance().screen != null) {
                return;
            }
            while (GeneratorKeyMappings.OPEN_GENERATOR.consumeClick()) {
                GeneratorNetwork.CHANNEL.sendToServer(new RequestOpenGeneratorScreenPacket());
            }
        }
    }
}
