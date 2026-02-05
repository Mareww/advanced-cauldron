package com.marew.advancedcauldron.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class CauldronSteamParticle extends SpriteBillboardParticle {

    private final SpriteProvider spriteProvider;

    protected CauldronSteamParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.spriteProvider = spriteProvider;

        this.red = 0.95F;
        this.green = 0.95F;
        this.blue = 0.95F;
        this.alpha = 0.6F;

        this.velocityX = (world.random.nextDouble() - 0.5) * 0.01;
        this.velocityY = 0.02 + world.random.nextDouble() * 0.015;
        this.velocityZ = (world.random.nextDouble() - 0.5) * 0.01;

        this.scale = 0.12F + world.random.nextFloat() * 0.08F;
        this.maxAge = 30 + world.random.nextInt(20);

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

        this.velocityX *= 0.95;
        this.velocityZ *= 0.95;

        this.move(this.velocityX, this.velocityY, this.velocityZ);

        float progress = (float) this.age / this.maxAge;
        this.alpha = 0.6F * (1.0F - progress);
        this.scale += 0.005F;

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
            return new CauldronSteamParticle(world, x, y, z, spriteProvider);
        }
    }
}
