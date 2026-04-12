package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.config.ModConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
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
            // Return a plain (un-tipped) arrow — use OriginalArrow if stored, else vanilla arrow
            NbtCompound nbt = customData.copyNbt();
            Item plainArrow = Items.ARROW;
            if (nbt.contains("OriginalArrow")) {
                Identifier id = Identifier.tryParse(nbt.getString("OriginalArrow"));
                if (id != null) {
                    Item found = Registries.ITEM.get(id);
                    if (found != Items.AIR) plainArrow = found;
                }
            }
            return new ItemStack(plainArrow, stack.getCount());
        }

        return stack;
    }
}
