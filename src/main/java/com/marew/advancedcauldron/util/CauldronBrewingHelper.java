package com.marew.advancedcauldron.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CauldronBrewingHelper {

    @Nullable
    public static Potion getBrewResult(World world, Potion currentPotion, ItemStack ingredient) {
        if (ingredient.isEmpty()) return null;

        ItemStack inputBottle = new ItemStack(Items.POTION);
        PotionUtil.setPotion(inputBottle, currentPotion);

        if (!BrewingRecipeRegistry.hasRecipe(inputBottle, ingredient)) {
            return null;
        }

        ItemStack result = BrewingRecipeRegistry.craft(ingredient, inputBottle);
        if (result.isEmpty()) return null;

        return PotionUtil.getPotion(result);
    }

    public static boolean isBrewingIngredient(World world, ItemStack stack) {
        return BrewingRecipeRegistry.isValidIngredient(stack);
    }
}
