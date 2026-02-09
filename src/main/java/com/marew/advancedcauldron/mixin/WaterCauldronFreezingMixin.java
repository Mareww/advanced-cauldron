package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.block.FrozenCauldronBlock;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeveledCauldronBlock.class)
public class WaterCauldronFreezingMixin {

    @Inject(method = "precipitationTick", at = @At("HEAD"))
    private void advancedcauldron$handleSnowOnWater(
            BlockState state, World world, BlockPos pos,
            Biome.Precipitation precipitation, CallbackInfo ci) {

        if (!state.isOf(Blocks.WATER_CAULDRON)) return;
        if (precipitation != Biome.Precipitation.SNOW) return;
        if (world.getRandom().nextFloat() >= 0.05f) return;

        int level = state.get(LeveledCauldronBlock.LEVEL);

        if (HeatSourceUtil.hasHeatSource(world, pos)) {
            // Snow melts into the boiling water, filling it
            if (level < 3) {
                world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, level + 1));
            }
        } else {
            // Water freezes
            world.setBlockState(pos, ModBlocks.FROZEN_CAULDRON.getDefaultState()
                    .with(FrozenCauldronBlock.LEVEL, level));
        }
    }
}
