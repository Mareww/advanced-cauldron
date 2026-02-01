package com.marew.advancedcauldron.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModConfigScreen {
    public static Screen create(Screen parent) {
        ModConfig config = ModConfig.get();
        ModConfig defaults = new ModConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.advancedcauldron.title"));

        ConfigEntryBuilder entry = builder.entryBuilder();

        // Brewing category
        ConfigCategory brewing = builder.getOrCreateCategory(Text.translatable("config.advancedcauldron.category.brewing"));

        brewing.addEntry(entry.startIntField(Text.translatable("config.advancedcauldron.brewTimeTicks"), config.brewTimeTicks)
                .setDefaultValue(defaults.brewTimeTicks)
                .setMin(1)
                .setTooltip(Text.translatable("config.advancedcauldron.brewTimeTicks.tooltip"))
                .setSaveConsumer(val -> config.brewTimeTicks = val)
                .build());

        brewing.addEntry(entry.startIntField(Text.translatable("config.advancedcauldron.itemPickupDelayTicks"), config.itemPickupDelayTicks)
                .setDefaultValue(defaults.itemPickupDelayTicks)
                .setMin(0)
                .setTooltip(Text.translatable("config.advancedcauldron.itemPickupDelayTicks.tooltip"))
                .setSaveConsumer(val -> config.itemPickupDelayTicks = val)
                .build());

        brewing.addEntry(entry.startIntField(Text.translatable("config.advancedcauldron.arrowsPerCauldronLevel"), config.arrowsPerCauldronLevel)
                .setDefaultValue(defaults.arrowsPerCauldronLevel)
                .setMin(1)
                .setTooltip(Text.translatable("config.advancedcauldron.arrowsPerCauldronLevel.tooltip"))
                .setSaveConsumer(val -> config.arrowsPerCauldronLevel = val)
                .build());

        // Heat damage category
        ConfigCategory heat = builder.getOrCreateCategory(Text.translatable("config.advancedcauldron.category.heat"));

        heat.addEntry(entry.startBooleanToggle(Text.translatable("config.advancedcauldron.heatDamageEnabled"), config.heatDamageEnabled)
                .setDefaultValue(defaults.heatDamageEnabled)
                .setTooltip(Text.translatable("config.advancedcauldron.heatDamageEnabled.tooltip"))
                .setSaveConsumer(val -> config.heatDamageEnabled = val)
                .build());

        heat.addEntry(entry.startDoubleField(Text.translatable("config.advancedcauldron.heatDamageAmount"), config.heatDamageAmount)
                .setDefaultValue(defaults.heatDamageAmount)
                .setMin(0.0)
                .setTooltip(Text.translatable("config.advancedcauldron.heatDamageAmount.tooltip"))
                .setSaveConsumer(val -> config.heatDamageAmount = val)
                .build());

        heat.addEntry(entry.startIntField(Text.translatable("config.advancedcauldron.heatDamageDelayTicks"), config.heatDamageDelayTicks)
                .setDefaultValue(defaults.heatDamageDelayTicks)
                .setMin(0)
                .setTooltip(Text.translatable("config.advancedcauldron.heatDamageDelayTicks.tooltip"))
                .setSaveConsumer(val -> config.heatDamageDelayTicks = val)
                .build());

        heat.addEntry(entry.startIntField(Text.translatable("config.advancedcauldron.heatDamageIntervalTicks"), config.heatDamageIntervalTicks)
                .setDefaultValue(defaults.heatDamageIntervalTicks)
                .setMin(1)
                .setTooltip(Text.translatable("config.advancedcauldron.heatDamageIntervalTicks.tooltip"))
                .setSaveConsumer(val -> config.heatDamageIntervalTicks = val)
                .build());

        // Particles category
        ConfigCategory particles = builder.getOrCreateCategory(Text.translatable("config.advancedcauldron.category.particles"));

        particles.addEntry(entry.startBooleanToggle(Text.translatable("config.advancedcauldron.steamParticlesEnabled"), config.steamParticlesEnabled)
                .setDefaultValue(defaults.steamParticlesEnabled)
                .setTooltip(Text.translatable("config.advancedcauldron.steamParticlesEnabled.tooltip"))
                .setSaveConsumer(val -> config.steamParticlesEnabled = val)
                .build());

        particles.addEntry(entry.startBooleanToggle(Text.translatable("config.advancedcauldron.brewingBubbleParticlesEnabled"), config.brewingBubbleParticlesEnabled)
                .setDefaultValue(defaults.brewingBubbleParticlesEnabled)
                .setTooltip(Text.translatable("config.advancedcauldron.brewingBubbleParticlesEnabled.tooltip"))
                .setSaveConsumer(val -> config.brewingBubbleParticlesEnabled = val)
                .build());

        particles.addEntry(entry.startBooleanToggle(Text.translatable("config.advancedcauldron.suppressCampfireSmoke"), config.suppressCampfireSmoke)
                .setDefaultValue(defaults.suppressCampfireSmoke)
                .setTooltip(Text.translatable("config.advancedcauldron.suppressCampfireSmoke.tooltip"))
                .setSaveConsumer(val -> config.suppressCampfireSmoke = val)
                .build());

        builder.setSavingRunnable(ModConfig::save);

        return builder.build();
    }
}
