package com.marew.advancedcauldron.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HeatSourceUtil {
    public static boolean hasHeatSource(World world, BlockPos cauldronPos) {
        BlockPos below = cauldronPos.down();
        BlockState belowState = world.getBlockState(below);

        if (belowState.isOf(Blocks.LAVA)) return true;
        if (belowState.isOf(Blocks.MAGMA_BLOCK)) return true;
        if (belowState.isOf(Blocks.FIRE)) return true;
        if (belowState.isOf(Blocks.SOUL_FIRE)) return true;
        if (belowState.getBlock() instanceof CampfireBlock && belowState.get(CampfireBlock.LIT)) return true;

        return false;
    }
}
