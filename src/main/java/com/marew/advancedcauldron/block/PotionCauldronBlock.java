package com.marew.advancedcauldron.block;

import com.marew.advancedcauldron.block.entity.PotionCauldronBlockEntity;
import com.marew.advancedcauldron.compat.SereneSeasonsCompat;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.registry.ModCauldronBehaviors;
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
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

public class PotionCauldronBlock extends AbstractCauldronBlock implements BlockEntityProvider {
    public static final MapCodec<PotionCauldronBlock> CODEC = createCodec(settings -> new PotionCauldronBlock(settings, ModBlocks.POTION_CAULDRON_BEHAVIOR));
    public static final IntProperty LEVEL = Properties.LEVEL_3;

    @Override
    public MapCodec<? extends PotionCauldronBlock> getCodec() {
        return CODEC;
    }

    public PotionCauldronBlock(Settings settings, CauldronBehavior.CauldronBehaviorMap behaviorMap) {
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
        return new PotionCauldronBlockEntity(pos, state);
    }

    public static void decrementLevel(BlockState state, World world, BlockPos pos) {
        decrementLevels(state, world, pos, 1);
    }

    public static void decrementLevels(BlockState state, World world, BlockPos pos, int amount) {
        int level = state.get(LEVEL) - amount;
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
        if (world.getRandom().nextFloat() >= 0.05f) return;

        if (precipitation == Biome.Precipitation.SNOW) {
            if (HeatSourceUtil.hasHeatSource(world, pos)) {
                if (state.get(LEVEL) < 3) {
                    world.setBlockState(pos, state.with(LEVEL, state.get(LEVEL) + 1));
                }
            } else {
                freezePotion(world, pos, state);
            }
        } else if (precipitation == Biome.Precipitation.RAIN) {
            if (state.get(LEVEL) < 3) {
                world.setBlockState(pos, state.with(LEVEL, state.get(LEVEL) + 1));
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
            freezePotion(world, pos, state);
        }
    }

    private void freezePotion(World world, BlockPos pos, BlockState state) {
        int level = state.get(LEVEL);
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof PotionCauldronBlockEntity potionBE) {
            PotionContentsComponent contents = potionBE.getPotionContents();
            int color = contents != null ? contents.getColor() : 0x3F76E4;
            NbtCompound potionData = new NbtCompound();
            if (contents != null) {
                PotionContentsComponent.CODEC
                        .encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), contents)
                        .result()
                        .ifPresent(encoded -> potionData.put("PotionContents", encoded));
            }
            FrozenCauldronBlock.freeze(world, pos, level, "potion", color, potionData, potionBE.getTipsUsed());
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Handle empty-hand drinking
        if (player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
            return ModCauldronBehaviors.tryDrinkFromPotionCauldron(state, world, pos, player);
        }
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        CauldronBehavior behavior = this.behaviorMap.map().get(stack.getItem());
        if (behavior != null) {
            ItemActionResult result = behavior.interact(state, world, pos, player, hand, stack);
            if (result != ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                return result;
            }
        }

        // Then try arrow tipping for any arrow-like item
        return ModCauldronBehaviors.tryTipArrow(state, world, pos, player, hand, stack);
    }
}
