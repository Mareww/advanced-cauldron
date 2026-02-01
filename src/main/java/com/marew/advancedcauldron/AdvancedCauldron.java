package com.marew.advancedcauldron;

import com.marew.advancedcauldron.config.ModConfig;
import com.marew.advancedcauldron.registry.ModBlocks;
import com.marew.advancedcauldron.registry.ModCauldronBehaviors;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedCauldron implements ModInitializer {
    public static final String MOD_ID = "advancedcauldron";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.load();
        ModBlocks.register();
        ModCauldronBehaviors.register();
        LOGGER.info("Advanced Cauldron loaded!");
    }
}
