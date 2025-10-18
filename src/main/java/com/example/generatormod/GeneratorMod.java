package com.example.generatormod;

import com.example.generatormod.client.ClientInitializer;
import com.example.generatormod.network.GeneratorNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(GeneratorMod.MODID)
public class GeneratorMod {
    public static final String MODID = "generatormod";

    public GeneratorMod() {
        GeneratorNetwork.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInitializer.init();
        }
    }
}
