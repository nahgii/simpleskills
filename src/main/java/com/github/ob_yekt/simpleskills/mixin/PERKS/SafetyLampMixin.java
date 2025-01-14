package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle off-hand inventory changes for Safety Lamp perk.
 */
@Mixin(PlayerInventory.class)
public abstract class SafetyLampMixin {

    /**
     * Inject into a method that triggers when the inventory is updated.
     */
    @Inject(method = "updateItems", at = @At("TAIL"))
    private void onInventoryUpdate(CallbackInfo ci) {
        PlayerInventory inventory = (PlayerInventory) (Object) this;

        // Check if we're on the server side and the player is valid
        if (inventory.player instanceof ServerPlayerEntity player) {
            // Check if the player has the Safety Lamp perk
            if (PerkHandler.doesPlayerHavePerk(player, "Safety Lamp")) {
                // Handle Night Vision effect based on off-hand item
                handleNightVision(player);
            }
        }
    }

    /**
     * Logic to apply or remove Night Vision effect based on off-hand item.
     */
    @Unique
    private void handleNightVision(ServerPlayerEntity player) {
        ItemStack offHandStack = player.getOffHandStack();

        if (offHandStack.getItem() == Items.TORCH ||
                offHandStack.getItem() == Items.SOUL_TORCH ||
                offHandStack.getItem() == Items.LANTERN ||
                offHandStack.getItem() == Items.SOUL_LANTERN) {
            // Check if the effect is already applied
            if (!player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                // Apply the effect if a Torch is in off-hand and Night Vision isn't active
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false));
            }
        } else {
            // Remove the Night Vision effect if a Torch is not in the off-hand
            if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        }
    }
}