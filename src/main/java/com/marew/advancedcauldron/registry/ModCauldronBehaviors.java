package com.marew.advancedcauldron.registry;

import com.marew.advancedcauldron.AdvancedCauldron;
import com.marew.advancedcauldron.block.BrewingCauldronBlock;
import com.marew.advancedcauldron.block.DyedWaterCauldronBlock;
import com.marew.advancedcauldron.block.FrozenCauldronBlock;
import com.marew.advancedcauldron.block.MilkCauldronBlock;
import com.marew.advancedcauldron.block.PotionCauldronBlock;
import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.DyedWaterCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.FrozenCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.PotionCauldronBlockEntity;
import com.marew.advancedcauldron.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.DyedColorComponent;
import com.marew.advancedcauldron.block.entity.MilkCauldronBlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.world.event.GameEvent;

import java.util.Map;
import java.util.Optional;

public class ModCauldronBehaviors {
    public static final TagKey<Item> TIPPABLE_ARROWS = TagKey.of(
            Registries.ITEM.getKey(),
            Identifier.of(AdvancedCauldron.MOD_ID, "tippable_arrows")
    );

    public static void register() {
        registerEmptyCauldronBehaviors();
        registerWaterCauldronBehaviors();
        registerPotionCauldronBehaviors();
        registerDyedWaterCauldronBehaviors();
        registerBrewingCauldronBehaviors();
        registerMilkCauldronBehaviors();
        registerFrozenCauldronBehaviors();
    }

    // ==================== WATER CAULDRON BEHAVIORS ====================

    private static void registerWaterCauldronBehaviors() {
        Map<Item, CauldronBehavior> waterMap = CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map();

        // Empty splash bottle -> fill with splash water bottle
        waterMap.put(ModItems.EMPTY_SPLASH_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                ItemStack splashBottle = new ItemStack(Items.SPLASH_POTION);
                splashBottle.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.WATER));

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                if (stack.isEmpty()) {
                    player.setStackInHand(hand, splashBottle);
                } else if (!player.getInventory().insertStack(splashBottle)) {
                    player.dropItem(splashBottle, false);
                }

