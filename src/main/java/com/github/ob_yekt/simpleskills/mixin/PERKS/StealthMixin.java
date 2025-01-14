package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.Entity;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class StealthMixin {

    @Inject(method = "setFlag", at = @At("RETURN"))
    private void onFlagChange(int flag, boolean value, CallbackInfo ci) {
        // Only process sneaking flag (flag == 1)
        if (flag == 1) {
            // Ensure this is a player instance
            if ((Object) this instanceof ServerPlayerEntity player) {
                if (PerkHandler.doesPlayerHavePerk(player, "Stealth")) {
                    var perk = PerkHandler.getPerk("Stealth");
                    if (perk != null) {
                        perk.onSneakChange(player, value);
                    }
                }
            }
        }
    }
}