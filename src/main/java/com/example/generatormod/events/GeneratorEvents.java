package com.example.generatormod.events;

import com.example.generatormod.GeneratorMod;
import com.example.generatormod.generator.GeneratorDataManager;
import com.example.generatormod.generator.GeneratorState;
import com.example.generatormod.network.GeneratorNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GeneratorMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneratorEvents {
    private GeneratorEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.player;
        GeneratorState state = GeneratorDataManager.get(player);
        boolean generated = state.tick(System.currentTimeMillis());
        if (generated) {
            GeneratorDataManager.save(player);
        } else {
            GeneratorDataManager.saveIfDirty(player);
        }
        String message = state.getTransientMessage();
        boolean shouldSend = generated || !message.isEmpty() || player.tickCount % 20 == 0;
        if (shouldSend) {
            GeneratorNetwork.syncToClient(player, state, false, message);
            state.clearTransientMessage();
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer clone) || !(event.getOriginal() instanceof ServerPlayer original)) {
            return;
        }
        if (!event.isWasDeath()) {
            return;
        }
        GeneratorDataManager.handlePlayerClone(original, clone);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GeneratorDataManager.save(player);
            GeneratorDataManager.remove(player);
        }
    }
}
