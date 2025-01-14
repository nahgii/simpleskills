package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;

import com.github.ob_yekt.simpleskills.simpleclasses.ClassMapping;
import com.github.ob_yekt.simpleskills.simpleclasses.Perk;
import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import com.github.ob_yekt.simpleskills.simpleclasses.PlayerClass;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.function.BiConsumer;

public class XPManager {
    private static final int MAX_LEVEL = 99; // Adjusted to match RuneScape max level

    // XPChangeListener - triggered on XP or level change
    private static BiConsumer<ServerPlayerEntity, Skills> onXPChangeListener;

    // Provide access to MAX_LEVEL
    public static int getMaxLevel() {
        return MAX_LEVEL;
    }

    // Set an optional listener for XP and level changes
    public static void setOnXPChangeListener(BiConsumer<ServerPlayerEntity, Skills> listener) {
        onXPChangeListener = listener;
    }

    /// XP LOGIC with Runescape-style leveling function
    public static int getExperienceForLevel(int level) {
        if (level == 1) return 0; // Level 1 starts with 0 XP

        int points = 0;
        int totalExperience = 0;
        for (int currentLevel = 1; currentLevel <= level; currentLevel++) {
            // Runescape formula for calculating points for the level
            points += (int) Math.floor(currentLevel + 300.0 * Math.pow(2.0, currentLevel / 7.0));
            if (currentLevel >= level) {
                return totalExperience; // Return calculated XP
            }
            totalExperience = (int) Math.floor((double) points / 4);
        }

        return 0;
    }

    // Inverse method to find the level for a given XP amount
    public static int getLevelForExperience(int experience) {
        int level = 1; // Start at level 1
        while (level < MAX_LEVEL && getExperienceForLevel(level + 1) <= experience) {
            level++;
        }
        return level;
    }

