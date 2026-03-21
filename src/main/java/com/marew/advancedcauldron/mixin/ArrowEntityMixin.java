package com.marew.advancedcauldron.mixin;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
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

    @Inject(method = "onHit(Lnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"))
    private void advancedcauldron$updateCauldronFlag(LivingEntity target, CallbackInfo ci) {
        ItemStack stack = ((PersistentProjectileEntity) (Object) this).getItemStack();
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
