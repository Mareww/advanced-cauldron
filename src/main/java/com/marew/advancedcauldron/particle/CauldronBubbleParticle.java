package com.marew.advancedcauldron.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class CauldronBubbleParticle extends SpriteBillboardParticle {

    private final SpriteProvider spriteProvider;

    protected CauldronBubbleParticle(ClientWorld world, double x, double y, double z,
                                      double r, double g, double b, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.spriteProvider = spriteProvider;

        // Use velocity params as RGB color (0.0-1.0)
        this.red = (float) Math.max(0.0, Math.min(1.0, r));
        this.green = (float) Math.max(0.0, Math.min(1.0, g));
        this.blue = (float) Math.max(0.0, Math.min(1.0, b));
        this.alpha = 0.8F;

        // Bubble-like behavior - barely rise, pop at surface
        this.velocityX = 0.0;
        this.velocityY = 0.002 + world.random.nextDouble() * 0.003; // Very slow rise
        this.velocityZ = 0.0;

        this.scale = 0.03F + world.random.nextFloat() * 0.02F; // Small bubbles
        this.maxAge = 6 + world.random.nextInt(6); // Quick pop but lasts long enough to overlap

        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        this.move(this.velocityX, this.velocityY, this.velocityZ);

        // Quick pop at end - scale up then disappear
        if (this.age >= this.maxAge - 1) {
            this.scale *= 1.5F;
            this.alpha = 0.3F;
        }

        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            // velocityX, velocityY, velocityZ are used as R, G, B (0.0-1.0)
            return new CauldronBubbleParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider);
        }
    }
}
