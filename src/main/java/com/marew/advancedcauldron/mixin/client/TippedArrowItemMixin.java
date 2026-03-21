package com.marew.advancedcauldron.mixin.client;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TippedArrowItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TippedArrowItem.class)
public class TippedArrowItemMixin {

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void advancedcauldron$fixCauldronTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null && customData.copyNbt().getBoolean("CauldronTipped")) {
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents != null) {
                contents.buildTooltip(tooltip::add, 0.25F, context.getUpdateTickRate());
            }
            ci.cancel();
        }
    }
}
