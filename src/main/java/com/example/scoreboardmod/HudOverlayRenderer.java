package com.example.scoreboardmod;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scoreboardmod", value = Dist.CLIENT)
public class HudOverlayRenderer {

    static {
        MinecraftForge.EVENT_BUS.register(HudOverlayRenderer.class);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || player.level == null) return;

        long ticks = player.level.getDayTime() % 24000;

        int hours = (int)((ticks / 1000 + 6) % 24);  // ゲーム内時間を補正
        int minutes = (int)((ticks % 1000) * 60 / 1000);
        String time = String.format("現在時刻: %02d:%02d", hours, minutes);

        BlockPos pos = player.blockPosition(); // 整数の座標
        String coords = String.format("座標: %d / %d / %d", pos.getX(), pos.getY(), pos.getZ());

        MatrixStack matrixStack = event.getMatrixStack();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int timeWidth = mc.font.width(time);
        int coordsWidth = mc.font.width(coords);

        mc.font.drawShadow(matrixStack, time, screenWidth - timeWidth - 5, 5, 0xFFFFFF);
        mc.font.drawShadow(matrixStack, coords, screenWidth - coordsWidth - 5, 15, 0xFFFFFF);
    }
}