                player.incrementStat(Stats.USE_CAULDRON);
                LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Empty lingering bottle -> fill with lingering water bottle
        waterMap.put(ModItems.EMPTY_LINGERING_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                ItemStack lingeringBottle = new ItemStack(Items.LINGERING_POTION);
                lingeringBottle.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.WATER));

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                if (stack.isEmpty()) {
                    player.setStackInHand(hand, lingeringBottle);
                } else if (!player.getInventory().insertStack(lingeringBottle)) {
                    player.dropItem(lingeringBottle, false);
                }

                player.incrementStat(Stats.USE_CAULDRON);
                LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Milk bucket -> clear player effects and drain 1 level
        waterMap.put(Items.MILK_BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                player.clearStatusEffects();

                int currentLevel = state.get(LeveledCauldronBlock.LEVEL);
                if (currentLevel <= 1) {
                    world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                } else {
                    world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, currentLevel - 1));
                }

                if (!player.getAbilities().creativeMode) {
                    player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                }

                player.incrementStat(Stats.USE_CAULDRON);
                world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Dye items into water cauldron -> creates dyed water cauldron
        for (DyeColor color : DyeColor.values()) {
            Item dyeItem = getDyeItem(color);
            if (dyeItem == null) continue;

            waterMap.put(dyeItem, (state, world, pos, player, hand, stack) -> {
                if (!world.isClient) {
                    int currentLevel = state.get(LeveledCauldronBlock.LEVEL);

                    BlockState dyedState = ModBlocks.DYED_WATER_CAULDRON.getDefaultState()
                            .with(DyedWaterCauldronBlock.LEVEL, currentLevel);
                    world.setBlockState(pos, dyedState);

                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof DyedWaterCauldronBlockEntity dyedBE) {
                        dyedBE.setColor(color.getEntityColor());
                    }

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                }
                return ItemActionResult.success(world.isClient);
            });
        }
    }

    // ==================== EMPTY CAULDRON BEHAVIORS ====================

    private static void registerEmptyCauldronBehaviors() {
        Map<Item, CauldronBehavior> emptyMap = CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.map();

        // Milk bucket -> fill cauldron with milk
        emptyMap.put(Items.MILK_BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                world.setBlockState(pos, ModBlocks.MILK_CAULDRON.getDefaultState()
                        .with(MilkCauldronBlock.LEVEL, 3));

                if (!player.getAbilities().creativeMode) {
                    player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                }

                player.incrementStat(Stats.FILL_CAULDRON);
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        emptyMap.put(Items.POTION, (state, world, pos, player, hand, stack) -> {
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            Optional<RegistryEntry<Potion>> potionOpt = contents.potion();
            if (potionOpt.isEmpty() || potionOpt.get() == Potions.WATER) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!world.isClient) {
                BlockState potionState = ModBlocks.POTION_CAULDRON.getDefaultState()
                        .with(PotionCauldronBlock.LEVEL, 1);
                world.setBlockState(pos, potionState);

                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    potionBE.setPotionContents(contents);
                }

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                    ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, bottle);
                    } else if (!player.getInventory().insertStack(bottle)) {
                        player.dropItem(bottle, false);
                    }
                }

                player.incrementStat(Stats.USE_CAULDRON);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        emptyMap.put(Items.SPLASH_POTION, (state, world, pos, player, hand, stack) -> {
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            Optional<RegistryEntry<Potion>> potionOpt = contents.potion();
            if (potionOpt.isEmpty() || potionOpt.get() == Potions.WATER) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!world.isClient) {
                BlockState potionState = ModBlocks.POTION_CAULDRON.getDefaultState()
                        .with(PotionCauldronBlock.LEVEL, 1);
                world.setBlockState(pos, potionState);

                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    potionBE.setPotionContents(contents);
                }

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                    ItemStack emptyBottle = new ItemStack(ModItems.EMPTY_SPLASH_BOTTLE);
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, emptyBottle);
                    } else if (!player.getInventory().insertStack(emptyBottle)) {
                        player.dropItem(emptyBottle, false);
                    }
                }

                player.incrementStat(Stats.USE_CAULDRON);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        emptyMap.put(Items.LINGERING_POTION, (state, world, pos, player, hand, stack) -> {
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            Optional<RegistryEntry<Potion>> potionOpt = contents.potion();
            if (potionOpt.isEmpty() || potionOpt.get() == Potions.WATER) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!world.isClient) {
                BlockState potionState = ModBlocks.POTION_CAULDRON.getDefaultState()
                        .with(PotionCauldronBlock.LEVEL, 1);
                world.setBlockState(pos, potionState);

                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    potionBE.setPotionContents(contents);
                }

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                    ItemStack emptyBottle = new ItemStack(ModItems.EMPTY_LINGERING_BOTTLE);
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, emptyBottle);
                    } else if (!player.getInventory().insertStack(emptyBottle)) {
                        player.dropItem(emptyBottle, false);
                    }
                }

                player.incrementStat(Stats.USE_CAULDRON);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return ItemActionResult.success(world.isClient);
        });
    }

    // ==================== POTION CAULDRON BEHAVIORS ====================

    private static void registerPotionCauldronBehaviors() {
        Map<Item, CauldronBehavior> potionMap = ModBlocks.POTION_CAULDRON_BEHAVIOR.map();

        // Glass bottle -> extract potion from cauldron (no bonus, can be poured back in)
        potionMap.put(Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    PotionContentsComponent storedContents = potionBE.getPotionContents();
                    if (storedContents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    ItemStack potionStack = new ItemStack(Items.POTION);
                    potionStack.set(DataComponentTypes.POTION_CONTENTS, storedContents);

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, potionStack);
                    } else if (!player.getInventory().insertStack(potionStack)) {
                        player.dropItem(potionStack, false);
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    PotionCauldronBlock.decrementLevel(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Milk bucket -> clear player effects and drain 1 level
        potionMap.put(Items.MILK_BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                player.clearStatusEffects();

                int currentLevel = state.get(PotionCauldronBlock.LEVEL);
                if (currentLevel <= 1) {
                    world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                } else {
                    world.setBlockState(pos, state.with(PotionCauldronBlock.LEVEL, currentLevel - 1));
                }

                if (!player.getAbilities().creativeMode) {
                    player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                }

                player.incrementStat(Stats.USE_CAULDRON);
                world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Empty splash bottle -> fill with splash potion (no bonus)
        potionMap.put(ModItems.EMPTY_SPLASH_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    PotionContentsComponent storedContents = potionBE.getPotionContents();
                    if (storedContents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    ItemStack splashPotion = new ItemStack(Items.SPLASH_POTION);
                    splashPotion.set(DataComponentTypes.POTION_CONTENTS, storedContents);

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, splashPotion);
                    } else if (!player.getInventory().insertStack(splashPotion)) {
                        player.dropItem(splashPotion, false);
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    PotionCauldronBlock.decrementLevel(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Empty lingering bottle -> fill with lingering potion (no bonus)
        potionMap.put(ModItems.EMPTY_LINGERING_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    PotionContentsComponent storedContents = potionBE.getPotionContents();
                    if (storedContents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    ItemStack lingeringPotion = new ItemStack(Items.LINGERING_POTION);
                    lingeringPotion.set(DataComponentTypes.POTION_CONTENTS, storedContents);

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, lingeringPotion);
                    } else if (!player.getInventory().insertStack(lingeringPotion)) {
                        player.dropItem(lingeringPotion, false);
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    PotionCauldronBlock.decrementLevel(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Potion bottle -> add more of the SAME potion to the cauldron
        potionMap.put(Items.POTION, (state, world, pos, player, hand, stack) -> {
            int level = state.get(PotionCauldronBlock.LEVEL);
            if (level >= 3) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            Optional<RegistryEntry<Potion>> incomingOpt = contents.potion();
            if (incomingOpt.isEmpty() || incomingOpt.get() == Potions.WATER) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            RegistryEntry<Potion> incomingPotion = incomingOpt.get();

            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    if (potionBE.getPotion() != incomingPotion) {
                        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    }

                    world.setBlockState(pos, state.with(PotionCauldronBlock.LEVEL, level + 1));

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                        if (stack.isEmpty()) {
                            player.setStackInHand(hand, bottle);
                        } else if (!player.getInventory().insertStack(bottle)) {
                            player.dropItem(bottle, false);
                        }
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Splash potion -> pour back into cauldron
        potionMap.put(Items.SPLASH_POTION, (state, world, pos, player, hand, stack) -> {
            int level = state.get(PotionCauldronBlock.LEVEL);
            if (level >= 3) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            Optional<RegistryEntry<Potion>> incomingOpt = contents.potion();
            if (incomingOpt.isEmpty() || incomingOpt.get() == Potions.WATER) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            RegistryEntry<Potion> incomingPotion = incomingOpt.get();

            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    if (potionBE.getPotion() != incomingPotion) {
                        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    }

                    world.setBlockState(pos, state.with(PotionCauldronBlock.LEVEL, level + 1));

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                        ItemStack emptyBottle = new ItemStack(ModItems.EMPTY_SPLASH_BOTTLE);
                        if (stack.isEmpty()) {
                            player.setStackInHand(hand, emptyBottle);
                        } else if (!player.getInventory().insertStack(emptyBottle)) {
                            player.dropItem(emptyBottle, false);
                        }
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Lingering potion -> pour back into cauldron
        potionMap.put(Items.LINGERING_POTION, (state, world, pos, player, hand, stack) -> {
            int level = state.get(PotionCauldronBlock.LEVEL);
            if (level >= 3) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            Optional<RegistryEntry<Potion>> incomingOpt = contents.potion();
            if (incomingOpt.isEmpty() || incomingOpt.get() == Potions.WATER) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            RegistryEntry<Potion> incomingPotion = incomingOpt.get();

            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    if (potionBE.getPotion() != incomingPotion) {
                        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    }

                    world.setBlockState(pos, state.with(PotionCauldronBlock.LEVEL, level + 1));

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                        ItemStack emptyBottle = new ItemStack(ModItems.EMPTY_LINGERING_BOTTLE);
                        if (stack.isEmpty()) {
                            player.setStackInHand(hand, emptyBottle);
                        } else if (!player.getInventory().insertStack(emptyBottle)) {
                            player.dropItem(emptyBottle, false);
                        }
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Bucket empties the potion cauldron
        potionMap.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Block leather armor from interacting with potion cauldrons
        CauldronBehavior noOp = (state, world, pos, player, hand, stack) -> ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        potionMap.put(Items.LEATHER_HELMET, noOp);
        potionMap.put(Items.LEATHER_CHESTPLATE, noOp);
        potionMap.put(Items.LEATHER_LEGGINGS, noOp);
        potionMap.put(Items.LEATHER_BOOTS, noOp);
        potionMap.put(Items.LEATHER_HORSE_ARMOR, noOp);
    }

    // ==================== DYED WATER CAULDRON BEHAVIORS ====================

    private static void registerDyedWaterCauldronBehaviors() {
        Map<Item, CauldronBehavior> dyedMap = ModBlocks.DYED_WATER_CAULDRON_BEHAVIOR.map();

        // Glass bottle -> extract dyed water (gives water bottle)
        dyedMap.put(Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                ItemStack waterBottle = new ItemStack(Items.POTION);
                waterBottle.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.WATER));

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                if (stack.isEmpty()) {
                    player.setStackInHand(hand, waterBottle);
                } else if (!player.getInventory().insertStack(waterBottle)) {
                    player.dropItem(waterBottle, false);
                }

                player.incrementStat(Stats.USE_CAULDRON);
                DyedWaterCauldronBlock.decrementLevel(state, world, pos);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Additional dyes -> mix color
        for (DyeColor color : DyeColor.values()) {
            Item dyeItem = getDyeItem(color);
            if (dyeItem == null) continue;

            dyedMap.put(dyeItem, (state, world, pos, player, hand, stack) -> {
                if (!world.isClient) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof DyedWaterCauldronBlockEntity dyedBE) {
                        dyedBE.mixColor(color);

                        if (!player.getAbilities().creativeMode) {
                            stack.decrement(1);
                        }

                        world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                    }
                }
                return ItemActionResult.success(world.isClient);
            });
        }

        // Bucket empties the dyed water cauldron
        dyedMap.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Dyeable items (leather armor) -> dye them in the cauldron
        registerDyeableItemBehavior(dyedMap, Items.LEATHER_HELMET);
        registerDyeableItemBehavior(dyedMap, Items.LEATHER_CHESTPLATE);
        registerDyeableItemBehavior(dyedMap, Items.LEATHER_LEGGINGS);
        registerDyeableItemBehavior(dyedMap, Items.LEATHER_BOOTS);
        registerDyeableItemBehavior(dyedMap, Items.LEATHER_HORSE_ARMOR);
    }

    private static void registerDyeableItemBehavior(Map<Item, CauldronBehavior> map, Item item) {
        map.put(item, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof DyedWaterCauldronBlockEntity dyedBE) {
                    stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(dyedBE.getColor(), true));
                    player.incrementStat(Stats.USE_CAULDRON);
                    DyedWaterCauldronBlock.decrementLevel(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });
    }

    // ==================== ARROW TIPPING LOGIC ====================

    public static ItemActionResult tryTipArrow(BlockState state, World world, BlockPos pos,
                                            PlayerEntity player, Hand hand, ItemStack stack) {
        if (!(state.getBlock() instanceof PotionCauldronBlock) && !(state.getBlock() instanceof BrewingCauldronBlock)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        Item item = stack.getItem();

        boolean isTippable = stack.isIn(TIPPABLE_ARROWS) || item instanceof ArrowItem;

        if (!isTippable) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.contains(DataComponentTypes.POTION_CONTENTS)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            PotionContentsComponent storedContents = null;
            int currentLevel;
            int tipsRemaining;

            if (be instanceof PotionCauldronBlockEntity potionBE) {
                storedContents = potionBE.getPotionContents();
                currentLevel = state.get(PotionCauldronBlock.LEVEL);
                tipsRemaining = potionBE.getTipsRemaining();
            } else if (be instanceof BrewingCauldronBlockEntity brewBE) {
                if (brewBE.isBrewing()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                storedContents = brewBE.getCurrentPotion();
                currentLevel = state.get(BrewingCauldronBlock.LEVEL);
                tipsRemaining = brewBE.getTipsRemaining();
            } else {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (storedContents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            // Tip up to remaining capacity in current level, capped by stack size
            int arrowsPerLevel = ModConfig.get().arrowsPerCauldronLevel;
            int totalAvailable = ((currentLevel - 1) * arrowsPerLevel) + tipsRemaining;
            int toTip = Math.min(Math.min(stack.getCount(), arrowsPerLevel), totalAvailable);

            if (toTip <= 0) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            // Determine the tipped arrow item
            Item tippedItem = Items.TIPPED_ARROW;
            NbtCompound customNbt = new NbtCompound();
            customNbt.putBoolean("CauldronTipped", true);

            if (item != Items.ARROW) {
                Identifier arrowId = Registries.ITEM.getId(item);
                customNbt.putString("OriginalArrow", arrowId.toString());

                String namespace = arrowId.getNamespace();
                String path = arrowId.getPath();
                Identifier tippedVariantId = Identifier.of(namespace, "tipped_" + path);
                Item tippedVariant = Registries.ITEM.get(tippedVariantId);

                if (tippedVariant != Items.AIR && tippedVariant != Items.TIPPED_ARROW) {
                    tippedItem = tippedVariant;
                }
            }

            // Create the tipped arrows with the exact same potion contents
            ItemStack tippedArrows = new ItemStack(tippedItem, toTip);
            tippedArrows.set(DataComponentTypes.POTION_CONTENTS, storedContents);
            tippedArrows.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customNbt));

            // Consume arrows
            if (!player.getAbilities().creativeMode) {
                stack.decrement(toTip);
            }

            // Give the tipped arrows
            if (!player.getInventory().insertStack(tippedArrows)) {
                player.dropItem(tippedArrows, false);
            }

            // Consume tips and drain levels
            if (be instanceof PotionCauldronBlockEntity potionBE) {
                int levelsDrained = potionBE.consumeTips(toTip);
                if (levelsDrained > 0) {
                    PotionCauldronBlock.decrementLevels(state, world, pos, levelsDrained);
                }
            } else if (be instanceof BrewingCauldronBlockEntity brewBE) {
                int levelsDrained = brewBE.consumeTips(toTip);
                if (levelsDrained > 0) {
                    BrewingCauldronBlock.decrementLevels(state, world, pos, levelsDrained);
                }
            }

            player.incrementStat(Stats.USE_CAULDRON);
            world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return ItemActionResult.success(world.isClient);
    }

    // ==================== BREWING CAULDRON BEHAVIORS ====================

    private static void registerBrewingCauldronBehaviors() {
        Map<Item, CauldronBehavior> brewMap = ModBlocks.BREWING_CAULDRON_BEHAVIOR.map();

        // Glass bottle -> extract current potion (25% extended, same as splash/lingering)
        brewMap.put(Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BrewingCauldronBlockEntity brewBE) {
                    if (brewBE.isBrewing()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

                    PotionContentsComponent contents = brewBE.getCurrentPotion();
                    if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

                    ItemStack potionStack = createExtendedPotion(contents, Items.POTION);

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, potionStack);
                    } else if (!player.getInventory().insertStack(potionStack)) {
                        player.dropItem(potionStack, false);
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    BrewingCauldronBlock.decrementLevel(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Regular potion -> seed/continue brewing cauldron
        brewMap.put(Items.POTION, (state, world, pos, player, hand, stack) -> {
            int level = state.get(BrewingCauldronBlock.LEVEL);
            if (level >= 3) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            Optional<RegistryEntry<Potion>> incomingOpt = contents.potion();
            if (incomingOpt.isEmpty() || incomingOpt.get() == Potions.WATER) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BrewingCauldronBlockEntity brewBE) {
                    if (brewBE.isBrewing()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

                    brewBE.setCurrentPotion(new PotionContentsComponent(incomingOpt.get()));
                    world.setBlockState(pos, state.with(BrewingCauldronBlock.LEVEL, level + 1));

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                        if (stack.isEmpty()) {
                            player.setStackInHand(hand, bottle);
                        } else if (!player.getInventory().insertStack(bottle)) {
                            player.dropItem(bottle, false);
                        }
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Splash potion -> pour back into brewing cauldron
        brewMap.put(Items.SPLASH_POTION, (state, world, pos, player, hand, stack) -> {
            int level = state.get(BrewingCauldronBlock.LEVEL);
            if (level >= 3) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            Optional<RegistryEntry<Potion>> incomingOpt = contents.potion();
            if (incomingOpt.isEmpty() || incomingOpt.get() == Potions.WATER) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BrewingCauldronBlockEntity brewBE) {
                    if (brewBE.isBrewing()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

                    // Strip custom effects to avoid compounding on re-extraction
                    brewBE.setCurrentPotion(new PotionContentsComponent(incomingOpt.get()));
                    world.setBlockState(pos, state.with(BrewingCauldronBlock.LEVEL, level + 1));

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                        ItemStack emptyBottle = new ItemStack(ModItems.EMPTY_SPLASH_BOTTLE);
                        if (stack.isEmpty()) {
                            player.setStackInHand(hand, emptyBottle);
                        } else if (!player.getInventory().insertStack(emptyBottle)) {
                            player.dropItem(emptyBottle, false);
                        }
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Lingering potion -> pour back into brewing cauldron
        brewMap.put(Items.LINGERING_POTION, (state, world, pos, player, hand, stack) -> {
            int level = state.get(BrewingCauldronBlock.LEVEL);
            if (level >= 3) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            Optional<RegistryEntry<Potion>> incomingOpt = contents.potion();
            if (incomingOpt.isEmpty() || incomingOpt.get() == Potions.WATER) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BrewingCauldronBlockEntity brewBE) {
                    if (brewBE.isBrewing()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

                    // Strip custom effects to avoid compounding on re-extraction
                    brewBE.setCurrentPotion(new PotionContentsComponent(incomingOpt.get()));
                    world.setBlockState(pos, state.with(BrewingCauldronBlock.LEVEL, level + 1));

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                        ItemStack emptyBottle = new ItemStack(ModItems.EMPTY_LINGERING_BOTTLE);
                        if (stack.isEmpty()) {
                            player.setStackInHand(hand, emptyBottle);
                        } else if (!player.getInventory().insertStack(emptyBottle)) {
                            player.dropItem(emptyBottle, false);
                        }
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Milk bucket -> clear player effects and drain 1 level
        brewMap.put(Items.MILK_BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                player.clearStatusEffects();

                int currentLevel = state.get(BrewingCauldronBlock.LEVEL);
                if (currentLevel <= 1) {
                    world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                } else {
                    world.setBlockState(pos, state.with(BrewingCauldronBlock.LEVEL, currentLevel - 1));
                }

                if (!player.getAbilities().creativeMode) {
                    player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                }

                player.incrementStat(Stats.USE_CAULDRON);
                world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Empty splash bottle -> fill with splash potion (25% extended, only when not brewing)
        brewMap.put(ModItems.EMPTY_SPLASH_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BrewingCauldronBlockEntity brewBE) {
                    if (brewBE.isBrewing()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

                    PotionContentsComponent contents = brewBE.getCurrentPotion();
                    if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    ItemStack splashPotion = createExtendedPotion(contents, Items.SPLASH_POTION);

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, splashPotion);
                    } else if (!player.getInventory().insertStack(splashPotion)) {
                        player.dropItem(splashPotion, false);
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    BrewingCauldronBlock.decrementLevel(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Empty lingering bottle -> fill with lingering potion (25% extended, only when not brewing)
        brewMap.put(ModItems.EMPTY_LINGERING_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BrewingCauldronBlockEntity brewBE) {
                    if (brewBE.isBrewing()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

                    PotionContentsComponent contents = brewBE.getCurrentPotion();
                    if (contents == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    ItemStack lingeringPotion = createExtendedPotion(contents, Items.LINGERING_POTION);

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, lingeringPotion);
                    } else if (!player.getInventory().insertStack(lingeringPotion)) {
                        player.dropItem(lingeringPotion, false);
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    BrewingCauldronBlock.decrementLevel(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
                }
            }
            return ItemActionResult.success(world.isClient);
        });

        // Bucket empties the brewing cauldron
        brewMap.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Block leather armor from interacting with brewing cauldrons
        CauldronBehavior noOp = (state, world, pos, player, hand, stack) -> ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        brewMap.put(Items.LEATHER_HELMET, noOp);
        brewMap.put(Items.LEATHER_CHESTPLATE, noOp);
        brewMap.put(Items.LEATHER_LEGGINGS, noOp);
        brewMap.put(Items.LEATHER_BOOTS, noOp);
        brewMap.put(Items.LEATHER_HORSE_ARMOR, noOp);
    }

    // ==================== MILK CAULDRON BEHAVIORS ====================

    private static void registerMilkCauldronBehaviors() {
        Map<Item, CauldronBehavior> milkMap = ModBlocks.MILK_CAULDRON_BEHAVIOR.map();

        // Empty hand -> drink milk and clear effects
        milkMap.put(Items.AIR, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                player.clearStatusEffects();
                player.incrementStat(Stats.USE_CAULDRON);
                MilkCauldronBlock.decrementLevel(state, world, pos);
                world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Glass bottle -> gives milk bucket
        milkMap.put(Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET);

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                if (!player.getInventory().insertStack(milkBucket)) {
                    player.dropItem(milkBucket, false);
                }

                player.incrementStat(Stats.USE_CAULDRON);
                MilkCauldronBlock.decrementLevel(state, world, pos);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Bucket -> extract milk bucket (only when full)
        milkMap.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            int level = state.get(MilkCauldronBlock.LEVEL);
            if (level < 3) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            if (!world.isClient) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET);
                if (stack.isEmpty()) {
                    player.setStackInHand(hand, milkBucket);
                } else if (!player.getInventory().insertStack(milkBucket)) {
                    player.dropItem(milkBucket, false);
                }

                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });

        // Tipped arrow -> wash off potion, return normal arrow
        milkMap.put(Items.TIPPED_ARROW, (state, world, pos, player, hand, stack) -> {
            return tryWashArrow(state, world, pos, player, hand, stack);
        });

        // Milk bucket -> fill more milk
        milkMap.put(Items.MILK_BUCKET, (state, world, pos, player, hand, stack) -> {
            int level = state.get(MilkCauldronBlock.LEVEL);
            if (level >= 3) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            if (!world.isClient) {
                world.setBlockState(pos, state.with(MilkCauldronBlock.LEVEL, 3));

                if (!player.getAbilities().creativeMode) {
                    player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                }

                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return ItemActionResult.success(world.isClient);
        });
    }

    // ==================== FROZEN CAULDRON BEHAVIORS ====================

    private static void registerFrozenCauldronBehaviors() {
        Map<Item, CauldronBehavior> frozenMap = ModBlocks.FROZEN_CAULDRON_BEHAVIOR.map();

        // Bucket -> pick up contents from full frozen cauldron
        frozenMap.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            int level = state.get(FrozenCauldronBlock.LEVEL);
            if (level < 3) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                ItemStack resultBucket;

                if (be instanceof FrozenCauldronBlockEntity frozenBE) {
                    resultBucket = switch (frozenBE.getSourceType()) {
                        case "snow" -> new ItemStack(Items.POWDER_SNOW_BUCKET);
                        case "milk" -> new ItemStack(Items.MILK_BUCKET);
                        default -> new ItemStack(Items.WATER_BUCKET);
                    };
                } else {
                    resultBucket = new ItemStack(Items.WATER_BUCKET);
                }

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                if (stack.isEmpty()) {
                    player.setStackInHand(hand, resultBucket);
                } else if (!player.getInventory().insertStack(resultBucket)) {
                    player.dropItem(resultBucket, false);
                }

                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                player.incrementStat(Stats.USE_CAULDRON);
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL_POWDER_SNOW, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemActionResult.success(world.isClient);
        });
    }

    // ==================== ARROW WASHING ====================

    /**
     * Wash tipped arrows in milk cauldron to return normal arrows.
     * Washes up to arrowsPerCauldronLevel (default 16) per milk level consumed.
     */
    public static ItemActionResult tryWashArrow(BlockState state, World world, BlockPos pos,
                                                 PlayerEntity player, Hand hand, ItemStack stack) {
        if (!(stack.getItem() instanceof TippedArrowItem)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!world.isClient) {
            int arrowsPerLevel = ModConfig.get().arrowsPerCauldronLevel;
            int toWash = Math.min(stack.getCount(), arrowsPerLevel);

            ItemStack normalArrows = new ItemStack(Items.ARROW, toWash);

            if (!player.getAbilities().creativeMode) {
                stack.decrement(toWash);
            }

            if (!player.getInventory().insertStack(normalArrows)) {
                player.dropItem(normalArrows, false);
            }

            player.incrementStat(Stats.USE_CAULDRON);
            MilkCauldronBlock.decrementLevel(state, world, pos);
            world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return ItemActionResult.success(world.isClient);
    }

    // ==================== EMPTY-HAND DRINKING ====================

    /**
     * Handle empty-hand drinking from potion cauldron (25% extended effects).
     * Called from PotionCauldronBlock.onUse() since onUseWithItem is not invoked for empty hands in 1.21.1.
     */
    public static ActionResult tryDrinkFromPotionCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!player.getMainHandStack().isEmpty()) return ActionResult.PASS;
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PotionCauldronBlockEntity potionBE) {
                PotionContentsComponent storedContents = potionBE.getPotionContents();
                if (storedContents == null) return ActionResult.PASS;

                Optional<RegistryEntry<Potion>> potionOpt = storedContents.potion();
                if (potionOpt.isEmpty() || potionOpt.get().value().getEffects().isEmpty()) return ActionResult.PASS;

                for (StatusEffectInstance effect : potionOpt.get().value().getEffects()) {
                    int extendedDuration = (int) (effect.getDuration() * 1.25);
                    player.addStatusEffect(new StatusEffectInstance(
                            effect.getEffectType(),
                            extendedDuration,
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.shouldShowParticles(),
                            effect.shouldShowIcon()
                    ));
                }

                player.incrementStat(Stats.USE_CAULDRON);
                PotionCauldronBlock.decrementLevel(state, world, pos);
                world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
        }
        return ActionResult.success(world.isClient);
    }

    /**
     * Handle empty-hand drinking from brewing cauldron (25% extended effects, only when not brewing).
     * Called from BrewingCauldronBlock.onUse().
     */
    public static ActionResult tryDrinkFromBrewingCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!player.getMainHandStack().isEmpty()) return ActionResult.PASS;
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BrewingCauldronBlockEntity brewBE) {
                if (brewBE.isBrewing()) return ActionResult.PASS;

                PotionContentsComponent contents = brewBE.getCurrentPotion();
                if (contents == null) return ActionResult.PASS;

                Optional<RegistryEntry<Potion>> potionOpt = contents.potion();
                if (potionOpt.isEmpty() || potionOpt.get().value().getEffects().isEmpty()) return ActionResult.PASS;

                for (StatusEffectInstance effect : potionOpt.get().value().getEffects()) {
                    int extendedDuration = (int) (effect.getDuration() * 1.25);
                    player.addStatusEffect(new StatusEffectInstance(
                            effect.getEffectType(),
                            extendedDuration,
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.shouldShowParticles(),
                            effect.shouldShowIcon()
                    ));
                }

                player.incrementStat(Stats.USE_CAULDRON);
                BrewingCauldronBlock.decrementLevel(state, world, pos);
                world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
        }
        return ActionResult.success(world.isClient);
    }

    /**
     * Handle empty-hand drinking from milk cauldron.
     * Called from MilkCauldronBlock.onUse().
     */
    public static ActionResult tryDrinkMilk(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!player.getMainHandStack().isEmpty()) return ActionResult.PASS;
        if (!world.isClient) {
            player.clearStatusEffects();
            player.incrementStat(Stats.USE_CAULDRON);
            MilkCauldronBlock.decrementLevel(state, world, pos);
            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
            world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return ActionResult.success(world.isClient);
    }

    // ==================== UTILITIES ====================

    /**
     * Creates a potion item with 25% extended duration (cauldron advantage).
     * Uses PotionContentsComponent with custom effects to avoid duplicate timer display.
     */
    private static ItemStack createExtendedPotion(PotionContentsComponent contents, Item potionItem) {
        ItemStack potionStack = new ItemStack(potionItem);

        if (contents == null) {
            potionStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.WATER));
            return potionStack;
        }

        Optional<RegistryEntry<Potion>> potionOpt = contents.potion();
        if (potionOpt.isEmpty() || potionOpt.get().value().getEffects().isEmpty()) {
            // For potions with no effects (water, awkward, etc), just set the base potion
            potionStack.set(DataComponentTypes.POTION_CONTENTS, contents);
            return potionStack;
        }

        // Create extended effects
        java.util.List<StatusEffectInstance> extendedEffects = new java.util.ArrayList<>();
        for (StatusEffectInstance effect : potionOpt.get().value().getEffects()) {
            int extendedDuration = (int) (effect.getDuration() * 1.25);
            extendedEffects.add(new StatusEffectInstance(
                    effect.getEffectType(),
                    extendedDuration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.shouldShowParticles(),
                    effect.shouldShowIcon()
            ));
        }

        // Build a PotionContentsComponent with custom effects and matching color
        PotionContentsComponent extended = new PotionContentsComponent(
                Optional.empty(), // No base potion (to avoid duplicate timers)
                Optional.of(contents.getColor()), // Custom color matching original
                extendedEffects
        );

        potionStack.set(DataComponentTypes.POTION_CONTENTS, extended);

        // Set the correct potion name (otherwise it shows "Uncraftable Potion")
        String itemPath = Registries.ITEM.getId(potionItem).getPath();
        String translationKey = Potion.finishTranslationKey(potionOpt, "item.minecraft." + itemPath + ".effect.");
        potionStack.set(DataComponentTypes.ITEM_NAME, Text.translatable(translationKey));

        return potionStack;
    }

    private static Item getDyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }
}
