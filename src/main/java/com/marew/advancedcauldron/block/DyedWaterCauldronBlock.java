package com.marew.advancedcauldron.block;

import com.marew.advancedcauldron.block.entity.DyedWaterCauldronBlockEntity;
import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.util.HeatDamageUtil;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

public class DyedWaterCauldronBlock extends AbstractCauldronBlock implements BlockEntityProvider {
    public static final MapCodec<DyedWaterCauldronBlock> CODEC = createCodec(settings -> new DyedWaterCauldronBlock(settings, ModBlocks.DYED_WATER_CAULDRON_BEHAVIOR));
    public static final IntProperty LEVEL = Properties.LEVEL_3;

    @Override
    public MapCodec<? extends DyedWaterCauldronBlock> getCodec() {
        return CODEC;
    }

    public DyedWaterCauldronBlock(Settings settings, CauldronBehavior.CauldronBehaviorMap behaviorMap) {
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

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DyedWaterCauldronBlockEntity(pos, state);
    }

    public static void decrementLevel(BlockState state, World world, BlockPos pos) {
        int level = state.get(LEVEL) - 1;
        if (level <= 0) {
            world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
        } else {
            world.setBlockState(pos, state.with(LEVEL, level));
        }
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) {
            HeatDamageUtil.tick(entity, world, pos);
        }
    }

    @Override
    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
        if (state.get(LEVEL) < 3 && world.getRandom().nextFloat() < (float) ModConfig.get().freezeChance) {
            if (precipitation == Biome.Precipitation.RAIN
                    || (precipitation == Biome.Precipitation.SNOW && HeatSourceUtil.hasHeatSource(world, pos))) {
                world.setBlockState(pos, state.with(LEVEL, state.get(LEVEL) + 1));
            }
        }
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        CauldronBehavior behavior = this.behaviorMap.map().get(stack.getItem());
        if (behavior != null) {
            return behavior.interact(state, world, pos, player, hand, stack);
        }
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
