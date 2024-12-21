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

        int totalLevels = 0; // To store the sum of all skill levels

        try (var rs = db.getPlayerSkills(player.getUuidAsString())) {
            while (rs.next()) {
                // Retrieve skill information
                String skillName = rs.getString("skill");
                int currentLevel = rs.getInt("level");
                int currentXp = rs.getInt("xp");

                totalLevels += currentLevel; // Add current level to total

                if (currentLevel == XPManager.getMaxLevel()) {
                    // Special formatting for max-level skills
                    String formattedSkillInfo = String.format(
                            "§6⭐ §e%-15s §e§l%-10s §r§bXP: %10d",
                            skillName, "Level 99", currentXp
                    );
                    skillInfo.append("§8---------------------------------------\n");
                    skillInfo.append(formattedSkillInfo).append("\n");
                } else {
                    // Calculate total XP requirement for next level
                    int xpForCurrentLevel = XPManager.getExperienceForLevel(currentLevel);
                    int xpToNextLevel = XPManager.getExperienceForLevel(currentLevel + 1) - xpForCurrentLevel;
                    int progressToNextLevel = currentXp - xpForCurrentLevel;

                    // Standard skill row formatting without progress bar
                    String formattedSkillInfo = String.format(
                            "§a%-15s §6Level %-7d §6XP: §e%7d §6/ §e%7d",
                            skillName,                // Skill name, left-aligned to 15 characters
                            currentLevel,             // Level, left-aligned to 7 characters
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

        // Add the Total Levels section after listing individual skills
        skillInfo.append("§8---------------------------------------\n");
        skillInfo.append(String.format("§b§lTotal Level: §a%d\n", totalLevels)); // Bold + blue styling for "Total Levels"

        // Footer styling
        skillInfo.append("§6§m=======================================\n");

        // Send the styled skill list to the player
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket(
                Text.of(skillInfo.toString()), // Header
                Text.of("") // No footer
        ));
    }
}