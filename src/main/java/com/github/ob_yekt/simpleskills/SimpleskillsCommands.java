package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;
import com.github.ob_yekt.simpleskills.simpleclasses.ClassCommandManager;
import com.github.ob_yekt.simpleskills.simpleclasses.PlayerClass;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.CommandSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.ob_yekt.simpleskills.AttributeUpdater.clearSkillAttributes;
import static com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler.getAvailablePerks;

public class SimpleskillsCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> // Delegate to setClassForPlayer
                // Delegate to setClassForPlayer
                dispatcher.register(
                        CommandManager.literal("simpleskills")
                                .requires(source -> source.hasPermissionLevel(0))

                                // TOGGLE SKILL TAB MENU (MULTIPLAYER
                                .then(CommandManager.literal("togglehud")
                                                .executes(SimpleskillsCommands::toggleHUD))

                                // IRONMAN MODE
                                .then(CommandManager.literal("ironman")
                                        .then(CommandManager.literal("enable")
                                                .executes(context -> enableIronman(context.getSource()))))

                                // RESET ALL SKILLS
                                .then(CommandManager.literal("reset")
                                        .then(CommandManager.argument("username", StringArgumentType.string()) // Restricted to operators
                                                .requires(source -> source.hasPermissionLevel(2)) // Operators can reset others
                                                .suggests((context, builder)
                                                        -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))
                                                .executes(SimpleskillsCommands::resetSkillsForPlayer)) // Operator resets another player's skills
                                        // Self-reset for all players (non-restricted)
                                        .executes(SimpleskillsCommands::resetSkillsForPlayer) // Reset self
                                )

                                // ADD XP COMMAND
                                .then(CommandManager.literal("addxp")
                                        .requires(source -> source.hasPermissionLevel(2)) // Restricted
                                        .then(CommandManager.argument("targets", StringArgumentType.string())
                                                .suggests((context, builder)
                                                        -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))
                                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                                        .suggests((context, builder)
                                                                -> CommandSource.suggestMatching(getValidSkills(), builder))
                                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(SimpleskillsCommands::addXP)))))

                                // SET LEVEL COMMAND
                                .then(CommandManager.literal("setlevel")
                                        .requires(source -> source.hasPermissionLevel(2)) // Restricted
                                        .then(CommandManager.argument("targets", StringArgumentType.string())
                                                .suggests((context, builder)
                                                        -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))
                                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                                        .suggests((context, builder)
                                                                -> CommandSource.suggestMatching(getValidSkills(), builder))
                                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(SimpleskillsCommands::setLevel)))))

                                // GET LEVEL COMMAND
                                .then(CommandManager.literal("getlevel")
                                        .then(CommandManager.argument("targets", StringArgumentType.string())
                                                .suggests((context, builder)
                                                        -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))
                                                .then(CommandManager.argument("skill", StringArgumentType.word())
                                                        .suggests((context, builder)
                                                                -> CommandSource.suggestMatching(getValidSkillsAndTotal(), builder))
                                                        .executes(context -> {
                                                            String skillName = StringArgumentType.getString(context, "skill");

                                                            if (skillName.equalsIgnoreCase("total")) {
                                                                // Query total level
                                                                return queryTotalLevel(context);
                                                            } else {
                                                                // Query a specific skill
                                                                return querySkill(context);
                                                            }
                                                        }))))

                                // CLASS-SPECIFIC COMMANDS
                                .then(CommandManager.literal("class")

                                        // REVOKE CLASS
                                        .then(CommandManager.literal("revoke")
                                                .then(CommandManager.argument("username", StringArgumentType.string()) // Restricted to operator
                                                        .requires(source -> source.hasPermissionLevel(2)) // Only accessible for operators
                                                        .suggests((context, builder)
                                                                -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))
                                                        .executes(ClassCommandManager::revokeClassForPlayer) // Operator revokes another player's class
                                                )
                                        )

                                        // SET CLASS
                                        .then(CommandManager.literal("set")
                                                .requires(source -> source.hasPermissionLevel(2)) // Only operators or cheats-enabled players
                                                .then(CommandManager.argument("username", StringArgumentType.string())
                                                        .suggests((context, builder)
                                                                -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))
                                                        .then(CommandManager.argument("classname", StringArgumentType.string())
                                                                .suggests((context, builder)
                                                                        -> CommandSource.suggestMatching(ClassCommandManager.getValidPlayerClasses(), builder)) // Suggest valid classes
                                                                .executes(ClassCommandManager::setClassForPlayer)
                                                        )
                                                )
                                        )

                                        // GET CLASS
                                        .then(CommandManager.literal("get")
                                                .then(CommandManager.argument("username", StringArgumentType.string())
                                                        .suggests((context, builder)
                                                                -> CommandSource.suggestMatching(getOnlinePlayerNames(context), builder))
                                                        .executes(ClassCommandManager::getClassForPlayer)
                                                )
                                                .executes(ClassCommandManager::getOwnClass) // Non-ops query their own class
                                        )

                                        // CLASS PERKS
                                        .then(CommandManager.literal("perks")
                                                .then(CommandManager.argument("classname", StringArgumentType.string())
                                                        .suggests((context, builder)
                                                                -> CommandSource.suggestMatching(ClassCommandManager.getValidPlayerClasses(), builder))
                                                        .executes(ClassCommandManager::listClassPerks)
                                                )
                                        )

                                        // LIST AVAILABLE CLASSES
                                        .then(CommandManager.literal("list")
                                                .requires(source -> source.hasPermissionLevel(0)) // Non-restricted
                                                .executes(ClassCommandManager::listAvailableClasses) // Executes the listAvailableClasses method
                                        )
                                )

                                // PERKINFO
                                .then(CommandManager.literal("perkinfo")
                                        .then(CommandManager.argument("perkname", StringArgumentType.greedyString())
                                                .suggests((context, builder)
                                                        -> CommandSource.suggestMatching(getAvailablePerks(), builder)) // Add suggestions
                                                .executes(ClassCommandManager::showPerkInfo)
                                        )
                                )
                ));
    }

    private static List<String> getValidSkillsAndTotal() {
        return Stream.concat(Stream.of("total"),      // Add "total" as a valid skill
                Stream.of(Skills.values()).map(Skills::name).map(String::toLowerCase)).toList();
    }


    /// TOGGLE HUD (Skill Tab Menu Visibility)
    private static int toggleHUD(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player != null) {
            // Toggle visibility of the SkillTabMenu for the player
            SkillTabMenu.toggleTabMenuVisibility(source);

            // Send feedback to the player
            player.sendMessage(Text.of("§6Skill Tab Menu visibility: " +
                    (SkillTabMenu.isTabMenuVisible ? "Enabled" : "Disabled")), false);
        } else {
            source.sendError(Text.of("§cYou must be a player to execute this command."));
        }
        return 1;
    }

    ///  IRONMAN MODE

    private static void createIronmanTeam(ServerScoreboard scoreboard) {
        Team ironmanTeam = scoreboard.getTeam("ironman");
        if (ironmanTeam == null) {
            ironmanTeam = scoreboard.addTeam("ironman");
            ironmanTeam.setPrefix(Text.literal("§c☠§f ")); // Changed Text.of to Text.literal
            ironmanTeam.setFriendlyFireAllowed(false);
            ironmanTeam.setShowFriendlyInvisibles(true);
        }
    }

    public static void assignPlayerToIronmanTeam(ServerPlayerEntity player) {
        ServerScoreboard scoreboard = Objects.requireNonNull(player.getServer()).getScoreboard();
        createIronmanTeam(scoreboard);
        Team ironmanTeam = scoreboard.getTeam("ironman");
        if (ironmanTeam != null) {
            // Use the player's name directly instead of trying to get online player names
            scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), ironmanTeam);
        }
    }

    public static void removePlayerFromIronmanTeam(ServerPlayerEntity player) {
        ServerScoreboard scoreboard = Objects.requireNonNull(player.getServer()).getScoreboard();
        Team ironmanTeam = scoreboard.getTeam("ironman");

        if (ironmanTeam != null) {
            // Remove the player from the Ironman team
            scoreboard.removeScoreHolderFromTeam(player.getNameForScoreboard(), ironmanTeam);
        }
    }

    private static int enableIronman(ServerCommandSource source) {
        // Ensure the command only works for players
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.of("§6[simpleskills]§f This command can only be used by players."));
            return 0;
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        String playerUUID = player.getUuidAsString();

        // Check if the player is already in Ironman Mode
        if (dbManager.isPlayerInIronmanMode(playerUUID)) {
            player.sendMessage(Text.of("§6[simpleskills]§f You have already enabled Ironman Mode."), false);
            return 0;
        }

        // Retrieve player skills
        ResultSet skills = dbManager.getPlayerSkills(playerUUID);
        if (skills == null) {
            source.sendError(Text.of("§6[simpleskills]§f Unable to fetch player skills."));
            return 0;
        }

        try {
            // Get the player's class and locate the corresponding PlayerClass enum
            String playerClassName = dbManager.getPlayerClass(playerUUID);
            PlayerClass playerClass = PlayerClass.valueOf(playerClassName.toUpperCase());

            boolean validForIronman = true;

            // Validate all skills
            while (skills.next()) {
                String skillName = skills.getString("skill");
                int level = skills.getInt("level");

                // Skip the primary skill (starting bonus)
                if (skillName.equalsIgnoreCase(playerClass.getPrimarySkill())) {
                    continue;
                }

                // If any other skill is above level 0, Ironman Mode cannot be enabled
                if (level > 0) {
                    validForIronman = false;
                    break;
                }
            }

            if (!validForIronman) {
                player.sendMessage(Text.of("§6[simpleskills]§f You must reset your skill levels with '/simpleskills reset' to begin Ironman Mode."), false);
                return 0;
            }

            // Mark player as in Ironman Mode in the database
            dbManager.enableIronmanMode(playerUUID);

            // Inform the player
            player.sendMessage(Text.of("§6[simpleskills]§f Ironman Mode enabled! All skills will reset upon death."), false);

            // Set player to the Ironman team
            assignPlayerToIronmanTeam(player);

            // Send message to all players on the server about the Ironman death
            String deathMessage = String.format("§6[simpleskills]§f Player " + player.getName().getString() + " has enabled Ironman Mode.");
            Text message = Text.literal(deathMessage);  // Convert the message string to a Text object
            Objects.requireNonNull(player.getServer()).getPlayerManager().broadcast(message, false);
            // Update SkillTabMenu
            SkillTabMenu.updateTabMenu(player);

            // Get the position to spawn particles and play sound
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            // Play a sound
            player.getWorld().playSound(
                    null,                                  // Target all nearby players to hear it
                    player.getBlockPos(),                  // Sound plays at the player's position
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,       // Thunder
                    SoundCategory.PLAYERS,                 // Player-related sound category
                    1.0F,                                  // Volume
                    1.0F                                   // Pitch
            );

            // Spawn particles in the server world (only if the world is an instance of ServerWorld)
            if (player.getServerWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.RAID_OMEN,
                        x, y, z,
                        50,
                        0.4,
                        1.5,
                        0.4,
                        0.05
                );

                serverWorld.spawnParticles(
                        ParticleTypes.TRIAL_SPAWNER_DETECTION_OMINOUS,
                        x, y, z,
                        40,
                        0.8,
                        1.5,
                        0.8,
                        0.05
                );

                serverWorld.spawnParticles(
                        ParticleTypes.WITCH,
                        x, y, z,
                        40,
                        0.5,
                        1.5,
                        0.5,
                        0.05
                );
            }

            return 1;

        } catch (IllegalArgumentException e) {
            source.sendError(Text.of("§6[simpleskills]§f Invalid player class detected."));
            return 0;
        } catch (SQLException e) {
            source.sendError(Text.of("§6[simpleskills]§f An error occurred while enabling Ironman Mode."));
            return 0;
        }
    }

    public static int resetSkillsForPlayer(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // Check if the command executor is only resetting their own skills
        boolean isSelfReset = false;

        ServerPlayerEntity targetPlayer;
        String playerName; // Use 'playerName' for consistency with other commands

        // Determine target player
        if (context.getInput().split(" ").length > 2) { // If target player is specified
            // Validate permission level for resetting another player's skills
            if (!source.hasPermissionLevel(2)) {
                source.sendError(Text.of("§6[simpleskills]§f You do not have permission to reset another player's skills."));
                return 0;
            }

            playerName = StringArgumentType.getString(context, "username"); // Target player's name
            targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);
            if (targetPlayer == null) {
                source.sendError(Text.of("§6[simpleskills]§f Player '" + playerName + "' not found."));
                return 0;
            }

        } else {
            playerName = null;

            // No target specified — resetting own skills
            targetPlayer = source.getPlayer();
            if (targetPlayer == null) {
                source.sendError(Text.of("§6[simpleskills]§f This command can only be executed by a player."));
                return 0;
            }
            isSelfReset = true; // Mark this as a self-reset
        }

        String playerUuid = targetPlayer.getUuidAsString();
        DatabaseManager dbManager = DatabaseManager.getInstance();

        // Set player's class to "Peasant" (resetting class)
        dbManager.setPlayerClass(playerUuid, PlayerClass.PEASANT.name());
        source.sendFeedback(() -> Text.of("§6[simpleskills]§f Player class set to 'Peasant' before resetting skills."), true);

        // Reset all skills in the database
        dbManager.resetPlayerSkills(playerUuid);

        // Clear all attributes to ensure no leftover modifiers
        clearSkillAttributes(targetPlayer);

        // Notify the target player
        targetPlayer.sendMessage(Text.of("§6[simpleskills]§f All your skill levels have been reset, and your class is now 'Peasant'."), false);

        // Update tab menu
        SkillTabMenu.updateTabMenu(targetPlayer);

        // Notify the command source when resetting others
        if (!isSelfReset) {
            source.sendFeedback(() -> Text.of("§6[simpleskills]§f Successfully reset all skills for player '" + playerName + "' and set their class to 'Peasant'."), true);
        }

        return 1;
    }

    private static int addXP(CommandContext<ServerCommandSource> context) {
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

        if (newLevel > 99) {
            context.getSource().sendError(Text.of("§6[simpleskills]§f Skill level cannot be above 99."));
            return 0;
        }

        if (targetPlayer == null) {
            context.getSource().sendError(Text.of("§6[simpleskills]§f Player '" + playerName + "' not found."));
            return 0;
        }

        Skills skill;
        try {
            skill = Skills.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.of("§6[simpleskills]§f Invalid skill '" + skillName + "'."));
            return 0;
        }

        // Calculate corresponding XP for the new level using XPManager
        int newXp = XPManager.getExperienceForLevel(newLevel);

        // Save updated values into the database
        DatabaseManager.getInstance().savePlayerSkill(targetPlayer.getUuidAsString(), skill.name(), newXp, newLevel);

        // Notify the player and command source
        targetPlayer.sendMessage(Text.of("§6[simpleskills]§f Your skill '" + skill.getDisplayName() + "' was set to level " + newLevel + "!"), false);
        context.getSource().sendFeedback(() -> Text.of("§6[simpleskills]§f Set " + skill.getDisplayName() + " to level " + newLevel + " for player " + playerName + "."), true);

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
            source.sendError(Text.of("§6[simpleskills]§f Player '" + playerName + "' not found."));
            return 0;
        }

        Skills skill;
        try {
            skill = Skills.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendError(Text.of("§6[simpleskills]§f Invalid skill '" + skillName + "'."));
            return 0;
        }

        int level = XPManager.getSkillLevel(targetPlayer.getUuidAsString(), skill);

        source.sendFeedback(() -> Text.of("§6[simpleskills]§f " + playerName + "'s '" + skill.getDisplayName() + "' level: " + level), false);
        return 1;
    }

    private static int modifyXpOrLevel(CommandContext<ServerCommandSource> context, String playerName, String skillName, int amount) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);

        if (targetPlayer == null) {
            source.sendError(Text.of("§6[simpleskills]§f Player '" + playerName + "' not found."));
            return 0;
        }

        Skills skill;
        try {
            skill = Skills.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendError(Text.of("§6[simpleskills]§f Invalid skill '" + skillName + "'."));
            return 0;
        }

        XPManager.addXPWithNotification(targetPlayer, skill, amount);
        source.sendFeedback(() -> Text.of("§6[simpleskills]§f Added " + amount + " XP to " + playerName + "'s skill '" + skill.getDisplayName() + "'."), true);

        targetPlayer.sendMessage(Text.of("§6[simpleskills]§f Your skill '" + skill.getDisplayName() + "' was updated!"), false);
        return 1;
    }

    private static int queryTotalLevel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerName = StringArgumentType.getString(context, "targets");

        // Find the specified player
        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);

        if (targetPlayer == null) {
            source.sendError(Text.of("§6[simpleskills]§f Player '" + playerName + "' not found."));
            return 0;
        }

        String playerUuid = targetPlayer.getUuidAsString();

        // Loop through all skills and calculate total level
        int totalLevel = Stream.of(Skills.values())
                .mapToInt(skill -> XPManager.getSkillLevel(playerUuid, skill))
                .sum();

        // Send feedback to the command source
        source.sendFeedback(
                () -> Text.of("§6[simpleskills]§f " + playerName + "'s total skill level: " + totalLevel),
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