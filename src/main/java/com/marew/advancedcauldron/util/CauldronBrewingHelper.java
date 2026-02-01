package com.marew.advancedcauldron.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CauldronBrewingHelper {

    @Nullable
    public static PotionContentsComponent getBrewResult(World world, PotionContentsComponent currentPotion, ItemStack ingredient) {
        if (ingredient.isEmpty()) return null;

        BrewingRecipeRegistry registry = world.getBrewingRecipeRegistry();

        ItemStack inputBottle = new ItemStack(Items.POTION);
        inputBottle.set(DataComponentTypes.POTION_CONTENTS, currentPotion);

        if (!registry.hasRecipe(inputBottle, ingredient)) {
            return null;
        }

        ItemStack result = registry.craft(ingredient, inputBottle);
        if (result.isEmpty()) return null;

        return result.get(DataComponentTypes.POTION_CONTENTS);
    }

    public static boolean isBrewingIngredient(World world, ItemStack stack) {
        BrewingRecipeRegistry registry = world.getBrewingRecipeRegistry();
        return registry.isValidIngredient(stack);
    }
}
