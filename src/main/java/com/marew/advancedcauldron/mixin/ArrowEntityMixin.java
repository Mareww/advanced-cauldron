package com.marew.advancedcauldron.mixin;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowEntity.class)
public class ArrowEntityMixin {

    @Unique
    private boolean advancedcauldron$cauldronTipped = false;

    // ArrowEntity overrides setStack, so this injection is valid.
    // Called when the entity is created from an item OR when loaded from NBT
    // (PersistentProjectileEntity.readCustomDataFromNbt calls setStack with the decoded item).
    @Inject(method = "setStack", at = @At("HEAD"))
    private void advancedcauldron$onSetStack(ItemStack stack, CallbackInfo ci) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        this.advancedcauldron$cauldronTipped = customData != null && customData.copyNbt().getBoolean("CauldronTipped");
    }

    @Redirect(
            method = "onHit(Lnet/minecraft/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/effect/StatusEffectInstance;mapDuration(Lit/unimi/dsi/fastutil/ints/Int2IntFunction;)I")
    )
    private int advancedcauldron$scaleArrowDuration(StatusEffectInstance instance, Int2IntFunction function) {
        if (this.advancedcauldron$cauldronTipped) {
            return instance.mapDuration(d -> d / 4);
        }
        return instance.mapDuration(function);
    }
}
