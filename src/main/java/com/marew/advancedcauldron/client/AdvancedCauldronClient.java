package com.marew.advancedcauldron.client;

import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.DyedWaterCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.PotionCauldronBlockEntity;
import com.marew.advancedcauldron.particle.CauldronBubbleParticle;
import com.marew.advancedcauldron.particle.CauldronSteamParticle;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.PotionContentsComponent;

public class AdvancedCauldronClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Color the potion cauldron water based on the stored potion
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (tintIndex != 0 || world == null || pos == null) return -1;
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PotionCauldronBlockEntity potionBE) {
                PotionContentsComponent contents = potionBE.getPotionContents();
                if (contents != null) {
                    return contents.getColor();
                }
            }
            return 0x3F76E4; // Default water color
        }, ModBlocks.POTION_CAULDRON);

        // Color the dyed water cauldron based on stored dye color
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (tintIndex != 0 || world == null || pos == null) return -1;
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof DyedWaterCauldronBlockEntity dyedBE) {
                return dyedBE.getColor();
            }
            return 0x3F76E4;
        }, ModBlocks.DYED_WATER_CAULDRON);

        // Color the brewing cauldron based on current potion state
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (tintIndex != 0 || world == null || pos == null) return -1;
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BrewingCauldronBlockEntity brewBE) {
                return brewBE.getDisplayColor();
            }
            return 0x3F76E4;
        }, ModBlocks.BREWING_CAULDRON);

        // Register custom cauldron bubble particle
        ParticleFactoryRegistry.getInstance().register(ModParticles.CAULDRON_BUBBLE, CauldronBubbleParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.CAULDRON_STEAM, CauldronSteamParticle.Factory::new);
    }
}
