package com.marew.advancedcauldron.block;

import com.marew.advancedcauldron.block.entity.FrozenCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.PotionCauldronBlockEntity;
import com.marew.advancedcauldron.compat.SereneSeasonsCompat;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

public class FrozenCauldronBlock extends AbstractCauldronBlock implements BlockEntityProvider {
    public static final MapCodec<FrozenCauldronBlock> CODEC = createCodec(
            settings -> new FrozenCauldronBlock(settings, ModBlocks.FROZEN_CAULDRON_BEHAVIOR)
    );
    public static final IntProperty LEVEL = Properties.LEVEL_3;

    @Override
    public MapCodec<? extends FrozenCauldronBlock> getCodec() {
        return CODEC;
    }

    public FrozenCauldronBlock(Settings settings, CauldronBehavior.CauldronBehaviorMap behaviorMap) {
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
        return new FrozenCauldronBlockEntity(pos, state);
    }

    @Override
    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
        if (precipitation != Biome.Precipitation.SNOW) return;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof FrozenCauldronBlockEntity frozenBE && "snow".equals(frozenBE.getSourceType())) {
            int level = state.get(LEVEL);
            if (level < 3) {
                world.setBlockState(pos, state.with(LEVEL, level + 1));
            }
        }
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (HeatSourceUtil.hasHeatSource(world, pos)) {
            thaw(state, world, pos);
            return;
        }

        boolean shouldThaw;
        if (SereneSeasonsCompat.isLoaded()) {
            shouldThaw = SereneSeasonsCompat.isWarmEnoughToThaw(world, pos);
        } else {
            shouldThaw = !world.getBiome(pos).value().isCold(pos);
        }

        if (shouldThaw) {
            thaw(state, world, pos);
            return;
        }

        // Snow accumulation for snow-filled cauldrons
        int level = state.get(LEVEL);
        if (level < 3 && world.isRaining()) {
            boolean isExposed = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos).getY() <= pos.getY() + 1;
            if (isExposed) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof FrozenCauldronBlockEntity frozenBE && "snow".equals(frozenBE.getSourceType())) {
                    world.setBlockState(pos, state.with(LEVEL, level + 1));
                }
            }
        }
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient && HeatSourceUtil.hasHeatSource(world, pos)) {
            thaw(state, world, pos);
        }
    }

    public static void freeze(World world, BlockPos pos, int level, String sourceType, int color,
                               @Nullable NbtCompound potionData, int tipsUsed) {
        world.setBlockState(pos, ModBlocks.FROZEN_CAULDRON.getDefaultState().with(LEVEL, level));
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof FrozenCauldronBlockEntity frozenBE) {
            frozenBE.setFrozenContents(sourceType, color, potionData, tipsUsed);
        }
    }

    private void thaw(BlockState state, World world, BlockPos pos) {
        int level = state.get(LEVEL);
        BlockEntity be = world.getBlockEntity(pos);
        String sourceType = "water";
        NbtCompound potionData = null;
        int tipsUsed = 0;

        if (be instanceof FrozenCauldronBlockEntity frozenBE) {
            sourceType = frozenBE.getSourceType();
            potionData = frozenBE.getPotionData();
            tipsUsed = frozenBE.getTipsUsed();
        }

        switch (sourceType) {
            case "potion" -> {
                world.setBlockState(pos, ModBlocks.POTION_CAULDRON.getDefaultState()
                        .with(PotionCauldronBlock.LEVEL, level));
                BlockEntity newBe = world.getBlockEntity(pos);
                if (newBe instanceof PotionCauldronBlockEntity potionBE) {
                    PotionContentsComponent contents = null;
                    if (potionData != null && potionData.contains("PotionContents")) {
                        contents = PotionContentsComponent.CODEC
                                .parse(world.getRegistryManager().getOps(NbtOps.INSTANCE),
                                        potionData.get("PotionContents"))
                                .result().orElse(null);
                    }
                    potionBE.restoreFromFrozen(contents, tipsUsed);
                }
            }
            case "milk" -> {
                world.setBlockState(pos, ModBlocks.MILK_CAULDRON.getDefaultState()
                        .with(MilkCauldronBlock.LEVEL, level));
            }
            default -> {
                world.setBlockState(pos, Blocks.WATER_CAULDRON.getDefaultState()
                        .with(LeveledCauldronBlock.LEVEL, level));
            }
        }

        world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 1.4F);
    }
}
