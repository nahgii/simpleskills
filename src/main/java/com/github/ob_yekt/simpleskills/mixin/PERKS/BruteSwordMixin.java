package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class BruteSwordMixin {

    @Inject(method = "getDamageAgainst", at = @At("RETURN"), cancellable = true)
    private void modifyDamageForBrute(Entity target, float baseDamage, DamageSource source, CallbackInfoReturnable<Float> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ItemStack mainHand = player.getMainHandStack();

        if (mainHand.getItem() instanceof SwordItem && PerkHandler.doesPlayerHavePerk(player, "Brute")) {
            float reducedDamage = cir.getReturnValue() * 0.25f; // 75% reduction

            // Visual feedback
            player.getServerWorld().spawnParticles(
                    ParticleTypes.SMOKE,
                    target.getX(), target.getY() + 1, target.getZ(),
                    5, // number of particles
                    0.2, 0.2, 0.2, // spread
                    0.3 // speed
            );

            // Play sound
            target.getWorld().playSound(
                    null,
                    player.getBlockPos(),
                    SoundEvents.ENTITY_ARMOR_STAND_HIT,
                    SoundCategory.PLAYERS,
                    0.7F,
                    0.7F
            );

            cir.setReturnValue(reducedDamage);
        }
    }
}