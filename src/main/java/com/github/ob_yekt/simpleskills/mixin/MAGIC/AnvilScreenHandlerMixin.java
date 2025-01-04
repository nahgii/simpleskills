package com.github.ob_yekt.simpleskills.mixin.MAGIC;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.XPManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilScreenHandler.class)
public class AnvilScreenHandlerMixin {

    @Inject(method = "onTakeOutput", at = @At("HEAD"), cancellable = true)
    private void onTakeOutput(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Check for enchantments in the output stack and verify the Magic level
            for (var enchantment : stack.getEnchantments().getEnchantments()) {
                String enchantId = enchantment.getIdAsString();

                // Get the enchantment's requirement from the magic_requirements.json
                SkillRequirement requirement = RequirementLoader.getMagicRequirement(enchantId);

                if (requirement != null) {
                    // Get the player's Magic level
                    int playerMagicLevel = XPManager.getSkillLevel(serverPlayer.getUuidAsString(), Skills.MAGIC);

                    // If the player's Magic level is too low, deny the interaction
                    if (playerMagicLevel < requirement.getLevel()) {
                        serverPlayer.sendMessage(Text.literal("[SimpleSkills] You need Magic level " + requirement.getLevel()
                                + " to apply this enchantment!"), true);
                        ci.cancel(); // Cancel the operation
                        clearDraggedItem(serverPlayer); // Clear the dragged item
                        return;
                    }
                }
            }

            // Grant XP based on anvil level cost if the player can enchant
            grantAnvilXP(serverPlayer);
        }
    }

    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void canTakeOutput(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        if (present && player instanceof ServerPlayerEntity serverPlayer) {
            // Access the output slot directly by index (index 2 for output slot)
            ItemStack outputStack = ((AnvilScreenHandler) (Object) this).slots.get(2).getStack();

            // Check for enchantments in the output stack and verify the Magic level
            for (var enchantment : outputStack.getEnchantments().getEnchantments()) {
                String enchantId = enchantment.getIdAsString();

                // Get the enchantment's requirement from the magic_requirements.json
                SkillRequirement requirement = RequirementLoader.getMagicRequirement(enchantId);

                if (requirement != null) {
                    // Get the player's Magic level
                    int playerMagicLevel = XPManager.getSkillLevel(serverPlayer.getUuidAsString(), Skills.MAGIC);

                    // If the player's Magic level is too low, prevent taking the item
                    if (playerMagicLevel < requirement.getLevel()) {
                        cir.setReturnValue(false);
                        serverPlayer.sendMessage(Text.literal("[SimpleSkills] You need Magic level " + requirement.getLevel()
                                + " to apply this enchantment!"), true);
                        return;
                    }
                }
            }
        }
    }

    @Unique
    private void grantAnvilXP(ServerPlayerEntity player) {
        // Access the level cost of the anvil operation
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
        int levelCost = handler.getLevelCost();

        if (levelCost > 0) {
            // Calculate and grant XP
            int xpToGrant = ConfigLoader.getBaseXp(Skills.MAGIC) * 10 * levelCost;
            XPManager.addXpWithNotification(player, Skills.MAGIC, xpToGrant);
        }
    }

    @Unique
    private void clearDraggedItem(ServerPlayerEntity player) {
        // Access the player's current screen handler and clear the cursor stack (the dragged item)
        player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
    }
}
