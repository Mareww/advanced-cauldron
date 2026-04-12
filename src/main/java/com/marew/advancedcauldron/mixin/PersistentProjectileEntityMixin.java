package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.config.ModConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileEntityMixin {

    @Redirect(
            method = "tryPickup",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;asItemStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack advancedcauldron$handleCauldronPickup(PersistentProjectileEntity entity) {
        ItemStack stack = entity.asItemStack();

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null || !customData.copyNbt().getBoolean("CauldronTipped")) {
            return stack;
        }

        if (!ModConfig.get().cauldronArrowPickupRestoresEffect) {
            // Strip the CauldronTipped flag so the arrow reverts to vanilla D/8 on next shot
            NbtCompound nbt = customData.copyNbt();
            nbt.remove("CauldronTipped");
            if (nbt.isEmpty()) {
                stack.remove(DataComponentTypes.CUSTOM_DATA);
            } else {
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            }
        }

        return stack;
    }
}
