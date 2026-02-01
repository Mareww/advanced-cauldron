package com.marew.advancedcauldron.mixin.client;

import com.marew.advancedcauldron.config.ModConfig;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlock.class)
public class CampfireSmokeMixin {

    @Inject(method = "spawnSmokeParticle", at = @At("HEAD"), cancellable = true)
    private static void advancedcauldron$suppressSmokeUnderCauldron(
            World world, BlockPos pos, boolean isSignal, boolean lotsOfSmoke, CallbackInfo ci) {
        if (!ModConfig.get().suppressCampfireSmoke) return;
        BlockPos above = pos.up();
        if (world.getBlockState(above).getBlock() instanceof AbstractCauldronBlock) {
            ci.cancel();
        }
    }
}
