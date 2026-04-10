package com.marew.advancedcauldron.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileEntityMixin {

    /**
     * Intercept the asItemStack() call inside tryPickup so we can attach
     * CauldronData to the picked-up item. This is more reliable than injecting
     * inside asItemStack() itself, because asItemStack() reconstructs the stack
     * from scratch (losing all custom NBT), and the @Redirect operates at the
     * call site where we can inspect the live entity state via ICauldronTipped.
     */
    @Redirect(
            method = "tryPickup",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;asItemStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack advancedcauldron$addCauldronDataOnPickup(PersistentProjectileEntity entity) {
        ItemStack stack = entity.asItemStack();
        if (entity instanceof ICauldronTipped tipped && tipped.advancedcauldron$isCauldronTipped()) {
            NbtCompound cauldronData = new NbtCompound();
            cauldronData.putBoolean("CauldronTipped", true);
            stack.getOrCreateNbt().put("CauldronData", cauldronData);
        }
        return stack;
    }
}
