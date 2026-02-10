package com.marew.advancedcauldron.block.entity;

import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.potion.Potion;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PotionCauldronBlockEntity extends BlockEntity {

    @Nullable
    private PotionContentsComponent potionContents = null;
    private int tipsUsed = 0;

    public PotionCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.POTION_CAULDRON_BLOCK_ENTITY, pos, state);
    }

    @Nullable
    public PotionContentsComponent getPotionContents() {
        return potionContents;
    }

    @Nullable
    public RegistryEntry<Potion> getPotion() {
        if (potionContents != null) {
            return potionContents.potion().orElse(null);
        }
        return null;
    }

    public void setPotionContents(@Nullable PotionContentsComponent contents) {
        this.potionContents = contents;
        this.tipsUsed = 0;
        markDirty();
        scheduleSync();
    }

    public void setPotion(@Nullable RegistryEntry<Potion> potion) {
        if (potion != null) {
            setPotionContents(new PotionContentsComponent(potion));
        } else {
            setPotionContents(null);
        }
    }

    private void scheduleSync() {
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    public int getTipsRemaining() {
        return ModConfig.get().arrowsPerCauldronLevel - tipsUsed;
    }

    public int getTipsUsed() {
        return tipsUsed;
    }

    public int consumeTips(int count) {
        tipsUsed += count;
        int levelsDrained = tipsUsed / ModConfig.get().arrowsPerCauldronLevel;
        tipsUsed = tipsUsed % ModConfig.get().arrowsPerCauldronLevel;
        markDirty();
        return levelsDrained;
    }

    public void restoreFromFrozen(@Nullable PotionContentsComponent contents, int tipsUsed) {
        this.potionContents = contents;
        this.tipsUsed = tipsUsed;
        markDirty();
        scheduleSync();
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("PotionContents")) {
            NbtElement potionNbt = nbt.get("PotionContents");
            this.potionContents = PotionContentsComponent.CODEC
                    .parse(registryLookup.getOps(NbtOps.INSTANCE), potionNbt)
                    .result()
                    .orElse(null);
        } else {
            this.potionContents = null;
        }
        this.tipsUsed = nbt.getInt("TipsUsed");

        if (this.world != null && this.world.isClient) {
            this.world.updateListeners(this.pos, Blocks.CAULDRON.getDefaultState(), this.getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (this.potionContents != null) {
            PotionContentsComponent.CODEC
                    .encodeStart(registryLookup.getOps(NbtOps.INSTANCE), this.potionContents)
                    .result()
                    .ifPresent(encoded -> nbt.put("PotionContents", encoded));
        }
        nbt.putInt("TipsUsed", this.tipsUsed);
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
