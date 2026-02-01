package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.block.BrewingCauldronBlock;
import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.util.CauldronBrewingHelper;
import com.marew.advancedcauldron.util.HeatDamageUtil;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeveledCauldronBlock.class)
public class WaterCauldronItemCollisionMixin {

    @Inject(method = "onEntityCollision", at = @At("HEAD"))
    private void advancedcauldron$catchItemInWaterCauldron(
            BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {

        if (world.isClient) return;

        HeatDamageUtil.tick(entity, world, pos);

        if (!(entity instanceof ItemEntity itemEntity)) return;
        if (!state.isOf(Blocks.WATER_CAULDRON)) return;
        if (itemEntity.age < ModConfig.get().itemPickupDelayTicks) return;
        if (!HeatSourceUtil.hasHeatSource(world, pos)) return;

        ItemStack stack = itemEntity.getStack();
        Potion result = CauldronBrewingHelper.getBrewResult(world, Potions.WATER, stack);
        if (result == null) return;

        int level = state.get(Properties.LEVEL_3);

        BlockState brewState = ModBlocks.BREWING_CAULDRON.getDefaultState()
                .with(BrewingCauldronBlock.LEVEL, level);
        world.setBlockState(pos, brewState);

        if (world.getBlockEntity(pos) instanceof BrewingCauldronBlockEntity brewBE) {
            brewBE.tryAddIngredient(itemEntity);
        }
    }
}