    // Add XP to a player's skill and notify them
    public static void addXPWithNotification(ServerPlayerEntity player, Skills skill, int XPToAdd) {
        String playerUuid = player.getUuidAsString();
        DatabaseManager db = DatabaseManager.getInstance();

        // Fetch current XP and level from the database
        int currentXP = 0;
        int currentLevel = 0;

        try (var rs = db.getPlayerSkills(playerUuid)) {
            while (rs.next()) {
                if (rs.getString("skill").equalsIgnoreCase(skill.name())) {
                    currentXP = rs.getInt("XP");
                    currentLevel = rs.getInt("level");
                    break;
                }
            }
        } catch (Exception e) {
            Simpleskills.LOGGER.error("Error reading skill data for player UUID: {}", playerUuid, e);
            return;
        }

        // Retrieve the player's class and perks
        String playerClassName = db.getPlayerClass(playerUuid);

        if (playerClassName != null) {
            try {
                // Locate the PlayerClass enum for the player's class
                PlayerClass playerClass = PlayerClass.valueOf(playerClassName.toUpperCase());

                // Check if the skill matches the player's primary skill
                if (skill.name().equalsIgnoreCase(playerClass.getPrimarySkill())) {
                    XPToAdd = (int) Math.round(XPToAdd * playerClass.getXPBonusMultiplier()); // Apply the multiplier for primary skills (rounded)
                }
            } catch (IllegalArgumentException e) {
                Simpleskills.LOGGER.warn("Unknown class '{}' for player UUID: {}", playerClassName, playerUuid);
            }
        }

        // Apply the perk-based XP modifications
        for (String perkName : ClassMapping.getPerksForClass(playerClassName)) {
            Perk perk = PerkHandler.getPerk(perkName);
            if (perk != null) {
                XPToAdd = (int) perk.modifyXP(skill, XPToAdd); // Apply the perk's XP modification logic
            }
        }

        // Add XP and determine the new potential level
        int newXP = currentXP + XPToAdd;
        int newLevel = getLevelForExperience(newXP);

        // Cap levels at MAX_LEVEL but continue tracking total XP
        newLevel = Math.min(newLevel, MAX_LEVEL);
        boolean leveledUp = newLevel > currentLevel;

        // Save the updated XP and level to the database (level doesn't exceed MAX_LEVEL)
        db.savePlayerSkill(playerUuid, skill.name(), newXP, newLevel);

        // Send notifications to the player
        String XPMessage = "§6[simpleskills]§f You gained " + XPToAdd + " XP in " + skill.getDisplayName() + "!";
        player.sendMessage(Text.of(XPMessage), true);

        if (leveledUp) {
            String levelUpMessage;

            var world = player.getWorld(); // Get the player's current world
            if (world instanceof ServerWorld serverWorld) {
                if (newLevel == MAX_LEVEL) {
                    // Special effects for reaching the maximum level
                    levelUpMessage = "§6[simpleskills]§f You have reached the maximum level of §6" + MAX_LEVEL + "§f in " + skill.getDisplayName() + "!";
                    player.sendMessage(Text.of(levelUpMessage), false);

                    // Notify the entire server

                    // Notify the entire server
                    serverWorld.getPlayers().forEach(onlinePlayer ->
                            onlinePlayer.sendMessage(Text.of("§6[SimpleSkills]§f " + player.getName().getString() + " has reached level §6" + MAX_LEVEL + "§f in " + skill.getDisplayName() + "!"), false)
                    );

                    // Play a unique sound for max level
                    serverWorld.playSound(
                            null, // Null means all players near the sound will hear it
                            player.getBlockPos(), // Position
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, // Special sound for max level
                            SoundCategory.PLAYERS, // Sound category
                            0.9f, // Volume
                            1.2f // Pitch
                    );

                    // Spawn max-level confetti particles
                    spawnConfettiParticles(serverWorld, player.getX(), player.getY(), player.getZ(), true);

                } else {
                    // Regular level-up effects
                    levelUpMessage = "§6[simpleskills]§f You leveled up in " + skill.getDisplayName() + "! New level: " + newLevel;
                    player.sendMessage(Text.of(levelUpMessage), false);

                    // Regular level-up sound
                    serverWorld.playSound(
                            null, // Null means all players near the sound will hear it
                            player.getBlockPos(), // Position
                            SoundEvents.ENTITY_PLAYER_LEVELUP, // Regular level-up sound
                            SoundCategory.PLAYERS, // Sound category
                            0.7f, // Volume
                            1.3f // Pitch
                    );

                    // Spawn regular level-up confetti particles
                    spawnConfettiParticles(serverWorld, player.getX(), player.getY(), player.getZ(), false);
                }
            }

            // Log level-up information
            Simpleskills.LOGGER.info("Player {} leveled up in {}! New level: {}", playerUuid, skill.name(), newLevel);
        }

        // Trigger XPChangeListener if it exists
        if (onXPChangeListener != null) {
            onXPChangeListener.accept(player, skill);
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

    private static void spawnConfettiParticles(ServerWorld serverWorld, double x, double y, double z, boolean isMaxLevel) {
        // Particle types for variety
        ParticleEffect[] particles = new ParticleEffect[]{
                ParticleTypes.HAPPY_VILLAGER,
                ParticleTypes.ENCHANTED_HIT,
                ParticleTypes.FIREWORK,
                ParticleTypes.CRIT
        };

        // Number of particles and spread adjustment
        int count = isMaxLevel ? 200 : 75;
        double spread = isMaxLevel ? 2.0 : 1.0;

        for (ParticleEffect particle : particles) {
            serverWorld.spawnParticles(
                    particle, // Current particle type
                    x, y + 1.5, z, // Center position
                    count / particles.length, // Divide evenly among types
                    spread, spread, spread, // Spread in X, Y, Z
                    0.05 // Velocity multiplier
            );
        }

        // Extra burst of fireworks for max level
        if (isMaxLevel) {
            serverWorld.spawnParticles(
                    ParticleTypes.FIREWORK,
                    x, y + 1.5, z,
                    50, // Extra burst count
                    1.5, 1.5, 1.5,
                    0.1
            );
        }
    }
}