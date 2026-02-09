package com.marew.advancedcauldron.block;

import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Map;

public class FrozenCauldronBlock extends AbstractCauldronBlock {
    public static final IntProperty LEVEL = Properties.LEVEL_3;

    public FrozenCauldronBlock(Settings settings, Map<Item, CauldronBehavior> behaviorMap) {
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
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(LEVEL);
    }

    @Override
    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (HeatSourceUtil.hasHeatSource(world, pos)) {
            thaw(state, world, pos);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient && HeatSourceUtil.hasHeatSource(world, pos)) {
            thaw(state, world, pos);
        }
    }

    private void thaw(BlockState state, World world, BlockPos pos) {
        int level = state.get(LEVEL);
        world.setBlockState(pos, Blocks.WATER_CAULDRON.getDefaultState()
                .with(LeveledCauldronBlock.LEVEL, level));
        world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 1.4F);
    }
}
