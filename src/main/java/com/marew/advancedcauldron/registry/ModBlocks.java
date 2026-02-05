package com.marew.advancedcauldron.registry;

import com.marew.advancedcauldron.AdvancedCauldron;
import com.marew.advancedcauldron.block.BrewingCauldronBlock;
import com.marew.advancedcauldron.block.DyedWaterCauldronBlock;
import com.marew.advancedcauldron.block.MilkCauldronBlock;
import com.marew.advancedcauldron.block.PotionCauldronBlock;
import com.marew.advancedcauldron.block.entity.BrewingCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.DyedWaterCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.MilkCauldronBlockEntity;
import com.marew.advancedcauldron.block.entity.PotionCauldronBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final CauldronBehavior.CauldronBehaviorMap POTION_CAULDRON_BEHAVIOR =
            CauldronBehavior.createMap("potion_cauldron");

    public static final CauldronBehavior.CauldronBehaviorMap DYED_WATER_CAULDRON_BEHAVIOR =
            CauldronBehavior.createMap("dyed_water_cauldron");

    public static final CauldronBehavior.CauldronBehaviorMap BREWING_CAULDRON_BEHAVIOR =
            CauldronBehavior.createMap("brewing_cauldron");

    public static final CauldronBehavior.CauldronBehaviorMap MILK_CAULDRON_BEHAVIOR =
            CauldronBehavior.createMap("milk_cauldron");

    public static final Block POTION_CAULDRON = new PotionCauldronBlock(
            AbstractBlock.Settings.copy(Blocks.CAULDRON),
            POTION_CAULDRON_BEHAVIOR
    );

    public static final Block DYED_WATER_CAULDRON = new DyedWaterCauldronBlock(
            AbstractBlock.Settings.copy(Blocks.CAULDRON),
            DYED_WATER_CAULDRON_BEHAVIOR
    );

    public static final Block BREWING_CAULDRON = new BrewingCauldronBlock(
            AbstractBlock.Settings.copy(Blocks.CAULDRON),
            BREWING_CAULDRON_BEHAVIOR
    );

    public static final Block MILK_CAULDRON = new MilkCauldronBlock(
            AbstractBlock.Settings.copy(Blocks.CAULDRON),
            MILK_CAULDRON_BEHAVIOR
    );

    public static BlockEntityType<PotionCauldronBlockEntity> POTION_CAULDRON_BLOCK_ENTITY;
    public static BlockEntityType<DyedWaterCauldronBlockEntity> DYED_WATER_CAULDRON_BLOCK_ENTITY;
    public static BlockEntityType<BrewingCauldronBlockEntity> BREWING_CAULDRON_BLOCK_ENTITY;
    public static BlockEntityType<MilkCauldronBlockEntity> MILK_CAULDRON_BLOCK_ENTITY;

    public static void register() {
        Registry.register(Registries.BLOCK, Identifier.of(AdvancedCauldron.MOD_ID, "potion_cauldron"), POTION_CAULDRON);
        Registry.register(Registries.BLOCK, Identifier.of(AdvancedCauldron.MOD_ID, "dyed_water_cauldron"), DYED_WATER_CAULDRON);
        Registry.register(Registries.BLOCK, Identifier.of(AdvancedCauldron.MOD_ID, "brewing_cauldron"), BREWING_CAULDRON);
        Registry.register(Registries.BLOCK, Identifier.of(AdvancedCauldron.MOD_ID, "milk_cauldron"), MILK_CAULDRON);

        POTION_CAULDRON_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(AdvancedCauldron.MOD_ID, "potion_cauldron_be"),
                FabricBlockEntityTypeBuilder.create(PotionCauldronBlockEntity::new, POTION_CAULDRON).build()
        );

        DYED_WATER_CAULDRON_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(AdvancedCauldron.MOD_ID, "dyed_water_cauldron_be"),
                FabricBlockEntityTypeBuilder.create(DyedWaterCauldronBlockEntity::new, DYED_WATER_CAULDRON).build()
        );

        BREWING_CAULDRON_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(AdvancedCauldron.MOD_ID, "brewing_cauldron_be"),
                FabricBlockEntityTypeBuilder.create(BrewingCauldronBlockEntity::new, BREWING_CAULDRON).build()
        );

        MILK_CAULDRON_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(AdvancedCauldron.MOD_ID, "milk_cauldron_be"),
                FabricBlockEntityTypeBuilder.create(MilkCauldronBlockEntity::new, MILK_CAULDRON).build()
        );
    }
}
