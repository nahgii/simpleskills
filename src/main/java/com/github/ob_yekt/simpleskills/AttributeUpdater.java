package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import com.github.ob_yekt.simpleskills.simpleclasses.ClassMapping;
import com.github.ob_yekt.simpleskills.simpleclasses.Perk;
import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import com.github.ob_yekt.simpleskills.simpleclasses.PlayerClass;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

import static com.github.ob_yekt.simpleskills.SimpleskillsCommands.removePlayerFromIronmanTeam;

public class AttributeUpdater {

    public static void registerPlayerEvents() {
        /// ATTRIBUTE LOGIC ON PLAYER JOIN/REJOIN
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Clear any old attributes (safety measure)
            clearSkillAttributes(player);
            clearPerkAttributes(player);


            // Check if classes are enabled in config.json
            if (!ConfigLoader.isFeatureEnabled("classes")) {
                // Fetch the player's current class
                String currentClass = DatabaseManager.getInstance().getPlayerClass(player.getUuidAsString());

                // Check if the player's class is NOT "PEASANT"
                if (!PlayerClass.PEASANT.name().equalsIgnoreCase(currentClass)) {
                    // Notify the player and reset their class to "PEASANT"
                    player.sendMessage(
                            Text.literal("§6[simpleskills]§f Classes are currently disabled by the configuration. Your class has been reset to Peasant."),
                            false
                    );
                    DatabaseManager.getInstance().setPlayerClass(player.getUuidAsString(), PlayerClass.PEASANT.name());
                    SkillTabMenu.updateTabMenu(player);
                }
            }

            // Reapply attributes and perks when the player joins/rejoins
            RefreshSkillAttributes(player);
            applyPerkAttributes(player);

            // Update tab menu
            SkillTabMenu.updateTabMenu(player);

            // Logging for debugging purposes
            Simpleskills.LOGGER.info("[simpleskills] Attributes and perks applied on player join.");
        });

        /// CLEAN EXIT LOGIC
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Clear perks and attributes when a player disconnects
            clearSkillAttributes(player);
            clearPerkAttributes(player);

            // Logging for debugging purposes
            Simpleskills.LOGGER.info("[simpleskills] Attributes and perks cleared on player disconnect.");
        });

        /// PLAYER RESPAWN LOGIC
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) { // Player has died
                // Calculate total skill levels before dying
                int totalLevelsBeforeDeath = calculateTotalSkillLevels(newPlayer);

                // Check if the player is in Ironman Mode
                if (DatabaseManager.getInstance().isPlayerInIronmanMode(newPlayer.getUuidAsString())) {
                    // Send message to the player about losing their skills and class
                    newPlayer.sendMessage(Text.of("§6[simpleskills]§f Your deal with death has cost you all skill levels and your class. Ironman mode has been disabled."), false);

                    // Set player's class to "Peasant" (resetting class)
                    DatabaseManager.getInstance().setPlayerClass(newPlayer.getUuidAsString(), PlayerClass.PEASANT.name());

                    // Reset all skills in the database
                    DatabaseManager.getInstance().resetPlayerSkills(newPlayer.getUuidAsString());

                    // Clear all attributes to ensure no leftover modifiers
                    clearSkillAttributes(newPlayer);
                    clearPerkAttributes(newPlayer);

                    // Remove Ironman Mode in the database
                    DatabaseManager.getInstance().disableIronmanMode(newPlayer.getUuidAsString());

                    // Remove player from the Ironman team
                    removePlayerFromIronmanTeam(newPlayer);

                    // Send message to all players on the server about the Ironman death
                    String deathMessage = String.format("§6[simpleskills]§f %s has died in Ironman mode with a total level of §6%d§f.", newPlayer.getName().getString(), totalLevelsBeforeDeath);
                    Text message = Text.literal(deathMessage);  // Convert the message string to a Text object
                    Objects.requireNonNull(newPlayer.getServer()).getPlayerManager().broadcast(message, false);

                }
            }

            // Reapply skills and perks after respawn
            clearSkillAttributes(newPlayer);
            clearPerkAttributes(newPlayer);
            RefreshSkillAttributes(newPlayer);
            applyPerkAttributes(newPlayer);
            // Refresh Skill Tab Menu
            SkillTabMenu.updateTabMenu(newPlayer);
        });
    }


    private static int calculateTotalSkillLevels(ServerPlayerEntity player) {
        int totalLevels = 0;
        for (Skills skill : Skills.values()) {  // Assuming Skills is an enum or list of all possible skills
            totalLevels += XPManager.getSkillLevel(player.getUuidAsString(), skill);
        }
        return totalLevels;
    }

    // Apply perk-based attributes
    public static void applyPerkAttributes(ServerPlayerEntity player) {
        String playerClass = DatabaseManager.getInstance().getPlayerClass(player.getUuidAsString());

        if (playerClass != null) {
            // Apply all perks for the player's class
            List<String> perks = ClassMapping.getPerksForClass(playerClass);

            if (!perks.isEmpty()) {
                perks.forEach(perkName -> {
                    Perk perk = PerkHandler.getPerk(perkName);
                    if (perk != null) {
                        perk.onApply(player);
                    }
                });
            }
        }
    }

    // Clear perk-related attributes
    public static void clearPerkAttributes(ServerPlayerEntity player) {
        String playerClass = DatabaseManager.getInstance().getPlayerClass(player.getUuidAsString());

        if (playerClass != null) {
            // Clear all perks for the player's class
            ClassMapping.getPerksForClass(playerClass).forEach(perkName -> {
                Perk perk = PerkHandler.getPerk(perkName);
                if (perk != null) {
                    perk.onRemove(player);
                }
            });
        }
    }

    // Apply skill-based attributes
    public static void RefreshSkillAttributes(ServerPlayerEntity player) {
        for (Skills skill : Skills.values()) { // Assuming Skills is an enum
            updatePlayerAttributes(player, skill);
        }
    }

    // Clear skill-related attributes
    public static void clearSkillAttributes(ServerPlayerEntity player) {
        for (Skills skill : Skills.values()) {
            EntityAttributeInstance attributeInstance = switch (skill) {
                case SLAYING -> player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
                case WOODCUTTING -> player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
                case DEFENSE -> player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                case MINING -> player.getAttributeInstance(EntityAttributes.BLOCK_BREAK_SPEED);
                case EXCAVATING -> player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                case FARMING -> player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
                case MAGIC -> player.getAttributeInstance(EntityAttributes.FALL_DAMAGE_MULTIPLIER);
            };

            if (attributeInstance != null) {
                attributeInstance.clearModifiers(); // Clear all modifiers for the relevant attribute
            }
        }
    }

    public static void updatePlayerAttributes(ServerPlayerEntity player, Skills skill) {
        String playerUuid = player.getUuidAsString();
        switch (skill) {
            case SLAYING -> {
                EntityAttributeInstance attackDamage = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
                if (attackDamage != null) {
                    attackDamage.removeModifier(Identifier.of("simpleskills:slaying_bonus")); // Clear existing modifiers
                    int slayingLevel = XPManager.getSkillLevel(playerUuid, Skills.SLAYING);
                    if (slayingLevel >= 75) {
                        double bonusDamage = (slayingLevel - 74) * 0.01;
                        attackDamage.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:slaying_bonus"),
                                Math.min(bonusDamage, 0.25),
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ));
                    }
                }
            }
            case WOODCUTTING -> {
                EntityAttributeInstance blockRange = player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
                if (blockRange != null) {
                    blockRange.removeModifier(Identifier.of("simpleskills:woodcutting_bonus")); // Clear existing modifiers
                    int woodcuttingLevel = XPManager.getSkillLevel(playerUuid, Skills.WOODCUTTING);
                    if (woodcuttingLevel >= 75) {

                        double additionalBonus = 2.5;
                        double bonusRange = additionalBonus * (woodcuttingLevel - 74) / 24.0;

                        blockRange.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:woodcutting_bonus"),
                                Math.min(bonusRange, additionalBonus), // Cap the bonus at 2.5
                                EntityAttributeModifier.Operation.ADD_VALUE // Add directly to the base value of 4.5
                        ));
                    }
                }
            }
            case DEFENSE -> {
                EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.removeModifier(Identifier.of("simpleskills:defense_bonus")); // Clear existing modifiers
                    int defenseLevel = XPManager.getSkillLevel(playerUuid, Skills.DEFENSE);
                    if (defenseLevel >= 75) {
                        // Updated logic: Add fixed 2 HP (1 health icon) every 8 levels starting from level 75
                        int hearts = 0;
                        hearts++;
                        if (defenseLevel >= 83) hearts++;
                        if (defenseLevel >= 91) hearts++;
                        if (defenseLevel >= 99) hearts++;

                        // Each heart is 2 HP
                        double bonusHealth = hearts * 2.0; // 4 hearts = 8 HP
                        maxHealth.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:defense_bonus"),
                                bonusHealth,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ));
                    }
                }
            }
            case MINING -> {
                EntityAttributeInstance breakSpeed = player.getAttributeInstance(EntityAttributes.BLOCK_BREAK_SPEED);
                if (breakSpeed != null) {
                    breakSpeed.removeModifier(Identifier.of("simpleskills:mining_bonus")); // Clear existing modifiers
                    int miningLevel = XPManager.getSkillLevel(playerUuid, Skills.MINING);
                    if (miningLevel >= 75) {
                        // Calculate the per-level bonus evenly distributed
                        double incrementPerLevel = 0.011916667; // Even increment per level
                        double bonusSpeed = (miningLevel - 75) * incrementPerLevel;

                        breakSpeed.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:mining_bonus"),
                                bonusSpeed, // Scales evenly and hits 1.286 total at level 99
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ));
                    }
                }
            }

            case EXCAVATING -> {
                EntityAttributeInstance moveSpeed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                if (moveSpeed != null) {
                    moveSpeed.removeModifier(Identifier.of("simpleskills:excavating_bonus")); // Clear existing modifiers
                    int excavatingLevel = XPManager.getSkillLevel(playerUuid, Skills.EXCAVATING);
                    if (excavatingLevel >= 75) {
                        double bonusSpeed = (excavatingLevel - 74) * 0.01;
                        moveSpeed.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:excavating_bonus"),
                                Math.min(bonusSpeed, 0.25),
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ));
                    }
                }
            }
            case FARMING -> {
                EntityAttributeInstance knockResist = player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
                if (knockResist != null) {
                    knockResist.removeModifier(Identifier.of("simpleskills:farming_bonus")); // Clear existing modifiers
                    int farmingLevel = XPManager.getSkillLevel(playerUuid, Skills.FARMING);
                    if (farmingLevel >= 75) {
                        double bonusResist = (farmingLevel - 74) + 0.01;
                        knockResist.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:farming_bonus"),
                                Math.min(bonusResist, 0.25),
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ));
                    }
                }
            }
            case MAGIC -> {
                EntityAttributeInstance fallDamageMultiplier = player.getAttributeInstance(EntityAttributes.FALL_DAMAGE_MULTIPLIER);
                if (fallDamageMultiplier != null) {
                    fallDamageMultiplier.removeModifier(Identifier.of("simpleskills:magic_bonus")); // Clear existing modifiers
                    int magicLevel = XPManager.getSkillLevel(playerUuid, Skills.MAGIC);
                    if (magicLevel >= 75) {
                        double bonusResist = (magicLevel - 74) * 0.01;
                        fallDamageMultiplier.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:magic_bonus"),
                                Math.min(bonusResist, 0.25),
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ));
                    }
                }
            }
        }
    }
}