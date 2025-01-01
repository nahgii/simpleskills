package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.function.BiConsumer;

public class XPManager {
    private static final int MAX_LEVEL = 99; // Adjusted to match RuneScape max level

    // XPChangeListener - triggered on XP or level change
    private static BiConsumer<ServerPlayerEntity, Skills> onXpChangeListener;

    // Provide access to MAX_LEVEL
    public static int getMaxLevel() {
        return MAX_LEVEL;
    }

    // Set an optional listener for XP and level changes
    public static void setOnXpChangeListener(BiConsumer<ServerPlayerEntity, Skills> listener) {
        onXpChangeListener = listener;

    }

    ///  XP LOGIC

    public static int getExperienceForLevel(int level) {
        if (level == 0) return 0;

        double baseXp = 100;

        if (level <= 10) {
            // Early game: gentle scaling
            return (int) Math.floor(baseXp * Math.pow(level, 1.4)); // Slightly steeper curve
        } else {
            // Mid to late game: moderate scaling
            int earlyXp = (int) Math.floor(baseXp * Math.pow(10, 1.4)); // XP required for level 10
            double harderScaling = Math.pow(level - 10, 1.65); // Adjust exponent for smoother growth
            return earlyXp + (int) Math.floor(harderScaling * 275); // Adjust multiplier for balanced difficulty
        }
    }

    // Inverse method to find the level for a given XP amount
    public static int getLevelForExperience(int experience) {
        int level = 0;
        while (getExperienceForLevel(level + 1) <= experience) {
            level++;
            if (level >= MAX_LEVEL) {
                return MAX_LEVEL; // Prevent levels from exceeding MAX_LEVEL
            }
        }
        return level;
    }

    // Method to get XP required for the next level
    public static int getXpToNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return Integer.MAX_VALUE; // No more leveling up for max level
        }
        return getExperienceForLevel(currentLevel + 1) - getExperienceForLevel(currentLevel);
    }

    // Add XP to a player's skill and notify them
    public static void addXpWithNotification(ServerPlayerEntity player, Skills skill, int xpToAdd) {
        String playerUuid = player.getUuidAsString();
        DatabaseManager db = DatabaseManager.getInstance();

        // Fetch current XP and level from the database
        int currentXp = 0;
        int currentLevel = 0;

        try (var rs = db.getPlayerSkills(playerUuid)) {
            while (rs.next()) {
                if (rs.getString("skill").equalsIgnoreCase(skill.name())) {
                    currentXp = rs.getInt("xp");
                    currentLevel = rs.getInt("level");
                    break;
                }
            }
        } catch (Exception e) {
            Simpleskills.LOGGER.error("Error reading skill data for player UUID: {}", playerUuid, e);
            return;
        }

        // Add XP and determine the new potential level
        int newXp = currentXp + xpToAdd;
        int newLevel = getLevelForExperience(newXp);

        // Cap levels at MAX_LEVEL but continue tracking total XP
        newLevel = Math.min(newLevel, MAX_LEVEL);
        boolean leveledUp = newLevel > currentLevel;

        // Save the updated XP and level to the database (level doesn't exceed MAX_LEVEL)
        db.savePlayerSkill(playerUuid, skill.name(), newXp, newLevel);

        // Send notifications to the player
        String xpMessage = "[SimpleSkills] You gained " + xpToAdd + " XP in " + skill.getDisplayName() + "!";
        player.sendMessage(Text.of(xpMessage), true);

        if (leveledUp) {
            String levelUpMessage;
            if (newLevel == MAX_LEVEL) {
                // Notify player they have reached max level
                levelUpMessage = "[SimpleSkills] You have reached the maximum level of 99 in " + skill.getDisplayName() + "!";


            } else {
                levelUpMessage = "[SimpleSkills] You leveled up in " + skill.getDisplayName() + "! New level: " + newLevel;
            }

            player.sendMessage(Text.of(levelUpMessage), false);
            Simpleskills.LOGGER.info("Player {} leveled up in {}! New level: {}", playerUuid, skill.name(), newLevel);

        }

        // Trigger XPChangeListener if it exists
        if (onXpChangeListener != null) {
            onXpChangeListener.accept(player, skill);
        }
        // Update the tab menu after XP or level change
        SkillTabMenu.updateTabMenu(player);
    }

    // Existing method to get a player's skill level
    public static int getSkillLevel(String playerUuid, Skills skill) {
        DatabaseManager db = DatabaseManager.getInstance();

        try (var rs = db.getPlayerSkills(playerUuid)) {
            while (rs.next()) {
                if (rs.getString("skill").equalsIgnoreCase(skill.name())) {
                    return rs.getInt("level");
                }
            }
        } catch (Exception e) {
            Simpleskills.LOGGER.error("Error while fetching skill level for player UUID: {}", playerUuid, e);
        }

        return 0;
    }
}