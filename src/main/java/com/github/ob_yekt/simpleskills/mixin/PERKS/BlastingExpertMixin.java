package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class BlastingExpertMixin {
    @Unique
    private static final float DAMAGE_REDUCTION_PERCENT = 0.20f;  // 20% damage reduction from Blasting Expert

    @Inject(
            method = "modifyAppliedDamage",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onModifyDamage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof ServerPlayerEntity player) {
            if (PerkHandler.doesPlayerHavePerk(player, "Blasting Expert")) {
                if (source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.PLAYER_EXPLOSION)) {
                    // Current damage after all other reductions (including armor and enchantments)
                    float currentAmount = cir.getReturnValue();

                    // Apply the Blasting Expert perk's damage reduction
                    // We reduce the damage by 40%, but we need to ensure it doesn't go below 0
                    float reducedDamage = currentAmount * (1.0f - DAMAGE_REDUCTION_PERCENT);

                    // Set the new reduced damage
                    cir.setReturnValue(Math.max(reducedDamage, 0.0f));
                }
            }
        }
    }
}