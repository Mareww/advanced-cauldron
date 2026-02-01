package com.marew.advancedcauldron.util;

import com.marew.advancedcauldron.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class HeatDamageUtil {
    private static final Map<UUID, Integer> ticksInHotCauldron = new HashMap<>();
    private static int cleanupCounter = 0;

    public static void tick(Entity entity, World world, BlockPos pos) {
        if (world.isClient) return;
        if (!(entity instanceof LivingEntity living)) return;
        if (!ModConfig.get().heatDamageEnabled) return;
        if (!HeatSourceUtil.hasHeatSource(world, pos)) return;

        UUID id = entity.getUuid();
        int ticks = ticksInHotCauldron.getOrDefault(id, 0) + 1;
        ticksInHotCauldron.put(id, ticks);

        int delay = ModConfig.get().heatDamageDelayTicks;
        int interval = ModConfig.get().heatDamageIntervalTicks;
        if (ticks >= delay && (ticks - delay) % interval == 0) {
            living.damage(world.getDamageSources().hotFloor(), (float) ModConfig.get().heatDamageAmount);
        }

        // Periodic cleanup of stale entries
        cleanupCounter++;
        if (cleanupCounter >= 200) {
            cleanupCounter = 0;
            cleanup();
        }
    }

    public static void clearEntity(Entity entity) {
        ticksInHotCauldron.remove(entity.getUuid());
    }

    private static void cleanup() {
        if (ticksInHotCauldron.size() > 100) {
            ticksInHotCauldron.clear();
        }
    }
}
