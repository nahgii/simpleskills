package com.github.ob_yekt.simpleskills.simpleclasses;

import com.github.ob_yekt.simpleskills.*;
import com.github.ob_yekt.simpleskills.data.DatabaseManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Stream;

import static com.github.ob_yekt.simpleskills.AttributeUpdater.applyPerkAttributes;
import static com.github.ob_yekt.simpleskills.AttributeUpdater.clearPerkAttributes;
import static com.github.ob_yekt.simpleskills.AttributeUpdater.RefreshSkillAttributes;

public class ClassCommandManager {

    public static List<String> getValidPlayerClasses() {
        return Stream.of(PlayerClass.values())
                .filter(playerClass -> !playerClass.name().equalsIgnoreCase("Peasant")) // Exclude Peasant
                .map(PlayerClass::getDisplayName)
                .map(String::toLowerCase)
                .toList();
    }

    public static int setClassForPlayer(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // Fetch username and target player
        String username = StringArgumentType.getString(context, "username");
        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(username);

        if (targetPlayer == null) {
            source.sendError(Text.of("§6[simpleskills]§f Player '" + username + "' not found."));
            return 0;
        }

        // Check if classes are enabled in config.json
        if (!ConfigLoader.isFeatureEnabled("classes")) {
            source.sendMessage(Text.literal("§6[simpleskills]§f Classes are currently disabled by the configuration."));
            return 0;
        }

        // Fetch the player's class details
        String className = StringArgumentType.getString(context, "classname");
        String playerUuid = targetPlayer.getUuidAsString();
        String currentClass = DatabaseManager.getInstance().getPlayerClass(playerUuid);

        // Prevent assigning a class if the player already has one and it isn't Peasant
        if (currentClass != null && !currentClass.equalsIgnoreCase("Peasant")) {
            source.sendError(Text.of("§6[simpleskills]§f Player '" + username + "' already has a class: " + currentClass + ". Revoke it first."));
            return 0;
        }

        // Validate if the className is a valid class
        PlayerClass playerClass = Stream.of(PlayerClass.values())
                .filter(pc -> pc.getDisplayName().equalsIgnoreCase(className))
                .findFirst()
                .orElse(null);

        if (playerClass == null) {
            source.sendError(Text.of("§6[simpleskills]§f Invalid class: " + className));
            return 0;
        }

        // Apply class change
        DatabaseManager.getInstance().setPlayerClass(playerUuid, playerClass.name());

        // Apply primary skill logic
        String primarySkillName = playerClass.getPrimarySkill();
        if (primarySkillName != null && !primarySkillName.isEmpty()) {
            Skills primarySkill;
            try {
                primarySkill = Skills.valueOf(primarySkillName.toUpperCase());

                int currentSkillLevel = XPManager.getSkillLevel(playerUuid, primarySkill);

                if (currentSkillLevel < 10) {
                    int XPForLevel10 = XPManager.getExperienceForLevel(10);
                    DatabaseManager.getInstance().savePlayerSkill(playerUuid, primarySkill.name(), XPForLevel10, 10);
                }
            } catch (IllegalArgumentException e) {
                Simpleskills.LOGGER.error("Failed to assign primary skill '{}' for class '{}'", primarySkillName, playerClass.name(), e);
            }
        }

        // Apply Perk Attributes
        applyPerkAttributes(targetPlayer);
        SkillTabMenu.updateTabMenu(targetPlayer);

        // Update Skill attributes
        RefreshSkillAttributes(targetPlayer);

        // Notify the operator and the target player of the change
        notifyClassChange(source, targetPlayer, playerClass);
        return 1;
    }

    public static int getClassForPlayer(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String username = StringArgumentType.getString(context, "username");

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(username);
        if (targetPlayer == null) {
            source.sendError(Text.of("§6[simpleskills]§f Player '" + username + "' not found."));
            return 0;
        }

        String playerUuid = targetPlayer.getUuidAsString();
        String currentClass = DatabaseManager.getInstance().getPlayerClass(playerUuid);

        source.sendFeedback(() -> Text.of("§6[simpleskills]§f Player '" + username + "' has class: " + (currentClass == null ? "NONE" : currentClass)), false);
        return 1;
    }

