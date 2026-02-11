package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.block.FrozenCauldronBlock;
import com.marew.advancedcauldron.compat.SereneSeasonsCompat;
import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeveledCauldronBlock.class)
public abstract class WaterCauldronFreezingMixin extends AbstractCauldronBlock {

    protected WaterCauldronFreezingMixin(Settings settings, CauldronBehavior.CauldronBehaviorMap behaviorMap) {
        super(settings, behaviorMap);
    }

    @Inject(method = "precipitationTick", at = @At("HEAD"))
    private void advancedcauldron$handleSnowOnWater(
            BlockState state, World world, BlockPos pos,
            Biome.Precipitation precipitation, CallbackInfo ci) {

        if (!state.isOf(Blocks.WATER_CAULDRON)) return;
        if (precipitation != Biome.Precipitation.SNOW) return;
        if (world.getRandom().nextFloat() >= (float) ModConfig.get().freezeChance) return;

        int level = state.get(LeveledCauldronBlock.LEVEL);

        if (HeatSourceUtil.hasHeatSource(world, pos)) {
            // Snow melts into the boiling water, filling it
            if (level < 3) {
                world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, level + 1));
            }
        } else if (ModConfig.get().freezingEnabled) {
            // Water freezes
            FrozenCauldronBlock.freeze(world, pos, level, "water", 0xA0C8E8, null, 0);
        }
    }

    // Enable random ticks for water cauldrons so they can freeze from biome temperature
    @Override
    protected boolean hasRandomTicks(BlockState state) {
        if (state.isOf(Blocks.WATER_CAULDRON)) {
            return true;
        }
        return super.hasRandomTicks(state);
    }

    // Fill water cauldrons from rain and freeze when cold enough
    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (!state.isOf(Blocks.WATER_CAULDRON)) return;

        int level = state.get(LeveledCauldronBlock.LEVEL);

        // Rain/snow filling when exposed to sky
        if (level < 3 && world.isRaining() && ModConfig.get().rainFillingEnabled) {
            boolean isExposed = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos).getY() <= pos.getY() + 1;
            if (isExposed) {
                Biome biome = world.getBiome(pos).value();
                if (biome.hasPrecipitation()) {
                    if (!biome.isCold(pos)) {
                        // Rain fills the cauldron
                        world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, level + 1));
                        return;
                    } else if (HeatSourceUtil.hasHeatSource(world, pos)) {
                        // Snow melts into heated water, filling it
                        world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, level + 1));
                        return;
                    }
                }
            }
        }

        // Freezing (only without heat source)
        if (!ModConfig.get().freezingEnabled) return;
        if (!ModConfig.get().biomeTemperatureFreezingEnabled) return;
        if (HeatSourceUtil.hasHeatSource(world, pos)) return;

        boolean shouldFreeze;
        if (SereneSeasonsCompat.isLoaded()) {
            shouldFreeze = SereneSeasonsCompat.isColdEnoughToFreeze(world, pos);
        } else {
            shouldFreeze = world.getBiome(pos).value().isCold(pos);
        }

        if (shouldFreeze) {
            level = state.get(LeveledCauldronBlock.LEVEL);
            FrozenCauldronBlock.freeze(world, pos, level, "water", 0xA0C8E8, null, 0);
        }
    }
}
