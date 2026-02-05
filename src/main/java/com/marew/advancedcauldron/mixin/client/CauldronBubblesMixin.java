package com.marew.advancedcauldron.mixin.client;

import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.registry.ModParticles;
import com.marew.advancedcauldron.util.CauldronColorUtil;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractCauldronBlock.class)
public abstract class CauldronBubblesMixin extends Block {

    protected CauldronBubblesMixin(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);

        if (!HeatSourceUtil.hasHeatSource(world, pos)) return;
        if (state.isOf(Blocks.CAULDRON)) return;

        // Steam particles (white wispy steam) - constant
        if (ModConfig.get().steamParticlesEnabled) {
            for (int i = 0; i < 3; i++) {
                double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
                double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
                double y = pos.getY() + 0.95;

                world.addParticle(
                        ModParticles.CAULDRON_STEAM,
                        x, y, z,
                        0.0, 0.0, 0.0
                );
            }
        }

        // POPPING BUBBLES - always when water is boiling (heat source)
        // Uses custom colored bubble particle
        if (ModConfig.get().brewingBubbleParticlesEnabled) {
            int waterColor = CauldronColorUtil.getLiquidColor(world, pos);
            if (waterColor == -1) waterColor = 0x3F76E4;

            float r = ((waterColor >> 16) & 0xFF) / 255.0F;
            float g = ((waterColor >> 8) & 0xFF) / 255.0F;
            float b = (waterColor & 0xFF) / 255.0F;

            for (int i = 0; i < 8; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 0.05 + random.nextDouble() * 0.18;
                double px = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double pz = pos.getZ() + 0.5 + Math.sin(angle) * radius;
                double py = pos.getY() + 0.92 + random.nextDouble() * 0.02; // Just at water surface

                // Custom particle: velocity params are RGB color
                world.addParticle(
                        ModParticles.CAULDRON_BUBBLE,
                        px, py, pz,
                        r, g, b
                );
            }

            // Bubbling sound
            world.playSound(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP,
                    SoundCategory.BLOCKS,
                    0.8F + random.nextFloat() * 0.4F,
                    0.8F + random.nextFloat() * 0.4F,
                    false
            );
        }

        // FLOWING COLORED BUBBLES - only during active brewing
        // Uses same color as water (getDisplayColor = transitioning color)
        BlockEntity be = world.getBlockEntity(pos);
        if (ModConfig.get().brewingBubbleParticlesEnabled && be instanceof BrewingCauldronBlockEntity brewBE && brewBE.isBrewing()) {
            int color = brewBE.getDisplayColor(); // Use transitioning color, not target

            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;

            // Colored flowing particles during brewing
            for (int i = 0; i < 6; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 0.15 + random.nextDouble() * 0.35;
                double px = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double pz = pos.getZ() + 0.5 + Math.sin(angle) * radius;
                double py = pos.getY() + 0.95 + random.nextDouble() * 0.3;

                world.addParticle(
                        ParticleTypes.ENTITY_EFFECT,
                        px, py, pz,
                        r, g, b
                );
            }
        }
    }
}
