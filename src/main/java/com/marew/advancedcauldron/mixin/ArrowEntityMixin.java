package com.marew.advancedcauldron.mixin;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import com.marew.advancedcauldron.duck.ICauldronTipped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowEntity.class)
public class ArrowEntityMixin implements ICauldronTipped {

    @Unique
    private boolean advancedcauldron$cauldronTipped = false;

    @Override
    public boolean advancedcauldron$isCauldronTipped() {
        return this.advancedcauldron$cauldronTipped;
    }

    // Set flag when arrow entity is created from an item stack (bow shot)
    @Inject(method = "initFromStack", at = @At("RETURN"))
    private void advancedcauldron$checkCauldronTipped(ItemStack stack, CallbackInfo ci) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("CauldronData")) {
            this.advancedcauldron$cauldronTipped = nbt.getCompound("CauldronData").getBoolean("CauldronTipped");
        }
    }

    // Persist flag so it survives chunk save/load
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void advancedcauldron$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.advancedcauldron$cauldronTipped) {
            nbt.putBoolean("AdvancedCauldronTipped", true);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void advancedcauldron$readNbt(NbtCompound nbt, CallbackInfo ci) {
        this.advancedcauldron$cauldronTipped = nbt.getBoolean("AdvancedCauldronTipped");
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
