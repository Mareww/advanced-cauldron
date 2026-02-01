package com.marew.advancedcauldron.mixin.client;

import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.util.CauldronColorUtil;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
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

        // Steam particles - always active on any hot cauldron (even empty), subtle wisp
        if (ModConfig.get().steamParticlesEnabled && random.nextInt(3) == 0) {
            double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
            double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
            double y = pos.getY() + 0.9 + random.nextDouble() * 0.1;

            world.addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x, y, z,
                    0.0, 0.03 + random.nextDouble() * 0.01, 0.0
            );
        }

        // Colored bubble particles - only during active brewing, from the liquid surface
        BlockEntity be = world.getBlockEntity(pos);
        if (ModConfig.get().brewingBubbleParticlesEnabled && be instanceof BrewingCauldronBlockEntity brewBE && brewBE.isBrewing()) {
            int color = CauldronColorUtil.getLiquidColor(world, pos);
            if (color == -1) color = 0x3F76E4; // fallback to water blue

            for (int i = 0; i < 5; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 0.05 + random.nextDouble() * 0.25;
                double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
                // Spawn at the top of the liquid surface within the cauldron
                double y = pos.getY() + 0.5 + random.nextDouble() * 0.15;

                world.addParticle(
                        EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, color),
                        x, y, z,
                        0.0, 0.03 + random.nextDouble() * 0.02, 0.0
                );
            }
        }
    }
}
