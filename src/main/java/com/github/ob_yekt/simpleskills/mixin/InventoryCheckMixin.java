package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;
import com.github.ob_yekt.simpleskills.XPManager;
import com.github.ob_yekt.simpleskills.Skills;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

    @Inject(method = "onClosed", at = @At("HEAD"))
    private void validateArmorRequirementsOnInventoryClose(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmorSlot()) continue;

            ItemStack equippedItem = serverPlayer.getEquippedStack(slot);
            if (equippedItem.isEmpty()) continue;

            boolean shouldRemoveArmor = false;
            String removalReason = "";

            // Check skill requirements first
            Identifier itemId = Registries.ITEM.getId(equippedItem.getItem());
            SkillRequirement requirement = RequirementLoader.getArmorRequirement(itemId.toString());

            if (requirement != null) {
                Skills requiredSkill = Skills.valueOf(requirement.getSkill().toUpperCase());
                int playerSkillLevel = XPManager.getSkillLevel(serverPlayer.getUuidAsString(), requiredSkill);

                if (playerSkillLevel < requirement.getLevel()) {
                    shouldRemoveArmor = true;
                    removalReason = "(requires " + requirement.getSkill() + " level " + requirement.getLevel() + ")";
                }
            }

            // Check wizard armor restrictions if skill check passed
            if (!shouldRemoveArmor && PerkHandler.doesPlayerHavePerk(serverPlayer, "I Put on My Robe and Wizard Hat")) {
                Item item = equippedItem.getItem();
                if (item != Items.TURTLE_HELMET &&
                        item != Items.LEATHER_HELMET &&
                        item != Items.LEATHER_CHESTPLATE &&
                        item != Items.LEATHER_LEGGINGS &&
                        item != Items.LEATHER_BOOTS &&
                        item != Items.ELYTRA) {
                    shouldRemoveArmor = true;
                    removalReason = "as it is not befitting of a Wizard!";
                }
            }

            // Handle armor removal if needed
            if (shouldRemoveArmor) {
                serverPlayer.sendMessage(Text.literal("§6[simpleskills]§f Removed invalid armor: " +
                        equippedItem.getName().getString() + " " + removalReason), true);

                // Remove the armor and try to add it to inventory
                ItemStack armorToMove = equippedItem.copy();
                serverPlayer.equipStack(slot, ItemStack.EMPTY);

                PlayerInventory inventory = serverPlayer.getInventory();
                boolean addedToInventory = false;

                // Try to find an empty slot
                int emptySlot = inventory.getEmptySlot();
                if (emptySlot != -1) {
                    inventory.setStack(emptySlot, armorToMove);
                    addedToInventory = true;
                }

                // Drop the item if we couldn't add it to inventory
                if (!addedToInventory) {
                    serverPlayer.dropItem(armorToMove, false);
                }
            }
        }
    }
}