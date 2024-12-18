package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;
import com.github.ob_yekt.simpleskills.XPManager;
import com.github.ob_yekt.simpleskills.Skills;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.screen.ScreenHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to validate equipped armor requirements when the inventory screen is closed.
 */
@Mixin(ScreenHandler.class)
public abstract class InventoryCheckMixin {

    /**
     * Called when the player closes their inventory. Checks all equipped armor slots
     * to verify if the player meets the skill requirements for them.
     */
    @Inject(method = "onClosed", at = @At("HEAD"))
    private void validateArmorRequirementsOnInventoryClose(PlayerEntity player, CallbackInfo ci) {
        // Ensure we're working with a server-side player
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Loop through all equipment slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            // Skip non-armor slots
            if (!slot.isArmorSlot()) continue;

            // Get the equipped item in the current slot
            ItemStack equippedItem = serverPlayer.getEquippedStack(slot);

            // Skip if there's no item equipped
            if (equippedItem.isEmpty()) continue;

            // Get the item identifier
            Identifier itemId = Registries.ITEM.getId(equippedItem.getItem());

            // Fetch the skill requirements for the armor piece
            SkillRequirement requirement = RequirementLoader.getArmorRequirement(itemId.toString());

            // If no requirement exists, allow the armor to stay equipped
            if (requirement == null) continue;

            // Fetch the player's skill level for the required skill
            Skills requiredSkill = Skills.valueOf(requirement.getSkill().toUpperCase());
            int playerSkillLevel = XPManager.getSkillLevel(serverPlayer.getUuidAsString(), requiredSkill);

            // If the player does not meet the required level, remove the armor
            if (playerSkillLevel < requirement.getLevel()) {
                serverPlayer.sendMessage(Text.of("[SimpleSkills] Removed invalid armor: " +
                        equippedItem.getName().getString() +
                        " (requires " + requirement.getSkill() + " level " + requirement.getLevel() + ")"), true);

                // Drop the invalid armor and clear the slot
                serverPlayer.dropItem(equippedItem.copy(), false);
                serverPlayer.equipStack(slot, ItemStack.EMPTY);
            }
        }
    }
}