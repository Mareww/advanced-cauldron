package com.marew.advancedcauldron.block;

import com.marew.advancedcauldron.block.entity.MilkCauldronBlockEntity;
import com.marew.advancedcauldron.compat.SereneSeasonsCompat;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.registry.ModCauldronBehaviors;
import com.marew.advancedcauldron.util.HeatDamageUtil;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TippedArrowItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

public class MilkCauldronBlock extends AbstractCauldronBlock implements BlockEntityProvider {
    public static final MapCodec<MilkCauldronBlock> CODEC = createCodec(settings -> new MilkCauldronBlock(settings, ModBlocks.MILK_CAULDRON_BEHAVIOR));
    public static final IntProperty LEVEL = Properties.LEVEL_3;

    @Override
    public MapCodec<? extends MilkCauldronBlock> getCodec() {
        return CODEC;
    }

    public MilkCauldronBlock(Settings settings, CauldronBehavior.CauldronBehaviorMap behaviorMap) {
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
        if (precipitation == Biome.Precipitation.SNOW && world.getRandom().nextFloat() < 0.05f) {
            if (!HeatSourceUtil.hasHeatSource(world, pos)) {
                freezeMilk(world, pos, state);
            }
        }
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (HeatSourceUtil.hasHeatSource(world, pos)) return;

        boolean shouldFreeze;
        if (SereneSeasonsCompat.isLoaded()) {
            shouldFreeze = SereneSeasonsCompat.isColdEnoughToFreeze(world, pos);
        } else {
            shouldFreeze = world.getBiome(pos).value().isCold(pos);
        }

        if (shouldFreeze) {
            freezeMilk(world, pos, state);
        }
    }

    private void freezeMilk(World world, BlockPos pos, BlockState state) {
        int level = state.get(LEVEL);
        FrozenCauldronBlock.freeze(world, pos, level, "milk", 0xFFFFFF, null, 0);
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(LEVEL);
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Handle tipped arrow washing
        if (stack.getItem() instanceof TippedArrowItem) {
            return ModCauldronBehaviors.tryWashArrow(state, world, pos, player, hand, stack);
        }
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
            return ModCauldronBehaviors.tryDrinkMilk(state, world, pos, player);
        }
        return super.onUse(state, world, pos, player, hit);
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
