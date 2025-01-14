package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.TradeOffer;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(VillagerEntity.class)
public class PatronageMixin {

    @Inject(method = "prepareOffersFor", at = @At("HEAD"))
    private void applyDefenderDiscountTradeModifier(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer && PerkHandler.doesPlayerHavePerk(serverPlayer, "Patronage")) {
            VillagerEntity villager = (VillagerEntity) (Object) this;

            // Apply an additional discount specific to the "Patronage" perk
            for (TradeOffer tradeOffer : villager.getOffers()) {
                int additionalDiscount = MathHelper.floor((float) 50 * tradeOffer.getPriceMultiplier()); // Perk-specific discount
                tradeOffer.increaseSpecialPrice(-additionalDiscount); // Apply the discount
            }
        }
    }

    @Inject(method = "resetCustomer", at = @At("HEAD"))
    private void clearDefenderDiscount(CallbackInfo ci) {
        // No need to clear here, as the discounts added by `prepareOffersFor` are temporary
        // and scoped to the individual trade session of the interacting player.
    }
}