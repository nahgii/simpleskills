package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class FortitudeMixin {
    @Inject(method = "update", at = @At("HEAD"))
    private void enforceMinimumHunger(ServerPlayerEntity player, CallbackInfo ci) {
        if (PerkHandler.doesPlayerHavePerk(player, "Fortitude")) {
            if (((HungerManager)(Object)this).getFoodLevel() < 7) {
                ((HungerManager)(Object)this).setFoodLevel(7);
            }
        }
    }
}