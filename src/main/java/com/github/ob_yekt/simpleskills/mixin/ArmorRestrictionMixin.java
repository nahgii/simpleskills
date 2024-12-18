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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to restrict armor equipping based on player skill levels.
 */
@Mixin(PlayerEntity.class)
public abstract class ArmorRestrictionMixin {

    /**
     * Inject into the `equipStack` method to check and restrict armor equipping.
     */
    @Inject(method = "equipStack", at = @At("HEAD"), cancellable = true)
    private void restrictArmorEquip(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        // Cast the current entity to PlayerEntity
        PlayerEntity self = (PlayerEntity) (Object) this;

        // If not a ServerPlayerEntity, return as it's not relevant
        if (!(self instanceof ServerPlayerEntity player)) return;

        // Only consider slots meant for armor
        if (!slot.isArmorSlot()) return;

        // If the stack is empty (e.g., unequipping) allow the action
        if (stack.isEmpty()) return;

        // Get the armor item as an Identifier
        Identifier itemId = Registries.ITEM.getId(stack.getItem());

        // Fetch armor requirements for this item from the JSON loader
        SkillRequirement requirement = RequirementLoader.getArmorRequirement(itemId.toString());

        if (requirement == null) return; // If no requirement found, allow equipping

        int playerLevel = XPManager.getSkillLevel(
                player.getUuidAsString(),
                Skills.valueOf(requirement.getSkill().toUpperCase())
        );

        // If the player does not meet the skill level requirement
        if (playerLevel < requirement.getLevel()) {
            player.sendMessage(Text.literal("[SimpleSkills] You need " +
                            requirement.getSkill() + " level " +
                            requirement.getLevel() + " to equip this item!"),
                    true
            );

            // Drop the item on the ground to prevent equipping
            player.dropItem(stack.copy(), false);

            // Cancel equipping
            ci.cancel();
        }
    }
}