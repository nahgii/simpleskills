package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class ScavengerExhaustionMixin {

    @Shadow
    private float exhaustion;

    @Unique
    private ServerPlayerEntity player; // Reference to the player

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void reduceExhaustion(float exhaustionValue, CallbackInfo ci) {
        // If player is null, we can't proceed
        if (player == null) {
            return;
        }

        // Check if the player has the Scavenger perk
        if (PerkHandler.doesPlayerHavePerk(player, "Scavenger")) {
            // Reduce exhaustion by 50%
            exhaustionValue *= 0.75f;
        }

        // Apply the reduced exhaustion value
        this.exhaustion = Math.min(this.exhaustion + exhaustionValue, 40.0F);
        ci.cancel(); // Prevent the original method from running
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void capturePlayer(ServerPlayerEntity player, CallbackInfo ci) {
        this.player = player; // Associate player with HungerManager
    }
}
