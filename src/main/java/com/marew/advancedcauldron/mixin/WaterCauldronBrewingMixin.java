package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.block.BrewingCauldronBlock;
import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.util.CauldronBrewingHelper;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCauldronBlock.class)
public class WaterCauldronBrewingMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void advancedcauldron$tryBrewInWaterCauldron(
            BlockState state, World world, BlockPos pos,
            PlayerEntity player, Hand hand, BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir) {

        if (!state.isOf(Blocks.WATER_CAULDRON)) return;
        if (!HeatSourceUtil.hasHeatSource(world, pos)) return;

        ItemStack stack = player.getStackInHand(hand);
        Potion result = CauldronBrewingHelper.getBrewResult(world, Potions.WATER, stack);
        if (result == null) return;

        if (!world.isClient) {
            int level = state.get(Properties.LEVEL_3);

            BlockState brewState = ModBlocks.BREWING_CAULDRON.getDefaultState()
                    .with(BrewingCauldronBlock.LEVEL, level);
            world.setBlockState(pos, brewState);

            if (world.getBlockEntity(pos) instanceof BrewingCauldronBlockEntity brewBE) {
                brewBE.tryAddIngredientFromPlayer(player, hand);
            }
        }

        cir.setReturnValue(ActionResult.success(world.isClient));
    }
}
