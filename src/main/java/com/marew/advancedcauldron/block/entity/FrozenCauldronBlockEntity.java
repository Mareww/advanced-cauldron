package com.marew.advancedcauldron.block.entity;

import com.marew.advancedcauldron.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class FrozenCauldronBlockEntity extends BlockEntity {

    private String sourceType = "water";
    private int color = -1;
    @Nullable
    private NbtCompound potionData = null;
    private int tipsUsed = 0;

    public FrozenCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FROZEN_CAULDRON_BLOCK_ENTITY, pos, state);
    }

    public String getSourceType() {
        return sourceType;
    }

    public int getColor() {
        return color;
    }

    @Nullable
    public NbtCompound getPotionData() {
        return potionData;
    }

    public int getTipsUsed() {
        return tipsUsed;
    }

    public void setFrozenContents(String sourceType, int color, @Nullable NbtCompound potionData, int tipsUsed) {
        this.sourceType = sourceType;
        this.color = color;
        this.potionData = potionData;
        this.tipsUsed = tipsUsed;
        markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.sourceType = nbt.getString("SourceType");
        if (this.sourceType.isEmpty()) this.sourceType = "water";
        this.color = nbt.getInt("Color");
        if (!nbt.contains("Color")) this.color = -1;
        this.tipsUsed = nbt.getInt("TipsUsed");
        if (nbt.contains("PotionData")) {
            this.potionData = nbt.getCompound("PotionData");
        } else {
            this.potionData = null;
        }

        if (this.world != null && this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("SourceType", this.sourceType);
        nbt.putInt("Color", this.color);
        nbt.putInt("TipsUsed", this.tipsUsed);
        if (this.potionData != null) {
            nbt.put("PotionData", this.potionData.copy());
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
