package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.command.ServerCommandSource;

import java.util.*;

public class SkillTabMenu {
    private static final List<String> DEFAULT_SKILL_ORDER = List.of(
            "SLAYING", "DEFENSE", "MINING", "WOODCUTTING", "EXCAVATING", "FARMING", "MAGIC"
    );
    public static boolean isTabMenuVisible = true;

    public static void toggleTabMenuVisibility(ServerCommandSource source) {
        isTabMenuVisible = !isTabMenuVisible;

        ServerPlayerEntity player = source.getPlayer();
        if (player != null) {
            if (isTabMenuVisible) {
                // Trigger an immediate tab menu update
                updateTabMenu(player);
            } else {
                // Clear the tab menu by sending an empty packet
                player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket(
                        Text.of(""), // Empty header
                        Text.of("")  // Empty footer
                ));
            }
        }
    }

    public static void updateTabMenu(ServerPlayerEntity player) {
        if (!isTabMenuVisible) return;

        DatabaseManager db = DatabaseManager.getInstance();
        StringBuilder skillInfo = new StringBuilder();

        try {
            // Header
            skillInfo.append("§6§m=======================================\n")
                    .append("§c§l⚔ Skills Overview ⚔§r\n")
                    .append("§6§m=======================================\n\n");

            // Player Class Section - Only if classes are enabled
            if (ConfigLoader.isFeatureEnabled("classes")) {
                String playerClass;
                try {
                    playerClass = db.getPlayerClass(player.getUuidAsString());
                } catch (Exception e) {
                    playerClass = "Unknown";
                    Simpleskills.LOGGER.error("Failed to fetch player class: {}", e.getMessage());
                }
                skillInfo.append(String.format("§6⚔ Class: §a%s §6⚔\n\n",
                        playerClass != null ? playerClass : "Peasant"));
            } else {
                Simpleskills.LOGGER.debug("Classes are disabled by configuration. Skipping class display in tab menu.");
            }

            // Ironman Mode Check
            boolean isIronman = db.isPlayerInIronmanMode(player.getUuidAsString());
            if (isIronman) {
                skillInfo.append("§cIronman Mode: §aENABLED\n\n");
            }

            // Skills Section
            Map<String, SkillData> skills = new HashMap<>();
            int totalLevels = 0;

            // Fetch all skills
            try (var rs = db.getPlayerSkills(player.getUuidAsString())) {
                if (rs != null) {

                    while (rs.next()) {
                        String skillName = rs.getString("skill");
                        int currentLevel = rs.getInt("level");
                        int currentXP = rs.getInt("XP");

                        if (skillName == null || skillName.equalsIgnoreCase("NONE")) {
                            continue;
                        }

                        // Create the SkillData and add to map
                        SkillData skillData = new SkillData(skillName, currentLevel, currentXP);
                        skills.put(skillName, skillData);
                        totalLevels += currentLevel;
                    }
                }
            }

            // Add skills in order with validation
            skillInfo.append("§8§m---------------------------------------\n\n");
            for (String skillName : DEFAULT_SKILL_ORDER) {
                SkillData skill = skills.get(skillName);
                if (skill != null) {
                    appendSkillInfo(skillInfo, skill);
                } else {
                    Simpleskills.LOGGER.debug("Skill {} not found for player {}", skillName, player.getName().getString());
                }
            }

            // Total Level Section
            skillInfo.append("\n§8§m---------------------------------------\n")
                    .append(String.format("§b§lTotal Level: §a%d\n", totalLevels))
                    .append("§6§m=======================================");

            // Send the tab menu to the player
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket(
                    Text.of(skillInfo.toString()),
                    Text.of("")
            ));

        } catch (Exception e) {
            Simpleskills.LOGGER.error("Failed to update tab menu for player {}", player.getName().getString(), e);
            player.sendMessage(Text.of("§cError: Failed to load skill data."));
        }
    }

    private static void appendSkillInfo(StringBuilder skillInfo, SkillData skill) {
        if (skill.currentLevel == XPManager.getMaxLevel()) {
            // Max level formatting
            skillInfo.append(String.format("§6⭐ §e%-12s §eLevel 99 §r§bXP: %,d\n",
                    skill.name,
                    skill.currentXP
            ));
        } else {
            // Calculate progress
            int XPForCurrentLevel = XPManager.getExperienceForLevel(skill.currentLevel);
            int XPToNextLevel = XPManager.getExperienceForLevel(skill.currentLevel + 1) - XPForCurrentLevel;
            int progressToNextLevel = skill.currentXP - XPForCurrentLevel;

            // Create progress bar
            String progressBar = createProgressBar(progressToNextLevel, XPToNextLevel);

            // Format skill info with progress bar
            skillInfo.append(String.format("§a%-12s §fLevel §b%-2d %s §7[§f%,d§7/§f%,d§7]\n",
                    skill.name,
                    skill.currentLevel,
                    progressBar,
                    progressToNextLevel,
                    XPToNextLevel
            ));
        }
    }

    private static String createProgressBar(int progress, int total) {
        int barLength = 10;
        if (total <= 0) total = 1;
        progress = Math.max(0, Math.min(progress, total));

        int filled = (int) ((double) progress / total * barLength);
        int empty = barLength - filled;

        return "§a" + "█".repeat(filled) + "§7" + "░".repeat(empty);
    }

    private static class SkillData {
        String name;
        int currentLevel;
        int currentXP;

        SkillData(String name, int level, int XP) {
            this.name = name;
            this.currentLevel = level;
            this.currentXP = XP;
        }
    }
}