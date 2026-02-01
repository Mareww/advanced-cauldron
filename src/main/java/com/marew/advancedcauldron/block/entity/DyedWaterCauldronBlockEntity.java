package com.marew.advancedcauldron.block.entity;

import com.marew.advancedcauldron.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class DyedWaterCauldronBlockEntity extends BlockEntity {
    private int color = 0x3F76E4; // Default water color

    public DyedWaterCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.DYED_WATER_CAULDRON_BLOCK_ENTITY, pos, state);
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        markDirty();
        scheduleSync();
    }

    /**
     * Mix a new dye color into the existing water color (Bedrock-style averaging).
     */
    public void mixColor(DyeColor dyeColor) {
        int dyeRgb = dyeColor.getEntityColor();

        int r1 = (this.color >> 16) & 0xFF;
        int g1 = (this.color >> 8) & 0xFF;
        int b1 = this.color & 0xFF;

        int r2 = (dyeRgb >> 16) & 0xFF;
        int g2 = (dyeRgb >> 8) & 0xFF;
        int b2 = dyeRgb & 0xFF;

        int r = (r1 + r2) / 2;
        int g = (g1 + g2) / 2;
        int b = (b1 + b2) / 2;

        this.color = (r << 16) | (g << 8) | b;
        markDirty();
        scheduleSync();
    }

    private void scheduleSync() {
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("Color")) {
            this.color = nbt.getInt("Color");
        }

        // When client receives updated block entity data, rebuild chunk mesh so tint color applies
        if (this.world != null && this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("Color", this.color);
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
