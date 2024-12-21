package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleskillsCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("simpleskills")
                        .requires(source -> source.hasPermissionLevel(2))

                        .then(CommandManager.literal("add")
                                .then(CommandManager.literal("xp")
                                        .then(CommandManager.argument("targets", StringArgumentType.string())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))
                                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                                        .suggests((context, builder) -> CommandSource.suggestMatching(getValidSkills(), builder))
                                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(SimpleskillsCommands::addXp))))))

                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("targets", StringArgumentType.string())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))
                                        .then(CommandManager.argument("skill", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(getValidSkills(), builder))
                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(SimpleskillsCommands::setLevel)))))

                        // Add sub-command for querying total level
                        .then(CommandManager.literal("query")
                                .then(CommandManager.argument("targets", StringArgumentType.string())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))

                                        .then(CommandManager.literal("total")
                                                .executes(SimpleskillsCommands::queryTotalLevel))))));
    }

    private static int addXp(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "targets");
        String skillName = StringArgumentType.getString(context, "skill");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        return modifyXpOrLevel(context, playerName, skillName, amount);
    }

    private static int setLevel(CommandContext<ServerCommandSource> context) {
        // Extract arguments
        String playerName = StringArgumentType.getString(context, "targets");
        String skillName = StringArgumentType.getString(context, "skill");
        int newLevel = IntegerArgumentType.getInteger(context, "amount");

        ServerPlayerEntity targetPlayer = context.getSource().getServer().getPlayerManager().getPlayer(playerName);

        if (targetPlayer == null) {
            context.getSource().sendError(Text.of("[SimpleSkills] Player '" + playerName + "' not found."));
            return 0;
        }

        Skills skill;
        try {
            skill = Skills.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.of("[SimpleSkills] Invalid skill '" + skillName + "'."));
            return 0;
        }

        // Calculate corresponding XP for the new level using XPManager
        int newXp = XPManager.getExperienceForLevel(newLevel);

        // Save updated values into the database
        DatabaseManager.getInstance().savePlayerSkill(targetPlayer.getUuidAsString(), skill.name(), newXp, newLevel);

        // Notify the player and command source
        targetPlayer.sendMessage(Text.of("[SimpleSkills] Your skill '" + skill.getDisplayName() + "' was set to level " + newLevel + "!"), false);
        context.getSource().sendFeedback(() -> Text.of("[SimpleSkills] Set " + skill.getDisplayName() + " to level " + newLevel + " for player " + playerName + "."), true);

        // Update the player's attributes for the modified skill
        AttributeUpdater.updatePlayerAttributes(targetPlayer, skill);

        // Update the player's tab menu to reflect the change
        SkillTabMenu.updateTabMenu(targetPlayer);

        return 1;
        }

    private static int querySkill(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerName = StringArgumentType.getString(context, "targets");
        String skillName = StringArgumentType.getString(context, "skill");

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);

        if (targetPlayer == null) {
            source.sendError(Text.of("[SimpleSkills] Player '" + playerName + "' not found."));
            return 0;
        }

        Skills skill;
        try {
            skill = Skills.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendError(Text.of("[SimpleSkills] Invalid skill '" + skillName + "'."));
            return 0;
        }

        int level = XPManager.getSkillLevel(targetPlayer.getUuidAsString(), skill);

        source.sendFeedback(() -> Text.of("[SimpleSkills] " + playerName + "'s '" + skill.getDisplayName() + "' level: " + level), false);
        return 1;
    }

    private static int modifyXpOrLevel(CommandContext<ServerCommandSource> context, String playerName, String skillName, int amount) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);

        if (targetPlayer == null) {
            source.sendError(Text.of("[SimpleSkills] Player '" + playerName + "' not found."));
            return 0;
        }

        Skills skill;
        try {
            skill = Skills.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendError(Text.of("[SimpleSkills] Invalid skill '" + skillName + "'."));
            return 0;
        }

        String playerUuid = targetPlayer.getUuidAsString();

        XPManager.addXpWithNotification(targetPlayer, skill, amount);
        source.sendFeedback(() -> Text.of("[SimpleSkills] Added " + amount + " XP to " + playerName + "'s skill '" + skill.getDisplayName() + "'."), true);

        targetPlayer.sendMessage(Text.of("[SimpleSkills] Your skill '" + skill.getDisplayName() + "' was updated!"), false);
        return 1;
    }

    private static int queryTotalLevel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerName = StringArgumentType.getString(context, "targets");

        // Find the specified player
        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);

        if (targetPlayer == null) {
            source.sendError(Text.of("[SimpleSkills] Player '" + playerName + "' not found."));
            return 0;
        }

        String playerUuid = targetPlayer.getUuidAsString();

        // Loop through all skills and calculate total level
        int totalLevel = Stream.of(Skills.values())
                .mapToInt(skill -> XPManager.getSkillLevel(playerUuid, skill))
                .sum();

        // Send feedback to the command source
        source.sendFeedback(
                () -> Text.of("[SimpleSkills] " + playerName + "'s total skill level: " + totalLevel),
                false
        );

        return 1;
    }

    private static List<String> getOnlinePlayerNames(CommandContext<ServerCommandSource> context) {
        return context.getSource().getServer().getPlayerManager().getPlayerList().stream()
                .map(player -> player.getGameProfile().getName())
                .collect(Collectors.toList());
    }

    private static List<String> getValidSkills() {
        return Stream.of(Skills.values())
                .map(Skills::name)
                .map(String::toLowerCase)
                .toList();
    }

}
