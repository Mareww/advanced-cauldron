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
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BrewingCauldronBlockEntity extends BlockEntity {

    private Potion currentPotion;
    private int brewingProgress = 0;
    private boolean isBrewing = false;
    private ItemStack pendingIngredient = ItemStack.EMPTY;

    public BrewingCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BREWING_CAULDRON_BLOCK_ENTITY, pos, state);
        this.currentPotion = Potions.WATER;
    }

    public int getDisplayColor() {
        if (currentPotion != null) {
            return PotionUtil.getColor(currentPotion.getEffects());
        }
        return 0x3F76E4;
    }

    public boolean isBrewing() {
        return isBrewing;
    }

    public int getBrewingProgress() {
        return brewingProgress;
    }

    public Potion getCurrentPotion() {
        return currentPotion;
    }

    public void tryAddIngredient(ItemEntity itemEntity) {
        if (isBrewing) return;
        if (world == null) return;
        if (!HeatSourceUtil.hasHeatSource(world, pos)) return;

        ItemStack stack = itemEntity.getStack();
        Potion result = CauldronBrewingHelper.getBrewResult(world, currentPotion, stack);

        if (result != null) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            pendingIngredient = copy;
            stack.decrement(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
            startBrewing();
        }
    }

    public boolean tryAddIngredientFromPlayer(PlayerEntity player, Hand hand) {
        if (isBrewing) return false;
        if (world == null) return false;
        if (!HeatSourceUtil.hasHeatSource(world, pos)) return false;

        ItemStack stack = player.getStackInHand(hand);
        Potion result = CauldronBrewingHelper.getBrewResult(world, currentPotion, stack);

        if (result != null) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            pendingIngredient = copy;
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            startBrewing();
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
        Potion result = CauldronBrewingHelper.getBrewResult(world, currentPotion, pendingIngredient);

        if (result != null) {
            this.currentPotion = result;
        }

        this.isBrewing = false;
        this.brewingProgress = 0;
        this.pendingIngredient = ItemStack.EMPTY;
        markDirty();
        scheduleSync();

        world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    private void scheduleSync() {
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("CurrentPotionId")) {
            String potionId = nbt.getString("CurrentPotionId");
            this.currentPotion = Registries.POTION.get(new Identifier(potionId));
        } else {
            this.currentPotion = Potions.WATER;
        }
        this.brewingProgress = nbt.getInt("BrewingProgress");
        this.isBrewing = nbt.getBoolean("IsBrewing");
        if (nbt.contains("PendingIngredient")) {
            this.pendingIngredient = ItemStack.fromNbt(nbt.getCompound("PendingIngredient"));
        }

        if (this.world != null && this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (this.currentPotion != null) {
            Identifier id = Registries.POTION.getId(this.currentPotion);
            nbt.putString("CurrentPotionId", id.toString());
        }
        nbt.putInt("BrewingProgress", this.brewingProgress);
        nbt.putBoolean("IsBrewing", this.isBrewing);
        if (!this.pendingIngredient.isEmpty()) {
            NbtCompound ingredientNbt = new NbtCompound();
            this.pendingIngredient.writeNbt(ingredientNbt);
            nbt.put("PendingIngredient", ingredientNbt);
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
