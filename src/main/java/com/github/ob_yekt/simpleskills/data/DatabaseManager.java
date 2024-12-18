package com.github.ob_yekt.simpleskills.data;

import com.github.ob_yekt.simpleskills.Simpleskills;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DATABASE_FOLDER = "simpleskills";
    private static final String DATABASE_NAME = "simpleskills.db";
    private static String DATABASE_FILE_PATH; // Full path to the database file

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabasePath();

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_FILE_PATH);
            initializeDatabase();
            Simpleskills.LOGGER.info("Connected to the SQLite database at: {}", DATABASE_FILE_PATH);
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to connect to the SQLite database.", e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize the database path and ensure the folder structure exists.
     */
    private void initializeDatabasePath() {
        try {
            // Get the directory containing the "mods" folder
            Path modsFolderPath = Paths.get(System.getProperty("user.dir"), "mods");

            // Path to the "simpleskills" folder inside the "mods" directory
            Path folderPath = modsFolderPath.resolve(DATABASE_FOLDER);

            // Ensure the "simpleskills" folder exists
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
                Simpleskills.LOGGER.info("Created folder for database: {}", folderPath.toAbsolutePath());
            }

            // Set the database file path inside the folder
            DATABASE_FILE_PATH = folderPath.resolve(DATABASE_NAME).toString();
        } catch (Exception e) {
            Simpleskills.LOGGER.error("Failed to create database folder.", e);
        }
    }

    private void initializeDatabase() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS player_skills (
                player_uuid TEXT NOT NULL,
                skill TEXT NOT NULL,
                xp INTEGER NOT NULL,
                level INTEGER NOT NULL,
                PRIMARY KEY (player_uuid, skill)
            );
        """;

        try (PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to create the database table.", e);
        }
    }

    private void checkOrReconnect() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(5)) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_FILE_PATH);
                initializeDatabase();
                Simpleskills.LOGGER.warn("Reconnected to the SQLite database.");
            }
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to reconnect to the SQLite database.", e);
        }
    }

    public void savePlayerSkill(String playerUuid, String skill, int xp, int level) {
        checkOrReconnect();

        String sql = """
            INSERT INTO player_skills (player_uuid, skill, xp, level)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid, skill)
            DO UPDATE SET xp = excluded.xp, level = excluded.level;
        """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setString(2, skill);
            statement.setInt(3, xp);
            statement.setInt(4, level);
            statement.executeUpdate();
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to save player skill data.", e);
        }
    }

    public ResultSet getPlayerSkills(String playerUuid) {
        checkOrReconnect();

        String sql = "SELECT skill, xp, level FROM player_skills WHERE player_uuid = ?";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, playerUuid);
            return statement.executeQuery();
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to load player skill data.", e);
            return null;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            Simpleskills.LOGGER.error("Failed to close SQLite connection.", e);
        }
    }
}