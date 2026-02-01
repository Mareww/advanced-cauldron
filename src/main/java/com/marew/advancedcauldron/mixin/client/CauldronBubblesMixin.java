package com.marew.advancedcauldron.mixin.client;

import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.util.CauldronColorUtil;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
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
        if (state.isOf(Blocks.CAULDRON)) return;

        // Steam particles
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

        // Colored bubble particles during active brewing
        BlockEntity be = world.getBlockEntity(pos);
        if (ModConfig.get().brewingBubbleParticlesEnabled && be instanceof BrewingCauldronBlockEntity brewBE && brewBE.isBrewing()) {
            int color = CauldronColorUtil.getLiquidColor(world, pos);
            if (color == -1) color = 0x3F76E4;

            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;

            for (int i = 0; i < 5; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 0.05 + random.nextDouble() * 0.25;
                double px = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double pz = pos.getZ() + 0.5 + Math.sin(angle) * radius;
                double py = pos.getY() + 0.5 + random.nextDouble() * 0.15;

                world.addParticle(
                        ParticleTypes.ENTITY_EFFECT,
                        px, py, pz,
                        r, g, b
                );
            }
        }
    }
}
