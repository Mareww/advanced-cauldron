package com.marew.advancedcauldron.registry;

import com.marew.advancedcauldron.AdvancedCauldron;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {

    public static final DefaultParticleType CAULDRON_BUBBLE = FabricParticleTypes.simple();
    public static final DefaultParticleType CAULDRON_STEAM = FabricParticleTypes.simple();

    public static void register() {
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(AdvancedCauldron.MOD_ID, "cauldron_bubble"), CAULDRON_BUBBLE);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(AdvancedCauldron.MOD_ID, "cauldron_steam"), CAULDRON_STEAM);
    }
}
