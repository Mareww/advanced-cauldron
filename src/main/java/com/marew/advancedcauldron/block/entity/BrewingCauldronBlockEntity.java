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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

    public BrewingCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BREWING_CAULDRON_BLOCK_ENTITY, pos, state);
        this.currentPotion = new PotionContentsComponent(Potions.WATER);
    }

    public int getDisplayColor() {
        if (currentPotion != null) {
            return currentPotion.getColor();
        }
        return 0x3F76E4;
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
        this.isBrewing = true;
        this.brewingProgress = 0;
        markDirty();
        scheduleSync();
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        if (!isBrewing) return;
        if (!HeatSourceUtil.hasHeatSource(world, pos)) return;

        brewingProgress++;

        if (brewingProgress >= ModConfig.get().brewTimeTicks) {
            completeBrewing(world, pos, state);
        }

        if (brewingProgress % 20 == 0) {
            markDirty();
            scheduleSync();
        }
    }

    private void completeBrewing(World world, BlockPos pos, BlockState state) {
        if (!ingredientQueue.isEmpty()) {
            ItemStack ingredient = ingredientQueue.remove(0);
            PotionContentsComponent result = CauldronBrewingHelper.getBrewResult(world, currentPotion, ingredient);

            if (result != null) {
                this.currentPotion = result;
            }
        }

        world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1.0F, 1.0F);

        if (!ingredientQueue.isEmpty()) {
            startBrewing();
        } else {
            this.isBrewing = false;
            this.brewingProgress = 0;
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
