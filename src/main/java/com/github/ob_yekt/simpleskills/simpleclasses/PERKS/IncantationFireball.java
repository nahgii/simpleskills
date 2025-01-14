package com.github.ob_yekt.simpleskills.simpleclasses.PERKS;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.SmallFireballEntity;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class IncantationFireball extends SmallFireballEntity {

    public IncantationFireball(EntityType<? extends SmallFireballEntity> entityType, World world) {
        super(entityType, world);
    }

    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);

        if (!this.getWorld().isClient) {
            Entity entity = entityHitResult.getEntity();  // Get the entity hit by the fireball
            explodeAtFeet(entity);  // Pass the entity to explodeAtFeet
        }
        this.discard(); // Remove the fireball after collision
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        // Directly implement custom behavior without invoking the super method
        World world = this.getWorld();
        if (!world.isClient) {

            explode();
            // Immediately discard the fireball to prevent further behavior (like setting fire)
            this.discard();
        }
    }

    private void explodeAtFeet(Entity entity) {
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();
            float explosionPower = 2.6F; // Adjust explosion power here for direct hits

            // different for mob hits
            serverWorld.createExplosion(
                    this,
                    x, y + 1.8, z,
                    explosionPower,
                    false, // Prevent fire creation
                    World.ExplosionSourceType.NONE);

            // Spawn explosion particles
            createExplosionParticles(serverWorld, x, y + 0.8, z);
        }
    }

    private void explode() {
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();
            float explosionPower = 2.2F;


            // Create an explosion that only affects entities
            serverWorld.createExplosion(
                    this,
                    x, y, z,
                    explosionPower,
                    false, // Prevent fire creation
                    World.ExplosionSourceType.NONE);
            createExplosionParticles(serverWorld, x, y, z);
        }
    }

    private void createExplosionParticles(ServerWorld serverWorld, double x, double y, double z) {
        // Core burst - Sparkling particles at the explosion's center
        serverWorld.spawnParticles(
                ParticleTypes.EXPLOSION,
                x, y, z,
                3,
                0.3, 0.3, 0.3,
                0.2
        );

        // Core burst - Sparkling particles at the explosion's center
        serverWorld.spawnParticles(
                ParticleTypes.END_ROD,
                x, y, z,
                40,
                0.3, 0.3, 0.3,
                0.1
        );

        // Colorful sparkles spreading outward
        serverWorld.spawnParticles(
                ParticleTypes.ENCHANTED_HIT,
                x, y, z,
                50,
                1.8, 1.8, 1.8,
                0.5
        );

        // Colorful sparkles spreading outward
        serverWorld.spawnParticles(
                ParticleTypes.WITCH,
                x, y, z,
                50,
                1.8, 1.8, 1.8,
                0.5
        );

        // Lingering magical mist
        serverWorld.spawnParticles(
                ParticleTypes.ASH,
                x, y, z,
                30,
                1.2, 1.2, 1.2,
                0.05
        );

        // Glowing embers slowly rising
        serverWorld.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                x, y, z,
                20,
                0.5, 1.0, 0.5,
                0.02
        );
    }
}
