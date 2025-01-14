package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatusEffectInstance.class)
public class ScavengerStatusEffectMixin {
    @Inject(method = "onApplied", at = @At("HEAD"))
    private void preventHungerEffect(LivingEntity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayerEntity player &&
                PerkHandler.doesPlayerHavePerk(player, "Scavenger")) {
            StatusEffectInstance effect = (StatusEffectInstance) (Object) this;
            if (effect.getEffectType() == StatusEffects.HUNGER) {
                // Remove the hunger effect immediately after it's applied
                player.removeStatusEffect(StatusEffects.HUNGER);
            }
        }
    }
}