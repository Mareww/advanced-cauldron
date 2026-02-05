package com.marew.advancedcauldron.registry;

import com.marew.advancedcauldron.AdvancedCauldron;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item EMPTY_SPLASH_BOTTLE = registerItem("empty_splash_bottle",
            new Item(new Item.Settings().maxCount(16)));

    public static final Item EMPTY_LINGERING_BOTTLE = registerItem("empty_lingering_bottle",
            new Item(new Item.Settings().maxCount(16)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(AdvancedCauldron.MOD_ID, name), item);
    }

    public static void register() {
        AdvancedCauldron.LOGGER.info("Registering items for " + AdvancedCauldron.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(EMPTY_SPLASH_BOTTLE);
            entries.add(EMPTY_LINGERING_BOTTLE);
        });
    }
}
