package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.block.FrozenCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CauldronBlock.class)
public class CauldronSnowMixin {

    @Inject(method = "precipitationTick", at = @At("HEAD"), cancellable = true)
    private void advancedcauldron$handleSnowOnEmpty(
            BlockState state, World world, BlockPos pos,
            Biome.Precipitation precipitation, CallbackInfo ci) {
        if (precipitation == Biome.Precipitation.SNOW) {
            FrozenCauldronBlock.freeze(world, pos, 1, "snow", 0xFFFFFF, null, 0);
            ci.cancel();
        }
    }
}
