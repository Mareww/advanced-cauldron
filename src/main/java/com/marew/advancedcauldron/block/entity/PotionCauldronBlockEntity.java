package com.marew.advancedcauldron.block.entity;

import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class PotionCauldronBlockEntity extends BlockEntity {

    @Nullable
    private Potion potion = null;
    private int tipsUsed = 0;

    public PotionCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.POTION_CAULDRON_BLOCK_ENTITY, pos, state);
    }

    @Nullable
    public Potion getPotion() {
        return potion;
    }

    public void setPotion(@Nullable Potion potion) {
        this.potion = potion;
        this.tipsUsed = 0;
        markDirty();
        scheduleSync();
    }

    private void scheduleSync() {
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
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

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("PotionId")) {
            String potionId = nbt.getString("PotionId");
            this.potion = Registries.POTION.get(new Identifier(potionId));
        } else {
            this.potion = null;
        }
        this.tipsUsed = nbt.getInt("TipsUsed");

        if (this.world != null && this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (this.potion != null) {
            Identifier id = Registries.POTION.getId(this.potion);
            nbt.putString("PotionId", id.toString());
        }
        nbt.putInt("TipsUsed", this.tipsUsed);
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
