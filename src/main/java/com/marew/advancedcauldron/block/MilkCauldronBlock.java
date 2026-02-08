package com.marew.advancedcauldron.block;

import com.marew.advancedcauldron.block.entity.MilkCauldronBlockEntity;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.util.HeatDamageUtil;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MilkCauldronBlock extends AbstractCauldronBlock implements BlockEntityProvider {
    public static final IntProperty LEVEL = Properties.LEVEL_3;

    public MilkCauldronBlock(Settings settings, Map<Item, CauldronBehavior> behaviorMap) {
        super(settings, behaviorMap);
        this.setDefaultState(this.stateManager.getDefaultState().with(LEVEL, 1));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.get(LEVEL) == 3;
    }

    @Override
    protected double getFluidHeight(BlockState state) {
        return (6.0 + (double) state.get(LEVEL) * 3.0) / 16.0;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) {
            HeatDamageUtil.tick(entity, world, pos);
        }
    }

    @Override
    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(LEVEL);
    }

    public static void decrementLevel(BlockState state, World world, BlockPos pos) {
        int level = state.get(LEVEL);
        if (level <= 1) {
            world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
        } else {
            world.setBlockState(pos, state.with(LEVEL, level - 1));
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MilkCauldronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return type == ModBlocks.MILK_CAULDRON_BLOCK_ENTITY
                ? (world1, pos, state1, blockEntity) -> MilkCauldronBlockEntity.tick(world1, pos, state1, (MilkCauldronBlockEntity) blockEntity)
                : null;
    }
}