    public static int getOwnClass(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.of("§6[simpleskills]§f Command can only be executed by a player."));
            return 0;
        }

        String playerUuid = player.getUuidAsString();
        String currentClass = DatabaseManager.getInstance().getPlayerClass(playerUuid);

        player.sendMessage(Text.of("§6[simpleskills]§f Your current class: " + (currentClass == null ? "NONE" : currentClass)), false);
        return 1;
    }

    public static int revokeClassForPlayer(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String username = StringArgumentType.getString(context, "username");

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(username);

        // Check if the player exists
        if (targetPlayer == null) {
            source.sendError(Text.of("§6[simpleskills]§f Player '" + username + "' not found."));
            return 0;
        }

        String playerUuid = targetPlayer.getUuidAsString();
        String currentClass = DatabaseManager.getInstance().getPlayerClass(playerUuid);

        // Check if the player is already a Peasant
        if (currentClass == null || currentClass.trim().isEmpty() || currentClass.equalsIgnoreCase(PlayerClass.PEASANT.name())) {
            source.sendError(Text.of("§6[simpleskills]§f Player '" + username + "' is already a Peasant and cannot have their class revoked."));
            return 0;
        }

        // Reset the primary skill associated with the current class
        PlayerClass classBeingRevoked = PlayerClass.valueOf(currentClass);
        String primarySkillName = classBeingRevoked.getPrimarySkill();

        if (primarySkillName != null && !primarySkillName.isEmpty()) {
            try {
                // Use XPManager to calculate XP for level 0 and reset the skill
                Skills primarySkill = Skills.valueOf(primarySkillName.toUpperCase());
                int newXP = XPManager.getExperienceForLevel(0); // XP for level 0

                // Save the reset skill level and XP to the database
                DatabaseManager.getInstance().savePlayerSkill(playerUuid, primarySkill.name(), newXP, 0);

                // Notify the player
                targetPlayer.sendMessage(Text.literal("§6[simpleskills]§f Your primary skill '" + primarySkill.getDisplayName() + "' has been reset."), false);


                // Update the player's attributes for the reset skill
                AttributeUpdater.updatePlayerAttributes(targetPlayer, primarySkill);
            } catch (IllegalArgumentException e) {
                source.sendError(Text.of("§6[simpleskills]§f Failed to reset the skill associated with the player's class."));
                Simpleskills.LOGGER.error("Failed to reset primary skill '{}' for player '{}'", primarySkillName, username, e);
            }
        }

        // Clear Perk Attributes for the player's previous class
        clearPerkAttributes(targetPlayer);

        // Reset the player's class to Peasant
        DatabaseManager.getInstance().setPlayerClass(playerUuid, PlayerClass.PEASANT.name());

        // Refresh the player's skill attributes
        RefreshSkillAttributes(targetPlayer);

        // Notify the player and the command issuer
        targetPlayer.sendMessage(Text.literal("§6[simpleskills]§f Your class has been revoked. You are now a 'Peasant'."), false);

        // Update skill tab menu
        SkillTabMenu.updateTabMenu(targetPlayer);
        source.sendFeedback(() -> Text.literal("§6[simpleskills]§f Revoked class for player '" + username + "'."), true);

        return 1;
    }

    public static int listAvailableClasses(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // Send a header message
        source.sendFeedback(() -> Text.of("§6[simpleskills]§f Available Classes:"), false);

        // List each class with its details
        for (PlayerClass playerClass : PlayerClass.values()) {

            String classInfo = String.format("""
                            §6%s§r:
                            Primary Skill: §b%s§r
                            Starting Level: §a%d§r
                            XP Bonus Multiplier: %.2fx
                            """,
                    playerClass.getDisplayName(),
                    playerClass.getPrimarySkill(),
                    playerClass.getStartingLevel(),
                    playerClass.getXPBonusMultiplier());

            source.sendFeedback(() -> Text.of(classInfo), false);
        }
        return 1;
    }

    public static int listClassPerks(CommandContext<ServerCommandSource> context) {
        String className = StringArgumentType.getString(context, "classname").toLowerCase();
        ServerCommandSource source = context.getSource();

        // Check if it's a valid class
        PlayerClass playerClass = Stream.of(PlayerClass.values())
                .filter(c -> c.getDisplayName().equalsIgnoreCase(className))
                .findFirst()
                .orElse(null);

        if (playerClass == null) {
            source.sendError(Text.of("§6[simpleskills]§f Invalid class name: " + className));
            return 0;
        }

        // Retrieve perks for the class
        List<String> perks = ClassMapping.getPerksForClass(playerClass.name());
        if (perks == null || perks.isEmpty()) {
            source.sendFeedback(() -> Text.of("§6[simpleskills]§f Class '" + playerClass.getDisplayName() + "' has no perks."), false);
            return 1;
        }

        // Display the perks
        source.sendFeedback(() -> Text.of("§6[simpleskills]§f Perks for class '" + playerClass.getDisplayName() + "':"), false);
        for (String perk : perks) {
            source.sendFeedback(() -> Text.of("- " + perk), false);
        }

        return 1;
    }

    public static int showPerkInfo(CommandContext<ServerCommandSource> context) {
        String perkName = StringArgumentType.getString(context, "perkname").toLowerCase();

        Perk perk = PerkHandler.getPerk(perkName);
        if (perk == null) {
            context.getSource().sendError(Text.of("§6[simpleskills]§f No perk found with the name '" + perkName + "'."));
            return 0;
        }

        // Display perk description
        context.getSource().sendFeedback(() -> Text.of("§6[simpleskills]§f Perk: " + PerkHandler.getPerk(perkName)), false);
        context.getSource().sendFeedback(() -> Text.of("Effect: NOT IMPLEMENTED YET! PLACEHOLDER TEXT2"), false);

        return 1;
    }

    private static void notifyClassChange(ServerCommandSource source, ServerPlayerEntity player, PlayerClass playerClass) {
        // Create a detailed message about the new class

        String classInfo = String.format("""
                        §6[simpleskills]§f Class set to '%s':
                        Primary Skill: %s (Starting Level: %d)
                        XP Bonus Multiplier: %.2fx""",
                playerClass.getDisplayName(),
                playerClass.getPrimarySkill(),
                playerClass.getStartingLevel(),
                playerClass.getXPBonusMultiplier());


        // Send a message only to the player (to reduce duplicate notifications)
        player.sendMessage(Text.of(classInfo), false);

        // Notify whoever issued the command (if not the player)
        if (source.getEntity() != player) {
            source.sendFeedback(() -> Text.of("§6[simpleskills]§f Successfully set the class for " + player.getName().getString() + "."), true);
        }
    }
}