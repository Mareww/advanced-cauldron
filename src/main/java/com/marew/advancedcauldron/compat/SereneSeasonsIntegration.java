package com.marew.advancedcauldron.compat;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import sereneseasons.season.SeasonHooks;

/**
 * Isolated class that references Serene Seasons classes directly.
 * Only loaded by the JVM when actually called (gated by SereneSeasonsCompat.isLoaded()).
 */
public class SereneSeasonsIntegration {

    public static boolean isColdEnoughToFreeze(World world, BlockPos pos) {
        return SeasonHooks.coldEnoughToSnowSeasonal(world, pos);
    }

    public static boolean isWarmEnoughToThaw(World world, BlockPos pos) {
        return !SeasonHooks.coldEnoughToSnowSeasonal(world, pos);
    }
}
