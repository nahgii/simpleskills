package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SkillTabMenu {
    public static void updateTabMenu(ServerPlayerEntity player) {
        DatabaseManager db = DatabaseManager.getInstance();

        // Header
        StringBuilder skillInfo = new StringBuilder();
        skillInfo.append("§6§m=======================================\n"); // Gold bar
        skillInfo.append("§c§l⚔ Skills Overview ⚔§r\n"); // Red + bold with special symbols
        skillInfo.append("§6§m=======================================\n");

        try (var rs = db.getPlayerSkills(player.getUuidAsString())) {
            while (rs.next()) {
                // Retrieve skill information
                String skillName = rs.getString("skill");
                int currentLevel = rs.getInt("level");
                int currentXp = rs.getInt("xp");

                if (currentLevel == XPManager.getMaxLevel()) {
                    // Special formatting for max-level skills
                    String formattedSkillInfo = String.format(
                            "§6⭐ §e%-15s §e§l%-10s §r§bXP: %10d",
                            skillName, "Level 99", currentXp
                    );
                    skillInfo.append("§8---------------------------------------\n");
                    skillInfo.append(formattedSkillInfo).append("\n");
                } else {
                    // Calculate progression
                    int xpForCurrentLevel = XPManager.getExperienceForLevel(currentLevel);
                    int xpToNextLevel = XPManager.getExperienceForLevel(currentLevel + 1) - xpForCurrentLevel;
                    int progressToNextLevel = currentXp - xpForCurrentLevel;

                    // Fixed-length progress bar
                    String progressBar = createProgressBar(progressToNextLevel, xpToNextLevel);

                    // Standard skill row formatting with aligned XP columns
                    String formattedSkillInfo = String.format(
                            "§a%-15s §6Level %-7d §7[§6%-20s§7] §6XP: §e%7d §6/ §e%7d",
                            skillName,                // Skill name, left-aligned to 15 characters
                            currentLevel,             // Level, left-aligned to 7 characters
                            progressBar,              // Progress bar, 20 slots wide
                            progressToNextLevel,      // XP progress, right-aligned to 7 characters
                            xpToNextLevel             // XP to next level, right-aligned to 7 characters
                    );
                    skillInfo.append("§8---------------------------------------\n");
                    skillInfo.append(formattedSkillInfo).append("\n");
                }
            }
        } catch (Exception e) {
            Simpleskills.LOGGER.error("Failed to fetch skill data for player {}", player.getUuidAsString(), e);
            skillInfo.append("§4§lError: §cUnable to load skill data.\n"); // Error message formatting
        }

        // Footer styling
        skillInfo.append("§6§m=======================================\n");

        // Send the styled skill list to the player
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket(
                Text.of(skillInfo.toString()), // Header
                Text.of("") // No footer
        ));
    }

    /**
     * Helper method to create a more thematic progress bar
     *
     * @param progress Current XP progress toward the next level
     * @param total    Total XP needed for the next level
     * @return A medieval-themed progress bar string
     */
    private static String createProgressBar(int progress, int total) {
        int progressBarLength = 20; // Length of the progress bar
        int filledBars = (int) ((double) progress / total * progressBarLength);
        int emptyBars = progressBarLength - filledBars;

        // Metallic-themed progress bar
        return "§6" + "█".repeat(filledBars) + "§8" + "░".repeat(emptyBars); // Gold and dark gray
    }
}