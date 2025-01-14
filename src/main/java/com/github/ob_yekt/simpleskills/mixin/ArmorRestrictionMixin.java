package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;
import com.github.ob_yekt.simpleskills.XPManager;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

    @Inject(method = "equipStack", at = @At("HEAD"), cancellable = true)
    private void restrictArmorEquip(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return;
        if (!slot.isArmorSlot() || stack.isEmpty()) return;

        boolean shouldPreventEquip = false;
        String preventReason = "";

        // Check skill requirements first
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        SkillRequirement requirement = RequirementLoader.getArmorRequirement(itemId.toString());

        if (requirement != null) {
            int playerLevel = XPManager.getSkillLevel(
                    player.getUuidAsString(),
                    Skills.valueOf(requirement.getSkill().toUpperCase())
            );

            if (playerLevel < requirement.getLevel()) {
                shouldPreventEquip = true;
                preventReason = "You need " + requirement.getSkill() +
                        " level " + requirement.getLevel() +
                        " to equip this item!";
            }
        }

        // Check wizard armor restrictions if skill check passed
        if (!shouldPreventEquip && PerkHandler.doesPlayerHavePerk(player, "I Put on My Robe and Wizard Hat")) {
            Item item = stack.getItem();
            if (item != Items.TURTLE_HELMET &&
                    item != Items.LEATHER_HELMET &&
                    item != Items.LEATHER_CHESTPLATE &&
                    item != Items.LEATHER_LEGGINGS &&
                    item != Items.LEATHER_BOOTS &&
                    item != Items.ELYTRA) {
                shouldPreventEquip = true;
                preventReason = "This item is not befitting of a Wizard!";
            }
        }

        // Handle prevention if needed
        if (shouldPreventEquip) {
            player.sendMessage(Text.literal("§6[simpleskills]§f " + preventReason), true);

            // Drop the item on the ground to prevent equipping
            player.dropItem(stack.copy(), false);

            // Cancel equipping
            ci.cancel();
        }
    }
}
