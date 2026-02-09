package com.marew.advancedcauldron.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SereneSeasonsCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("sereneseasons");

    public static boolean isLoaded() {
        return LOADED;
    }

    public static boolean isColdEnoughToFreeze(World world, BlockPos pos) {
        return SereneSeasonsIntegration.isColdEnoughToFreeze(world, pos);
    }

    public static boolean isWarmEnoughToThaw(World world, BlockPos pos) {
        return SereneSeasonsIntegration.isWarmEnoughToThaw(world, pos);
    }
}
