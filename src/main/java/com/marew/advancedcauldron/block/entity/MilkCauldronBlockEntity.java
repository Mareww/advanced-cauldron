package com.marew.advancedcauldron.block.entity;

import com.marew.advancedcauldron.block.MilkCauldronBlock;
import com.marew.advancedcauldron.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class MilkCauldronBlockEntity extends BlockEntity {

    public MilkCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MILK_CAULDRON_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, MilkCauldronBlockEntity blockEntity) {
        if (world.isClient) return;

        // Check for entities standing in the cauldron
        Box cauldronBox = new Box(pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
                pos.getX() + 0.9, pos.getY() + 1.0, pos.getZ() + 0.9);

        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, cauldronBox, e -> true)) {
            // Only clear if the entity has effects
            if (!entity.getStatusEffects().isEmpty()) {
                entity.clearStatusEffects();

                // Drain 1 level
                int currentLevel = state.get(MilkCauldronBlock.LEVEL);
                if (currentLevel <= 1) {
                    world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                } else {
                    world.setBlockState(pos, state.with(MilkCauldronBlock.LEVEL, currentLevel - 1));
                }
                break; // Only process one entity per tick to avoid draining too fast
            }
        }
    }
}
