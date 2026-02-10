package com.marew.advancedcauldron.block;

import com.marew.advancedcauldron.block.entity.PotionCauldronBlockEntity;
import com.marew.advancedcauldron.compat.SereneSeasonsCompat;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.registry.ModCauldronBehaviors;
import com.marew.advancedcauldron.util.HeatDamageUtil;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PotionCauldronBlock extends AbstractCauldronBlock implements BlockEntityProvider {
    public static final IntProperty LEVEL = Properties.LEVEL_3;

    public PotionCauldronBlock(Settings settings, Map<Item, CauldronBehavior> behaviorMap) {
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
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
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
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
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
            Potion potion = potionBE.getPotion();
            int color = potion != null ? PotionUtil.getColor(potion.getEffects()) : 0x3F76E4;
            NbtCompound potionData = new NbtCompound();
            if (potion != null) {
                Identifier id = Registries.POTION.getId(potion);
                potionData.putString("PotionId", id.toString());
            }
            FrozenCauldronBlock.freeze(world, pos, level, "potion", color, potionData, potionBE.getTipsUsed());
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        CauldronBehavior behavior = this.behaviorMap.get(stack.getItem());
        if (behavior != null) {
            ActionResult result = behavior.interact(state, world, pos, player, hand, stack);
            if (result != ActionResult.PASS) {
                return result;
            }
        }

        return ModCauldronBehaviors.tryTipArrow(state, world, pos, player, hand, stack);
    }
}
