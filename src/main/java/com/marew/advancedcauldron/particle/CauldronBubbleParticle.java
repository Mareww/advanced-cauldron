package com.marew.advancedcauldron.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class CauldronBubbleParticle extends SpriteBillboardParticle {

    private final SpriteProvider spriteProvider;

    protected CauldronBubbleParticle(ClientWorld world, double x, double y, double z,
                                      double r, double g, double b, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.spriteProvider = spriteProvider;

        this.red = (float) r;
        this.green = (float) g;
        this.blue = (float) b;
        this.alpha = 0.9F;

        this.velocityX = (world.random.nextDouble() - 0.5) * 0.01;
        this.velocityY = 0.002 + world.random.nextDouble() * 0.003;
        this.velocityZ = (world.random.nextDouble() - 0.5) * 0.01;

        this.scale = 0.03F + world.random.nextFloat() * 0.02F;
        this.maxAge = 6 + world.random.nextInt(6);

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

        float progress = (float) this.age / this.maxAge;
        this.alpha = 0.9F * (1.0F - progress * 0.5F);

        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new CauldronBubbleParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider);
        }
    }
}
