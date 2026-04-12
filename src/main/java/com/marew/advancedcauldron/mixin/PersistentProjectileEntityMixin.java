package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.duck.ICauldronTipped;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileEntityMixin {

    @Redirect(
            method = "tryPickup",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;asItemStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack advancedcauldron$handleCauldronPickup(PersistentProjectileEntity entity) {
        ItemStack stack = entity.asItemStack();
        if (!(entity instanceof ICauldronTipped tipped) || !tipped.advancedcauldron$isCauldronTipped()) {
            return stack;
        }

        if (!ModConfig.get().cauldronArrowPickupRestoresEffect) {
            // Config disabled: return the original plain arrow (no potion effects)
            Item plainArrow = Items.ARROW;
            Identifier originalId = tipped.advancedcauldron$getOriginalArrowId();
            if (originalId != null) {
                Item found = Registries.ITEM.get(originalId);
                if (found != Items.AIR) plainArrow = found;
            }
            return new ItemStack(plainArrow, stack.getCount());
        }

        List<StatusEffectInstance> effects = tipped.advancedcauldron$getCauldronEffects();
        Identifier tippedItemId = tipped.advancedcauldron$getTippedItemId();
        Item tippedItem = tippedItemId != null ? Registries.ITEM.get(tippedItemId) : null;

        // If the entity's asItemStack() returned the wrong item type (e.g. pnkus returning
        // plain arrow instead of tipped variant), rebuild the stack with the correct item.
        if (tippedItem != null && tippedItem != Items.AIR && stack.getItem() != tippedItem) {
            ItemStack corrected = new ItemStack(tippedItem, stack.getCount());
            Potion potion = PotionUtil.getPotion(stack);
            if (potion == Potions.EMPTY) {
                // asItemStack() didn't carry the Potion tag (e.g. pnkus plain arrow) — use stored ID
                Identifier potionId = tipped.advancedcauldron$getPotionId();
                if (potionId != null) {
                    potion = Registries.POTION.get(potionId);
                }
            }
            if (potion != null && potion != Potions.EMPTY) {
                PotionUtil.setPotion(corrected, potion);
            }
            if (!effects.isEmpty()) {
                PotionUtil.setCustomPotionEffects(corrected, effects);
            }
            return corrected;
        }

        // Correct item type — just ensure CustomPotionEffects are present
        if (PotionUtil.getCustomPotionEffects(stack).isEmpty() && !effects.isEmpty()) {
            PotionUtil.setCustomPotionEffects(stack, effects);
        }
        return stack;
    }
}
