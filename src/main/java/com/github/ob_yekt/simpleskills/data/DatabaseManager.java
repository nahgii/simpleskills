package com.github.ob_yekt.simpleskills.data;


import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.simpleclasses.PlayerClass;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DATABASE_NAME = "simpleskills.db";
    private static DatabaseManager instance;
    private Connection connection;
    private Path currentDatabasePath;

    private DatabaseManager() {
        // Initialize without connecting - we'll connect when a world is loaded
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize the database for a specific world
     *
     * @param server The MinecraftServer instance
     */
    public void initializeDatabase(MinecraftServer server) {
        // Get the world's data directory using the current server's directory structure
        Path worldDirectory = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("data");
        Path newDatabasePath = worldDirectory.resolve(DATABASE_NAME);

        // If we're already connected to this database, no need to reconnect
        if (currentDatabasePath != null && currentDatabasePath.equals(newDatabasePath) && isConnectionValid()) {
            return;
        }

        // Close existing connection if any
        closeConnection();

        try {
            // Create the data directory if it doesn't exist
            Files.createDirectories(worldDirectory);

            // Establish new connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + newDatabasePath);
            currentDatabasePath = newDatabasePath;

            // Initialize database tables
            createTables();

            Simpleskills.LOGGER.info("Connected to SQLite database at: {}", newDatabasePath);
        } catch (Exception e) {
            Simpleskills.LOGGER.error("Failed to initialize database at {}.", newDatabasePath, e);
        }
    }

    private boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private void createTables() {
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS player_skills (
                    player_uuid TEXT NOT NULL,
                    skill TEXT NOT NULL,
                    XP INTEGER NOT NULL,
                    level INTEGER NOT NULL,
                    player_class TEXT DEFAULT NULL,
                    is_ironman INTEGER DEFAULT 0,
                    PRIMARY KEY (player_uuid, skill)
                );
                """;

        try (PreparedStatement createTableStatement = connection.prepareStatement(createTableSQL)) {
            createTableStatement.executeUpdate();

            // Add 'is_ironman' column if it doesn't exist
            if (!isColumnExists()) {
                String addIronmanColumnSQL = """
                        ALTER TABLE player_skills
                        ADD COLUMN is_ironman INTEGER DEFAULT 0;
                        """;
                try (PreparedStatement addIronmanColumnStatement = connection.prepareStatement(addIronmanColumnSQL)) {
                    addIronmanColumnStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to create database tables.", e);
        }
    }

    private boolean isColumnExists() {
        try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, "player_skills", "is_ironman")) {
            return resultSet.next();
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to check column existence.", e);
            return false;
        }
    }

    private void checkConnection() {
        if (!isConnectionValid()) {
            Simpleskills.LOGGER.warn("Database connection lost, attempting to reconnect...");
            try {
                if (currentDatabasePath != null) {
                    connection = DriverManager.getConnection("jdbc:sqlite:" + currentDatabasePath);
                    createTables();
                    Simpleskills.LOGGER.info("Successfully reconnected to the database.");
                } else {
                    Simpleskills.LOGGER.error("Cannot reconnect - no database path set!");
                }
            } catch (SQLException e) {
                Simpleskills.LOGGER.error("Failed to reconnect to the database.", e);
            }
        }
    }

    // SKILLS OPERATIONS

    public void savePlayerSkill(String playerUuid, String skill, int XP, int level) {
        checkConnection();

        String sql = """
                INSERT INTO player_skills (player_uuid, skill, XP, level, player_class)
                VALUES (?, ?, ?, ?, NULL)
                ON CONFLICT(player_uuid, skill)
                DO UPDATE SET XP = excluded.XP, level = excluded.level;
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setString(2, skill);
            statement.setInt(3, XP);
            statement.setInt(4, level);
            statement.executeUpdate();
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to save player skill data for UUID: {}", playerUuid, e);
        }
    }

    public ResultSet getPlayerSkills(String playerUuid) {
        checkConnection();

        String sql = "SELECT skill, XP, level FROM player_skills WHERE player_uuid = ?";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, playerUuid);
            return statement.executeQuery();
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to load player skill data for UUID: {}", playerUuid, e);
            return null;
        }
    }

    public int getTotalSkillLevel(String playerUUID) {
        checkConnection();

        String sql = "SELECT SUM(level) as total_level FROM player_skills WHERE player_uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID);

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getInt("total_level"); // The sum of all levels
            }
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to get total skill level for UUID: {}", playerUUID, e);
        }

        return 0; // Default to 0 if something goes wrong
    }

    // CLASS OPERATIONS

    public void setPlayerClass(String playerUuid, String playerClass) {
        checkConnection();

        String sql = """
                INSERT INTO player_skills (player_uuid, skill, XP, level, player_class)
                VALUES (?, 'NONE', 0, 0, ?)
                ON CONFLICT(player_uuid, skill)
                DO UPDATE SET player_class = excluded.player_class;
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setString(2, playerClass);
            int rowsUpdated = statement.executeUpdate();

            Simpleskills.LOGGER.info("Set class for player {}: {} (Rows Updated: {})", playerUuid, playerClass, rowsUpdated);
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to set player class for {} to {}.", playerUuid, playerClass, e);
        }
    }

    public String getPlayerClass(String playerUuid) {
        checkConnection();

        String sql = "SELECT player_class FROM player_skills WHERE player_uuid = ? AND skill = 'NONE'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String playerClass = resultSet.getString("player_class");
                if (playerClass != null && !playerClass.isEmpty()) {
                    return playerClass;
                }

                // If class is null or empty, set default to PEASANT
                Simpleskills.LOGGER.info("Setting default class PEASANT for player {}", playerUuid);
                setPlayerClass(playerUuid, "PEASANT");
                return "PEASANT";
            }

            // If no row exists, create new entry with PEASANT class
            Simpleskills.LOGGER.info("Creating new entry with default class PEASANT for player {}", playerUuid);
            setPlayerClass(playerUuid, "PEASANT");
            return "PEASANT";
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to get player class for {}. Using default: PEASANT", playerUuid, e);
            return "PEASANT";
        }
    }

    public void resetPlayerSkills(String playerUUID) {
        checkConnection();

        // Fetch the player's class from the database
        String playerClassName = getPlayerClass(playerUUID);

        try {
            // Locate the PlayerClass enum for the player's class
            PlayerClass playerClass = PlayerClass.valueOf(playerClassName.toUpperCase());

            // Use the primary skill from the PlayerClass enum
            String primarySkill = playerClass.getPrimarySkill();

            // Reset skills: primary skill to level 10, others to level 0
            for (Skills skill : Skills.values()) {
                int startingLevel = skill.name().equalsIgnoreCase(primarySkill) ? 10 : 0;
                savePlayerSkill(playerUUID, skill.name(), 0, startingLevel);
            }

        } catch (IllegalArgumentException e) {
            Simpleskills.LOGGER.warn("Invalid player class {} for UUID {}. Cannot reset skills.", playerClassName, playerUUID, e);
        }
    }

    // Add method to enable Ironman Mode:
    public void enableIronmanMode(String playerUUID) {
        checkConnection();

        String sql = """
                UPDATE player_skills
                SET is_ironman = 1
                WHERE player_uuid = ? AND skill = 'NONE';
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID);
            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                // Add a new entry for the player if none exists
                String insertSql = """
                        INSERT INTO player_skills (player_uuid, skill, XP, level, player_class, is_ironman)
                        VALUES (?, 'NONE', 0, 0, NULL, 1);
                        """;
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    insertStatement.setString(1, playerUUID);
                    insertStatement.executeUpdate();
                }
            }

            Simpleskills.LOGGER.info("Ironman Mode enabled for player UUID: {}", playerUUID);
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to enable Ironman Mode for player UUID: {}", playerUUID, e);
        }
    }

    public void disableIronmanMode(String playerUUID) {
        checkConnection();

        String sql = """
                UPDATE player_skills
                SET is_ironman = 0
                WHERE player_uuid = ? AND skill = 'NONE';
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID);
            statement.executeUpdate();

            Simpleskills.LOGGER.info("Ironman Mode disabled for player UUID: {}", playerUUID);
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to disable Ironman Mode for UUID: {}", playerUUID, e);
        }
    }


    // Add method to check if a player is in Ironman Mode:
    public boolean isPlayerInIronmanMode(String playerUUID) {
        checkConnection();

        String sql = "SELECT is_ironman FROM player_skills WHERE player_uuid = ? AND skill = 'NONE'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID);

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getInt("is_ironman") == 1;
            }
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to check Ironman Mode for UUID: {}", playerUUID, e);
        }

        return false;
    }

    private void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to close database connection.", e);
        }
    }

    public void close() {
        closeConnection();
        currentDatabasePath = null;
    }
}