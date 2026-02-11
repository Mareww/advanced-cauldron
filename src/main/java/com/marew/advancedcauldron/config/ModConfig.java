package com.marew.advancedcauldron.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.marew.advancedcauldron.AdvancedCauldron;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static ModConfig INSTANCE = new ModConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int brewTimeTicks = 400;
    public int itemPickupDelayTicks = 10;
    public boolean heatDamageEnabled = true;
    public double heatDamageAmount = 1.0;
    public int heatDamageDelayTicks = 40;
    public int heatDamageIntervalTicks = 20;
    public int arrowsPerCauldronLevel = 16;
    public boolean steamParticlesEnabled = true;
    public boolean brewingBubbleParticlesEnabled = true;
    public boolean suppressCampfireSmoke = true;

    // Weather & Freezing
    public boolean freezingEnabled = true;
    public boolean snowFillingEnabled = true;
    public boolean rainFillingEnabled = true;
    public double freezeChance = 0.05;
    public boolean biomeTemperatureFreezingEnabled = true;

    public static ModConfig get() {
        return INSTANCE;
    }

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("advancedcauldron.json");

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ModConfig();
                }
                AdvancedCauldron.LOGGER.info("Loaded config from {}", configFile);
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                AdvancedCauldron.LOGGER.error("Failed to load config, using defaults", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            AdvancedCauldron.LOGGER.info("Config not found, creating defaults at {}", configFile);
        }

        // Always write back to ensure new fields are saved
        save();
    }

    public static void save() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("advancedcauldron.json");
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            AdvancedCauldron.LOGGER.error("Failed to save config", e);
        }
    }
}
