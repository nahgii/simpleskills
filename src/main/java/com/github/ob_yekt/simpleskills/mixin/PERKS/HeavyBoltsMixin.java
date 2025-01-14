package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossbowItem.class)
public abstract class HeavyBoltsMixin {

    // Inject into the shootAll method to apply modifications to projectiles
    @Inject(method = "shoot", at = @At("HEAD"))
    private void onShoot(
            LivingEntity shooter, ProjectileEntity projectile, int index, float speed, float divergence, float yaw, LivingEntity target, CallbackInfo ci
    ) {
        // Only modify if the projectile is a PersistentProjectileEntity
        if (projectile instanceof PersistentProjectileEntity persistentProjectile) {
            if (shooter instanceof ServerPlayerEntity player &&
                    PerkHandler.doesPlayerHavePerk(player, "Heavy Bolts")) {
                // Increase damage by x%
                double baseDamage = persistentProjectile.getDamage();
                persistentProjectile.setDamage(baseDamage * 1.45);

                // Add a custom particle effect at the time of shooting
                if (shooter.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                    // Ad dust for weight
                    serverWorld.spawnParticles(
                            ParticleTypes.END_ROD,
                            persistentProjectile.getX(), persistentProjectile.getY(), persistentProjectile.getZ(),
                            10, // More particles to give a larger effect
                            0.5, 0.3, 0.5, // Spread
                            0.1 // Particle speed
                    );

                    // Add some flame particles for energy
                    serverWorld.spawnParticles(
                            ParticleTypes.FLAME,
                            persistentProjectile.getX(), persistentProjectile.getY(), persistentProjectile.getZ(),
                            3, // 3 particles for a small flame effect
                            0.5, 0.3, 0.5, // Spread
                            0.2 // Speed
                    );
                }
            }
        }
    }
}