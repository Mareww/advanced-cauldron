package com.marew.advancedcauldron.mixin;

import com.marew.advancedcauldron.duck.ICauldronTipped;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ArrowEntity.class)
public class ArrowEntityMixin implements ICauldronTipped {

    @Unique private boolean advancedcauldron$cauldronTipped = false;
    @Unique private Identifier advancedcauldron$tippedItemId = null;
    @Unique private Identifier advancedcauldron$potionId = null;
    @Unique private List<StatusEffectInstance> advancedcauldron$cauldronEffects = List.of();

    @Override
    public boolean advancedcauldron$isCauldronTipped() {
        return this.advancedcauldron$cauldronTipped;
    }

    @Override
    public Identifier advancedcauldron$getTippedItemId() {
        return this.advancedcauldron$tippedItemId;
    }

    @Override
    public Identifier advancedcauldron$getPotionId() {
        return this.advancedcauldron$potionId;
    }

    @Override
    public List<StatusEffectInstance> advancedcauldron$getCauldronEffects() {
        return this.advancedcauldron$cauldronEffects;
    }

    // When arrow entity is created from an item stack, check for cauldron-tipped marker
    @Inject(method = "initFromStack", at = @At("RETURN"))
    private void advancedcauldron$checkCauldronTipped(ItemStack stack, CallbackInfo ci) {
        List<StatusEffectInstance> customEffects = PotionUtil.getCustomPotionEffects(stack);
        if (!customEffects.isEmpty()) {
            this.advancedcauldron$cauldronTipped = true;
            this.advancedcauldron$tippedItemId = Registries.ITEM.getId(stack.getItem());
            Potion potion = PotionUtil.getPotion(stack);
            if (potion != Potions.EMPTY) {
                this.advancedcauldron$potionId = Registries.POTION.getId(potion);
            }
            this.advancedcauldron$cauldronEffects = new ArrayList<>(customEffects);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void advancedcauldron$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!this.advancedcauldron$cauldronTipped) return;
        nbt.putBoolean("AdvancedCauldronTipped", true);
        if (this.advancedcauldron$tippedItemId != null) {
            nbt.putString("AdvancedCauldronTippedItem", this.advancedcauldron$tippedItemId.toString());
        }
        if (this.advancedcauldron$potionId != null) {
            nbt.putString("AdvancedCauldronPotion", this.advancedcauldron$potionId.toString());
        }
        // Persist our pre-scaled effects so they survive chunk save/load
        if (!this.advancedcauldron$cauldronEffects.isEmpty()) {
            NbtList list = new NbtList();
            for (StatusEffectInstance effect : this.advancedcauldron$cauldronEffects) {
                list.add(effect.writeNbt(new NbtCompound()));
            }
            nbt.put("AdvancedCauldronEffects", list);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void advancedcauldron$readNbt(NbtCompound nbt, CallbackInfo ci) {
        this.advancedcauldron$cauldronTipped = nbt.getBoolean("AdvancedCauldronTipped");
        if (!this.advancedcauldron$cauldronTipped) return;
        if (nbt.contains("AdvancedCauldronTippedItem")) {
            this.advancedcauldron$tippedItemId = Identifier.tryParse(nbt.getString("AdvancedCauldronTippedItem"));
        }
        if (nbt.contains("AdvancedCauldronPotion")) {
            this.advancedcauldron$potionId = Identifier.tryParse(nbt.getString("AdvancedCauldronPotion"));
        }
        if (nbt.contains("AdvancedCauldronEffects")) {
            List<StatusEffectInstance> effects = new ArrayList<>();
            for (NbtElement element : nbt.getList("AdvancedCauldronEffects", NbtElement.COMPOUND_TYPE)) {
                StatusEffectInstance instance = StatusEffectInstance.fromNbt((NbtCompound) element);
                if (instance != null) effects.add(instance);
            }
            this.advancedcauldron$cauldronEffects = effects;
        }
    }
}
