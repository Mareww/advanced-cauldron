package com.marew.advancedcauldron.duck;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;

import java.util.List;

public interface ICauldronTipped {
    boolean advancedcauldron$isCauldronTipped();
    Identifier advancedcauldron$getTippedItemId();
    Identifier advancedcauldron$getOriginalArrowId();
    Identifier advancedcauldron$getPotionId();
    List<StatusEffectInstance> advancedcauldron$getCauldronEffects();
}
