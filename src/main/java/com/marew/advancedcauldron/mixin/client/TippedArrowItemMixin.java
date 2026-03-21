package com.marew.advancedcauldron.mixin.client;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TippedArrowItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TippedArrowItem.class)
public class TippedArrowItemMixin {

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void advancedcauldron$fixCauldronTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("CauldronData") && nbt.getCompound("CauldronData").getBoolean("CauldronTipped")) {
            PotionUtil.buildTooltip(stack, tooltip, 0.25F);
            ci.cancel();
        }
    }
}
