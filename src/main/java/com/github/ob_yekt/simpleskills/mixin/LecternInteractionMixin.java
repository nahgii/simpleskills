package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.XPManager;

import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@Mixin(LecternBlock.class)
public class LecternInteractionMixin {

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void initialize(AbstractBlock.Settings settings, CallbackInfo ci) {
        // Listen for the UseBlockCallback for this block
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            // Only handle interactions with Lectern blocks
            BlockState state = world.getBlockState(hitResult.getBlockPos());
            if (!(state.getBlock() instanceof LecternBlock)) return ActionResult.PASS;

            // Ensure this only runs on the server side
            if (world.isClient()) return ActionResult.PASS;

            // Check for empty hand
            ItemStack handItem = player.getStackInHand(hand);
            if (!handItem.isEmpty()) return ActionResult.PASS;

            // Lectern state debugging
            System.out.println("[SimpleSkills] Detected lectern interaction.");
            boolean hasBook = state.get(LecternBlock.HAS_BOOK);
            System.out.println("[SimpleSkills] HAS_BOOK: " + hasBook);
            if (hasBook) {
                System.out.println("[SimpleSkills] Lectern has a book - default interaction.");
                return ActionResult.PASS;
            }

            // Process empty-hand XP conversion
            if (!hasBook && player instanceof ServerPlayerEntity serverPlayer) {
                // Call the XP conversion logic
                convertExperience(serverPlayer);
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }

    /**
     * Converts all the player's XP into Magic XP and resets their experience points.
     *
     * @param player The player whose XP should be converted.
     */
    @Unique
    private void convertExperience(ServerPlayerEntity player) {
        // Calculate the player's total XP at the start of the interaction
        int totalXP = calculateTotalExperience(player);

        // Check if the player has any XP at all
        if (totalXP > 0) {
            // Conversion rate.
            int convertedXP = totalXP * (ConfigLoader.getBaseXp(Skills.MAGIC));

            // Reset the player's vanilla XP (ensure proper XP removal).
            player.setExperienceLevel(0);
            player.experienceProgress = 0;
            player.totalExperience = 0;
            player.addExperience(-totalXP); // Prevents any inconsistencies.

            // Add the converted XP to the Magic skill (using your skill system).
            XPManager.addXpWithNotification(player, Skills.MAGIC, convertedXP);

            // Notify the player *only* of successful conversion.
            player.sendMessage(
                    Text.literal("[SimpleSkills] Successfully converted " + totalXP + " XP into " + convertedXP + " Magic XP!"),
                    false
            );

            // Log the successful conversion for debugging.
            System.out.println("[SimpleSkills] Successfully converted " + totalXP + " XP into " + convertedXP + " Magic XP.");
        } else {
            // (No XP conversion happened) Notify the player only in this case.
            player.sendMessage(
                    Text.literal("[SimpleSkills] You do not have any XP to convert."),
                    false
            );

            // Log the lack of XP for debugging.
            System.out.println("[SimpleSkills] Player has no XP points to convert.");
        }
    }

    /**
     * Calculates the total experience points of the player.
     *
     * @param player The player whose XP needs to be calculated.
     * @return The total experience points.
     */

    @Unique
    private int calculateTotalExperience(ServerPlayerEntity player) {
        int level = player.experienceLevel; // The player's current level.
        float progress = player.experienceProgress; // Progress toward the next level (0.0 - 1.0).

        // Calculate total XP from levels.
        int totalXP;
        if (level < 17) { // Levels 0–16
            totalXP = (level * level) + (6 * level);
        } else if (level < 32) { // Levels 17–31
            totalXP = (int) ((2.5 * level * level) - (40.5 * level) + 360);
        } else { // Level 32 and above
            totalXP = (int) ((4.5 * level * level) - (162.5 * level) + 2220);
        }

        // Add progress toward the next level (partial XP within current level).
        int xpToNextLevel = calculatePointsToNextLevel(level);
        totalXP += Math.round(progress * xpToNextLevel);

        return totalXP;
    }

    /**
     * Calculates the XP required to reach the next level.
     *
     * @param level The current level of the player.
     * @return The XP required for the next level.
     */

    @Unique
    private int calculatePointsToNextLevel(int level) {
        if (level < 16) {
            return 2 * level + 7;
        } else if (level < 31) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }
}