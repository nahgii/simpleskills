package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.XPManager;
import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantingTableBlock.class)
public class EnchantingTableInteractionMixin {

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void initialize(CallbackInfo ci) {
        // Listen for the UseBlockCallback for any enchanting table interaction
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;

            BlockState blockState = world.getBlockState(hitResult.getBlockPos());

            // Ensure interaction is with an enchanting table
            if (!(blockState.getBlock() instanceof EnchantingTableBlock)) return ActionResult.PASS;

            if (player instanceof ServerPlayerEntity serverPlayer) {
                // Check requirements for the enchanting table
                String blockID = blockState.getBlock().getTranslationKey(); // Enchanting Table block ID
                SkillRequirement requirement = RequirementLoader.getMagicRequirement(blockID);

                if (requirement == null) return ActionResult.PASS; // Allow if no restrictions found

                // Get the player's Magic level
                int playerMagicLevel = XPManager.getSkillLevel(serverPlayer.getUuidAsString(), Skills.MAGIC);

                if (playerMagicLevel < requirement.getLevel()) {
                    // Deny interaction and notify the player
                    player.sendMessage(Text.literal("[SimpleSkills] You need Magic level "
                            + requirement.getLevel() + " to use the enchanting table!"), true);
                    return ActionResult.FAIL; // Cancel the player's interaction
                }
            }
            // Allow if requirements are met
            return ActionResult.PASS;
        });
    }
}