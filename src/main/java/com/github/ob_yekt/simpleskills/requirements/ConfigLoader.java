package com.github.ob_yekt.simpleskills.requirements;

import com.github.ob_yekt.simpleskills.Skills;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {

    // Define the base path for the JSON file
    private static final Path BASE_PATH = Paths.get(System.getProperty("user.dir"), "mods", "simpleskills");
    private static final Path CONFIG_FILE = BASE_PATH.resolve("base_xp.json");

    private static final Map<Skills, Integer> BASE_XP_MAP = new HashMap<>();

    public static void loadBaseXpConfig() {
        try {
            // Ensure the directory exists
            if (!Files.exists(BASE_PATH)) {
                Files.createDirectories(BASE_PATH);
            }

            // Check if the JSON file exists; if not, create a default one
            if (!Files.exists(CONFIG_FILE)) {
                try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
                    writer.write(getDefaultConfig());
                }
            }

            // Read the JSON content from the file
            JsonObject jsonObject = null;
            try (FileReader reader = new FileReader(CONFIG_FILE.toFile())) {
                jsonObject = new Gson().fromJson(reader, JsonObject.class);
            }

            // Map the JSON content to `BASE_XP_MAP`
            for (Skills skill : Skills.values()) {
                if (jsonObject != null && jsonObject.has(skill.name())) {
                    BASE_XP_MAP.put(skill, jsonObject.get(skill.name()).getAsInt());
                } else {
                    System.err.println("Skill missing in base_xp.json: " + skill.name());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading base_xp.json.");
        }
    }

    public static int getBaseXp(Skills skill) {
        // Return the Base XP for the skill, or default to 10 if the skill is not found
        return BASE_XP_MAP.getOrDefault(skill, 10);
    }

    private static String getDefaultConfig() {
        // The default Base XP values for each skill
        return "{\n" +
                "    \"WOODCUTTING\": 30,\n" +
                "    \"EXCAVATING\": 20,\n" +
                "    \"MINING\": 25,\n" +
                "    \"SLAYING\": 8,\n" +
                "    \"DEFENSE\": 10,\n" +
                "    \"MAGIC\": 8\n" +
                "}";
    }
}