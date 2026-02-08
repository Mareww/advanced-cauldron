package com.marew.advancedcauldron.util;

import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.DyedWaterCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.MilkCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.PotionCauldronBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CauldronColorUtil {
    public static final int DEFAULT_WATER_COLOR = 0x3F76E4;

    public static int getLiquidColor(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (state.isOf(Blocks.WATER_CAULDRON)) {
            return DEFAULT_WATER_COLOR;
        }

        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof PotionCauldronBlockEntity potionBE) {
            Potion potion = potionBE.getPotion();
            if (potion != null) return PotionUtil.getColor(potion.getEffects());
            return DEFAULT_WATER_COLOR;
        }

        if (be instanceof DyedWaterCauldronBlockEntity dyedBE) {
            return dyedBE.getColor();
        }

        if (be instanceof BrewingCauldronBlockEntity brewBE) {
            return brewBE.getDisplayColor();
        }

        if (be instanceof MilkCauldronBlockEntity) {
            return 0xFFFFFF; // White for milk
        }

        return -1;
    }
}
