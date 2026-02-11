package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.block.FrozenCauldronBlock;
import com.marew.advancedcauldron.config.ModConfig;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.item.Item;
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

import java.util.Map;

@Mixin(CauldronBlock.class)
public abstract class CauldronSnowMixin extends AbstractCauldronBlock {

    protected CauldronSnowMixin(Settings settings, Map<Item, CauldronBehavior> behaviorMap) {
        super(settings, behaviorMap);
    }

    @Inject(method = "precipitationTick", at = @At("HEAD"), cancellable = true)
    private void advancedcauldron$handleSnowOnEmpty(
            BlockState state, World world, BlockPos pos,
            Biome.Precipitation precipitation, CallbackInfo ci) {
        if (precipitation == Biome.Precipitation.SNOW && ModConfig.get().snowFillingEnabled) {
            FrozenCauldronBlock.freeze(world, pos, 1, "snow", 0xFFFFFF, null, 0);
            ci.cancel();
        }
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!world.isRaining()) return;
        if (world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos).getY() > pos.getY() + 1) return;

        Biome biome = world.getBiome(pos).value();
        if (!biome.hasPrecipitation()) return;

        if (biome.isCold(pos)) {
            if (ModConfig.get().snowFillingEnabled) {
                FrozenCauldronBlock.freeze(world, pos, 1, "snow", 0xFFFFFF, null, 0);
            }
        } else {
            if (ModConfig.get().rainFillingEnabled) {
                world.setBlockState(pos, Blocks.WATER_CAULDRON.getDefaultState());
            }
        }
    }
}
