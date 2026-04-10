package com.marew.advancedcauldron.block.entity;

import com.marew.advancedcauldron.block.BrewingCauldronBlock;
import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.util.CauldronBrewingHelper;
import com.marew.advancedcauldron.util.HeatSourceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BrewingCauldronBlockEntity extends BlockEntity {

    private PotionContentsComponent currentPotion;
    private int brewingProgress = 0;
    private boolean isBrewing = false;
    private final List<ItemStack> ingredientQueue = new ArrayList<>();
    private int tipsUsed = 0;

    // Color fade animation
    private int fadeProgress = 0;
    private int fadeStartColor = -1;
    private static final int FADE_DURATION = 30; // 1.5 seconds

    // Smooth color transition - stores the color at the start of brewing
    private int brewingStartColor = -1;

    public BrewingCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BREWING_CAULDRON_BLOCK_ENTITY, pos, state);
        this.currentPotion = new PotionContentsComponent(Potions.WATER);
    }

    public int getDisplayColor() {
        int baseColor = getBaseColor();

        // During brewing: blend from start color to target color
        if (isBrewing && !ingredientQueue.isEmpty()) {
            int startColor = (brewingStartColor != -1) ? brewingStartColor : baseColor;
            int targetColor = getTargetColor();
            float progress = (float) brewingProgress / ModConfig.get().brewTimeTicks;
            return lerpColor(startColor, targetColor, progress);
        }

        // During fade: blend from fade start color to final color
        if (fadeProgress > 0 && fadeStartColor != -1) {
            float progress = 1.0F - ((float) fadeProgress / FADE_DURATION);
            return lerpColor(fadeStartColor, baseColor, progress);
        }

        return baseColor;
    }

    // Special colors for ingredients
    private static final int NETHER_WART_COLOR = 0xB02020; // Bright red like nether wart

    private int getTargetColor() {
        if (!ingredientQueue.isEmpty() && world != null) {
            ItemStack nextIngredient = ingredientQueue.get(0);

            // Special color for nether wart
            if (nextIngredient.isOf(Items.NETHER_WART)) {
                return NETHER_WART_COLOR;
            }

            PotionContentsComponent result = CauldronBrewingHelper.getBrewResult(world, currentPotion, nextIngredient);
            if (result != null) {
                return result.getColor();
            }
        }
        return getBaseColor();
    }

    private int getBaseColor() {
        if (currentPotion == null) return 0x3F76E4;

        // Special handling for potions with no effects (awkward, mundane, thick, water)
        java.util.Optional<net.minecraft.registry.entry.RegistryEntry<net.minecraft.potion.Potion>> potionOpt = currentPotion.potion();
        if (potionOpt.isPresent()) {
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.potion.Potion> potion = potionOpt.get();
            if (potion == Potions.AWKWARD) return 0x8B2252;
            if (potion == Potions.MUNDANE) return 0x4060C0;
            if (potion == Potions.THICK) return 0x5050A0;
            if (potion == Potions.WATER) return 0x3F76E4;
        }

        // For potions with effects, use PotionContentsComponent.getColor()
        return currentPotion.getColor();
    }

    private int lerpColor(int from, int to, float progress) {
        int fromR = (from >> 16) & 0xFF;
        int fromG = (from >> 8) & 0xFF;
        int fromB = from & 0xFF;

        int toR = (to >> 16) & 0xFF;
        int toG = (to >> 8) & 0xFF;
        int toB = to & 0xFF;

        int r = (int) (fromR + (toR - fromR) * progress);
        int g = (int) (fromG + (toG - fromG) * progress);
        int b = (int) (fromB + (toB - fromB) * progress);

        return (r << 16) | (g << 8) | b;
    }

    public int getBrewingBubbleColor() {
        if (isBrewing && !ingredientQueue.isEmpty()) {
            return getTargetColor();
        }
        return getDisplayColor();
    }

    public boolean isBrewing() {
        return isBrewing;
    }

    public int getBrewingProgress() {
        return brewingProgress;
    }

    public PotionContentsComponent getCurrentPotion() {
        return currentPotion;
    }

    public void setCurrentPotion(PotionContentsComponent potion) {
        this.currentPotion = potion;
        markDirty();
        scheduleSync();
    }

    public int getTipsRemaining() {
        return ModConfig.get().arrowsPerCauldronLevel - tipsUsed;
    }

    public int consumeTips(int count) {
        tipsUsed += count;
        int levelsDrained = tipsUsed / ModConfig.get().arrowsPerCauldronLevel;
        tipsUsed = tipsUsed % ModConfig.get().arrowsPerCauldronLevel;
        markDirty();
        return levelsDrained;
    }

    private PotionContentsComponent getProjectedPotion() {
        PotionContentsComponent projected = currentPotion;
        for (ItemStack queued : ingredientQueue) {
            PotionContentsComponent next = CauldronBrewingHelper.getBrewResult(world, projected, queued);
            if (next != null) {
                projected = next;
            }
        }
        return projected;
    }

    public void tryAddIngredient(ItemEntity itemEntity) {
        if (world == null) return;
        if (!HeatSourceUtil.hasHeatSource(world, pos)) return;

        ItemStack stack = itemEntity.getStack();
        PotionContentsComponent projected = getProjectedPotion();
        PotionContentsComponent result = CauldronBrewingHelper.getBrewResult(world, projected, stack);

        if (result != null) {
            ingredientQueue.add(stack.copyWithCount(1));
            stack.decrement(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
            if (!isBrewing) {
                startBrewing();
            } else {
                markDirty();
                scheduleSync();
            }
        }
    }

    public boolean tryAddIngredientFromPlayer(PlayerEntity player, Hand hand) {
        if (world == null) return false;
        if (!HeatSourceUtil.hasHeatSource(world, pos)) return false;

        ItemStack stack = player.getStackInHand(hand);
        PotionContentsComponent projected = getProjectedPotion();
        PotionContentsComponent result = CauldronBrewingHelper.getBrewResult(world, projected, stack);

        if (result != null) {
            ingredientQueue.add(stack.copyWithCount(1));
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            if (!isBrewing) {
                startBrewing();
            } else {
                markDirty();
                scheduleSync();
            }
            return true;
        }
        return false;
    }

    private void startBrewing() {
        // Capture current color for smooth transition
        this.brewingStartColor = getDisplayColor();
        this.isBrewing = true;
        this.brewingProgress = 0;
        markDirty();
        scheduleSync();
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        // Handle fade animation
        if (fadeProgress > 0) {
            fadeProgress--;
            if (fadeProgress == 0) {
                fadeStartColor = -1;
            }
            scheduleSync();
        }

        // Apply potion effects to entities standing in the cauldron
        if (currentPotion != null) {
            java.util.Optional<net.minecraft.registry.entry.RegistryEntry<net.minecraft.potion.Potion>> potionOpt = currentPotion.potion();
            if (potionOpt.isPresent() && !potionOpt.get().value().getEffects().isEmpty()) {
                Box cauldronBox = new Box(pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
                        pos.getX() + 0.9, pos.getY() + 1.0, pos.getZ() + 0.9);
                for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, cauldronBox, e -> true)) {
                    for (StatusEffectInstance effect : potionOpt.get().value().getEffects()) {
                        // Apply short duration effect (refreshed each tick)
                        entity.addStatusEffect(new StatusEffectInstance(
                                effect.getEffectType(),
                                40, // 2 seconds, refreshed each tick
                                effect.getAmplifier(),
                                true, // ambient
                                false, // show particles
                                true // show icon
                        ));
                    }
                }
            }
        }

        if (!isBrewing) return;
        if (!HeatSourceUtil.hasHeatSource(world, pos)) return;

        brewingProgress++;

        if (brewingProgress >= ModConfig.get().brewTimeTicks) {
            completeBrewing(world, pos, state);
        }

        // Sync every tick for smooth color transitions
        markDirty();
        scheduleSync();
    }

    private void completeBrewing(World world, BlockPos pos, BlockState state) {
        // Capture current display color before changing potion for fade effect
        int colorBeforeChange = getDisplayColor();

        if (!ingredientQueue.isEmpty()) {
            ItemStack ingredient = ingredientQueue.remove(0);
            PotionContentsComponent result = CauldronBrewingHelper.getBrewResult(world, currentPotion, ingredient);

            if (result != null) {
                this.currentPotion = result;
            }
        }

        if (!ingredientQueue.isEmpty()) {
            // More ingredients to brew - continue without sound
            // Use the color we just reached as the new start color
            this.brewingStartColor = colorBeforeChange;
            this.isBrewing = true;
            this.brewingProgress = 0;
            markDirty();
            scheduleSync();
        } else {
            // All ingredients done - play completion sound
            world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1.0F, 1.0F);
            this.isBrewing = false;
            this.brewingProgress = 0;
            this.brewingStartColor = -1;
            // Start fade animation
            this.fadeStartColor = colorBeforeChange;
            this.fadeProgress = FADE_DURATION;
            markDirty();
            scheduleSync();
        }
    }

    private void scheduleSync() {
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("CurrentPotion")) {
            NbtElement potionNbt = nbt.get("CurrentPotion");
            this.currentPotion = PotionContentsComponent.CODEC
                    .parse(registryLookup.getOps(NbtOps.INSTANCE), potionNbt)
                    .result()
                    .orElse(new PotionContentsComponent(Potions.WATER));
        }
        this.brewingProgress = nbt.getInt("BrewingProgress");
        this.isBrewing = nbt.getBoolean("IsBrewing");
        this.tipsUsed = nbt.getInt("TipsUsed");
        this.fadeProgress = nbt.getInt("FadeProgress");
        this.fadeStartColor = nbt.getInt("FadeStartColor");
        this.brewingStartColor = nbt.getInt("BrewingStartColor");
        this.ingredientQueue.clear();
        if (nbt.contains("IngredientQueue", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("IngredientQueue", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.fromNbt(registryLookup, list.getCompound(i))
                        .ifPresent(this.ingredientQueue::add);
            }
        } else if (nbt.contains("PendingIngredient")) {
            ItemStack.fromNbt(registryLookup, nbt.get("PendingIngredient"))
                    .ifPresent(this.ingredientQueue::add);
        }

        if (this.world != null && this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (this.currentPotion != null) {
            PotionContentsComponent.CODEC
                    .encodeStart(registryLookup.getOps(NbtOps.INSTANCE), this.currentPotion)
                    .result()
                    .ifPresent(encoded -> nbt.put("CurrentPotion", encoded));
        }
        nbt.putInt("BrewingProgress", this.brewingProgress);
        nbt.putBoolean("IsBrewing", this.isBrewing);
        nbt.putInt("TipsUsed", this.tipsUsed);
        nbt.putInt("FadeProgress", this.fadeProgress);
        nbt.putInt("FadeStartColor", this.fadeStartColor);
        nbt.putInt("BrewingStartColor", this.brewingStartColor);
        if (!this.ingredientQueue.isEmpty()) {
            NbtList list = new NbtList();
            for (ItemStack stack : this.ingredientQueue) {
                ItemStack.CODEC
                        .encodeStart(registryLookup.getOps(NbtOps.INSTANCE), stack)
                        .result()
                        .ifPresent(list::add);
            }
            nbt.put("IngredientQueue", list);
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}
