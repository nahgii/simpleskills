package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.PotionEffectHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatusEffectInstance.class)
public class PotionMixin {
    @Inject(method = "onApplied", at = @At("HEAD"))
    private void onEffectApplied(LivingEntity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayerEntity player && !PotionEffectHandler.isProcessing()) {
            PotionEffectHandler.handleEffectApplication(player, (StatusEffectInstance)(Object)this);
        }
    }
}