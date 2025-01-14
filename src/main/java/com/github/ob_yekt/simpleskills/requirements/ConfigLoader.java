package com.github.ob_yekt.simpleskills.requirements;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.Skills;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigLoader {

    private static final Gson GSON = new Gson();

    // Paths for configuration files
    private static final Path BASE_PATH = Paths.get(System.getProperty("user.dir"), "mods", "simpleskills");
    private static final Path BASE_XP_FILE = BASE_PATH.resolve("base_XP.json");
    private static final Path CONFIG_FILE = BASE_PATH.resolve("config.json");

    private static final Map<Skills, Integer> BASE_XP_MAP = new HashMap<>();
    private static JsonObject config = new JsonObject();

    public static void loadBaseXPConfig() {
        try {
            // Ensure the directory exists
            if (!Files.exists(BASE_PATH)) {
                Files.createDirectories(BASE_PATH);
            }

            // Check if the JSON file exists; if not, create a default one
            if (!Files.exists(BASE_XP_FILE)) {
                try (FileWriter writer = new FileWriter(BASE_XP_FILE.toFile())) {
                    writer.write(getDefaultBaseXPConfig());
                }
            }

            // Read the JSON content from the file
            JsonObject jsonObject;
            try (FileReader reader = new FileReader(BASE_XP_FILE.toFile())) {
                String jsonContent = new BufferedReader(reader).lines().collect(Collectors.joining(System.lineSeparator()));
                jsonObject = GSON.fromJson(jsonContent, JsonObject.class);
            } catch (JsonSyntaxException e) {
                Simpleskills.LOGGER.error("[simpleskills] JSON syntax error in base_XP.json: {}", e.getMessage());
                return;
            }

            // Map the JSON content to BASE_XP_MAP
            for (Skills skill : Skills.values()) {
                if (jsonObject != null && jsonObject.has(skill.name())) {
                    BASE_XP_MAP.put(skill, jsonObject.get(skill.name()).getAsInt());
                } else {
                    System.err.println("[simpleskills] Skill missing in base_XP.json: " + skill.name());
                }
            }

        } catch (Exception e) {
            Simpleskills.LOGGER.error("[simpleskills] Error loading base_XP.json: {}", e.getMessage());
        }
    }

    public static void loadFeatureConfig() {
        try {
            // Check if the config file exists
            if (Files.exists(CONFIG_FILE)) {
                // Read the existing content if the file exists
                String json = Files.readString(CONFIG_FILE);
                try {
                    config = GSON.fromJson(json, JsonObject.class);
                } catch (JsonSyntaxException e) {
                    Simpleskills.LOGGER.error("[simpleskills] JSON syntax error in config.json: {}", e.getMessage());
                }
            } else {
                // If the config file doesn't exist, create it with default settings
                Simpleskills.LOGGER.info("[simpleskills] Creating default config.json");

                // Add properties one at a time for clarity
                config.addProperty("classes", true);
                config.addProperty("villager_trades", true);

                saveFeatureConfig(); // Save the default config
            }
        } catch (IOException e) {
            Simpleskills.LOGGER.error("[simpleskills] Error loading config.json: {}", e.getMessage());
        }
    }

    private static void saveFeatureConfig() throws IOException {
        // Create the directories if they don't exist
        Files.createDirectories(CONFIG_FILE.getParent());

        // Use GsonBuilder to format the JSON in a readable way
        try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
            String prettyJson = GSON.toJson(config);
            writer.write(prettyJson); // Write pretty JSON to the file
        }
    }

    public static int getBaseXP(Skills skill) {
        // Return the Base XP for the skill, or default to 10 if the skill is not found
        return BASE_XP_MAP.getOrDefault(skill, 10);
    }

    public static boolean isFeatureEnabled(String feature) {
        return config.has(feature) && config.get(feature).getAsBoolean();
    }

    private static String getDefaultBaseXPConfig() {
        // The default Base XP values for each skill
        return """
                {
                  "MINING": 15,
                  "WOODCUTTING": 25,
                  "EXCAVATING": 10,
                  "FARMING": 30,
                  "SLAYING": 35,
                  "DEFENSE": 40,
                  "MAGIC": 80
                }
                """;
    }
}
