package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.block.FrozenCauldronBlock;
import com.marew.advancedcauldron.compat.SereneSeasonsCompat;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LeveledCauldronBlock.class)
public abstract class WaterCauldronFreezingMixin extends AbstractCauldronBlock {

    protected WaterCauldronFreezingMixin(Settings settings, Map<Item, CauldronBehavior> behaviorMap) {
        super(settings, behaviorMap);
    }

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
            FrozenCauldronBlock.freeze(world, pos, level, "water", -1, null, 0);
        }
    }

    // Enable random ticks for water cauldrons so they can freeze from biome temperature
    @Override
    public boolean hasRandomTicks(BlockState state) {
        if (state.isOf(Blocks.WATER_CAULDRON)) {
            return true;
        }
        return super.hasRandomTicks(state);
    }

    // Freeze water cauldrons when cold enough (biome temperature or Serene Seasons)
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (!state.isOf(Blocks.WATER_CAULDRON)) return;
        if (HeatSourceUtil.hasHeatSource(world, pos)) return;

        boolean shouldFreeze;
        if (SereneSeasonsCompat.isLoaded()) {
            shouldFreeze = SereneSeasonsCompat.isColdEnoughToFreeze(world, pos);
        } else {
            shouldFreeze = world.getBiome(pos).value().isCold(pos);
        }

        if (shouldFreeze) {
            int level = state.get(LeveledCauldronBlock.LEVEL);
            FrozenCauldronBlock.freeze(world, pos, level, "water", -1, null, 0);
        }
    }
}
