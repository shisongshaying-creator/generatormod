package com.example.generatormod.client;

import com.example.generatormod.GeneratorMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class GeneratorKeyMappings {
    private GeneratorKeyMappings() {}

    public static final KeyMapping OPEN_GENERATOR = new KeyMapping(
        "key." + GeneratorMod.MODID + ".open_generator",
        GLFW.GLFW_KEY_O,
        "key.categories.inventory"
    );

    static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GENERATOR);
    }
}
