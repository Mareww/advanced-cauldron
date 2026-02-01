package com.marew.advancedcauldron.registry;

import com.marew.advancedcauldron.AdvancedCauldron;
import com.marew.advancedcauldron.block.BrewingCauldronBlock;
import com.marew.advancedcauldron.block.DyedWaterCauldronBlock;
import com.marew.advancedcauldron.block.PotionCauldronBlock;
import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.DyedWaterCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.PotionCauldronBlockEntity;
import com.marew.advancedcauldron.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Map;

public class ModCauldronBehaviors {
    public static final TagKey<Item> TIPPABLE_ARROWS = TagKey.of(
            Registries.ITEM.getKey(),
            new Identifier(AdvancedCauldron.MOD_ID, "tippable_arrows")
    );

    public static void register() {
        registerEmptyCauldronBehaviors();
        registerWaterCauldronBehaviors();
        registerPotionCauldronBehaviors();
        registerDyedWaterCauldronBehaviors();
        registerBrewingCauldronBehaviors();
    }

    // ==================== WATER CAULDRON BEHAVIORS ====================

    private static void registerWaterCauldronBehaviors() {
        Map<Item, CauldronBehavior> waterMap = CauldronBehavior.WATER_CAULDRON_BEHAVIOR;

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
                        dyedBE.setColor(color.getSignColor());
                    }

                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }

                    player.incrementStat(Stats.USE_CAULDRON);
                    world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                }
                return ActionResult.success(world.isClient);
            });
        }
    }

    // ==================== EMPTY CAULDRON BEHAVIORS ====================

    private static void registerEmptyCauldronBehaviors() {
        Map<Item, CauldronBehavior> emptyMap = CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR;

        emptyMap.put(Items.POTION, (state, world, pos, player, hand, stack) -> {
            Potion potion = PotionUtil.getPotion(stack);
            if (potion == Potions.EMPTY || potion == Potions.WATER) {
                return ActionResult.PASS;
            }

            if (!world.isClient) {
                BlockState potionState = ModBlocks.POTION_CAULDRON.getDefaultState()
                        .with(PotionCauldronBlock.LEVEL, 1);
                world.setBlockState(pos, potionState);

                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    potionBE.setPotion(potion);
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
            return ActionResult.success(world.isClient);
        });
    }

    // ==================== POTION CAULDRON BEHAVIORS ====================

    private static void registerPotionCauldronBehaviors() {
        Map<Item, CauldronBehavior> potionMap = ModBlocks.POTION_CAULDRON_BEHAVIOR;

        // Glass bottle -> extract potion from cauldron
        potionMap.put(Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    Potion storedPotion = potionBE.getPotion();
                    if (storedPotion == null) return ActionResult.PASS;
                    ItemStack potionStack = new ItemStack(Items.POTION);
                    PotionUtil.setPotion(potionStack, storedPotion);

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
            return ActionResult.success(world.isClient);
        });

        // Potion bottle -> add more of the SAME potion to the cauldron
        potionMap.put(Items.POTION, (state, world, pos, player, hand, stack) -> {
            int level = state.get(PotionCauldronBlock.LEVEL);
            if (level >= 3) return ActionResult.PASS;

            Potion incomingPotion = PotionUtil.getPotion(stack);
            if (incomingPotion == Potions.EMPTY || incomingPotion == Potions.WATER) {
                return ActionResult.PASS;
            }

            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof PotionCauldronBlockEntity potionBE) {
                    if (potionBE.getPotion() != incomingPotion) {
                        return ActionResult.PASS;
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
            return ActionResult.success(world.isClient);
        });

        // Bucket empties the potion cauldron
        potionMap.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ActionResult.success(world.isClient);
        });

        // Block leather armor from interacting with potion cauldrons
        CauldronBehavior noOp = (state, world, pos, player, hand, stack) -> ActionResult.PASS;
        potionMap.put(Items.LEATHER_HELMET, noOp);
        potionMap.put(Items.LEATHER_CHESTPLATE, noOp);
        potionMap.put(Items.LEATHER_LEGGINGS, noOp);
        potionMap.put(Items.LEATHER_BOOTS, noOp);
        potionMap.put(Items.LEATHER_HORSE_ARMOR, noOp);
    }

    // ==================== DYED WATER CAULDRON BEHAVIORS ====================

    private static void registerDyedWaterCauldronBehaviors() {
        Map<Item, CauldronBehavior> dyedMap = ModBlocks.DYED_WATER_CAULDRON_BEHAVIOR;

        // Glass bottle -> extract dyed water (gives water bottle)
        dyedMap.put(Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                ItemStack waterBottle = new ItemStack(Items.POTION);
                PotionUtil.setPotion(waterBottle, Potions.WATER);

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
            return ActionResult.success(world.isClient);
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
                return ActionResult.success(world.isClient);
            });
        }

        // Bucket empties the dyed water cauldron
        dyedMap.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ActionResult.success(world.isClient);
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
                    if (stack.getItem() instanceof DyeableItem dyeableItem) {
                        dyeableItem.setColor(stack, dyedBE.getColor());
                    }
                    player.incrementStat(Stats.USE_CAULDRON);
                    DyedWaterCauldronBlock.decrementLevel(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                }
            }
            return ActionResult.success(world.isClient);
        });
    }

    // ==================== ARROW TIPPING LOGIC ====================

    public static ActionResult tryTipArrow(BlockState state, World world, BlockPos pos,
                                            PlayerEntity player, Hand hand, ItemStack stack) {
        if (!(state.getBlock() instanceof PotionCauldronBlock)) return ActionResult.PASS;

        Item item = stack.getItem();

        boolean isTippable = stack.isIn(TIPPABLE_ARROWS) || item instanceof ArrowItem;

        if (!isTippable) return ActionResult.PASS;

        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PotionCauldronBlockEntity potionBE) {
                Potion storedPotion = potionBE.getPotion();
                if (storedPotion == null) return ActionResult.PASS;

                // Tip up to remaining capacity in current level, capped by stack size
                int currentLevel = state.get(PotionCauldronBlock.LEVEL);
                int arrowsPerLevel = ModConfig.get().arrowsPerCauldronLevel;
                int totalAvailable = ((currentLevel - 1) * arrowsPerLevel) + potionBE.getTipsRemaining();
                int toTip = Math.min(Math.min(stack.getCount(), arrowsPerLevel), totalAvailable);

                if (toTip <= 0) return ActionResult.PASS;

                // Determine the tipped arrow item
                Item tippedItem = Items.TIPPED_ARROW;
                NbtCompound customNbt = new NbtCompound();
                customNbt.putBoolean("CauldronTipped", true);

                if (item != Items.ARROW) {
                    Identifier arrowId = Registries.ITEM.getId(item);
                    customNbt.putString("OriginalArrow", arrowId.toString());

                    String namespace = arrowId.getNamespace();
                    String path = arrowId.getPath();
                    Identifier tippedVariantId = new Identifier(namespace, "tipped_" + path);
                    Item tippedVariant = Registries.ITEM.get(tippedVariantId);

                    if (tippedVariant != Items.AIR && tippedVariant != Items.TIPPED_ARROW) {
                        tippedItem = tippedVariant;
                    }
                }

                // Create the tipped arrows with the exact same potion
                ItemStack tippedArrows = new ItemStack(tippedItem, toTip);
                PotionUtil.setPotion(tippedArrows, storedPotion);
                NbtCompound nbt = tippedArrows.getOrCreateNbt();
                nbt.put("CauldronData", customNbt);

                // Consume arrows
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(toTip);
                }

                // Give the tipped arrows
                if (!player.getInventory().insertStack(tippedArrows)) {
                    player.dropItem(tippedArrows, false);
                }

                // Consume tips and drain levels proportionally
                int levelsDrained = potionBE.consumeTips(toTip);
                if (levelsDrained > 0) {
                    PotionCauldronBlock.decrementLevels(state, world, pos, levelsDrained);
                }

                player.incrementStat(Stats.USE_CAULDRON);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
        }
        return ActionResult.success(world.isClient);
    }

    // ==================== BREWING CAULDRON BEHAVIORS ====================

    private static void registerBrewingCauldronBehaviors() {
        Map<Item, CauldronBehavior> brewMap = ModBlocks.BREWING_CAULDRON_BEHAVIOR;

        // Glass bottle -> extract current potion (if not actively brewing)
        brewMap.put(Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BrewingCauldronBlockEntity brewBE) {
                    if (brewBE.isBrewing()) return ActionResult.PASS;

                    Potion potion = brewBE.getCurrentPotion();
                    if (potion == null) return ActionResult.PASS;

                    ItemStack potionStack = new ItemStack(Items.POTION);
                    PotionUtil.setPotion(potionStack, potion);

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
            return ActionResult.success(world.isClient);
        });

        // Bucket empties the brewing cauldron
        brewMap.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            if (!world.isClient) {
                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ActionResult.success(world.isClient);
        });

        // Block leather armor from interacting with brewing cauldrons
        CauldronBehavior noOp = (state, world, pos, player, hand, stack) -> ActionResult.PASS;
        brewMap.put(Items.LEATHER_HELMET, noOp);
        brewMap.put(Items.LEATHER_CHESTPLATE, noOp);
        brewMap.put(Items.LEATHER_LEGGINGS, noOp);
        brewMap.put(Items.LEATHER_BOOTS, noOp);
        brewMap.put(Items.LEATHER_HORSE_ARMOR, noOp);
    }

    // ==================== UTILITIES ====================

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
