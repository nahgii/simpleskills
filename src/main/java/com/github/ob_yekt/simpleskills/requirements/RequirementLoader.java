package com.github.ob_yekt.simpleskills.requirements;

import com.github.ob_yekt.simpleskills.Simpleskills;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequirementLoader {
    private static final Path BASE_PATH = Paths.get(System.getProperty("user.dir"), "mods", "simpleskills");
    private static Map<String, SkillRequirement> toolRequirements = new ConcurrentHashMap<>();
    private static Map<String, SkillRequirement> armorRequirements = new ConcurrentHashMap<>();
    private static Map<String, SkillRequirement> weaponRequirements = new ConcurrentHashMap<>();
    private static Map<String, SkillRequirement> magicRequirements = new ConcurrentHashMap<>();
    private static Map<Integer, Integer> xpRequirements = new ConcurrentHashMap<>();

    // Load all JSON configuration files
    public static void loadRequirements() {

        // Ensure that the "config/simpleskills" folder exists
        if (!Files.exists(BASE_PATH)) {
            try {
                Files.createDirectories(BASE_PATH);
            } catch (IOException e) {
                Simpleskills.LOGGER.error("[SimpleSkills] Failed to create directory: {}", BASE_PATH, e);
            }
        }

        toolRequirements = loadOrGenerateDefaults("tool_requirements.json", getDefaultToolRequirements());
        armorRequirements = loadOrGenerateDefaults("armor_requirements.json", getDefaultArmorRequirements());
        weaponRequirements = loadOrGenerateDefaults("weapon_requirements.json", getDefaultWeaponRequirements());
        magicRequirements = loadOrGenerateDefaults("magic_requirements.json", getDefaultMagicRequirements());
    }

    // Load a JSON file or create it with default values if missing
    private static Map<String, SkillRequirement> loadOrGenerateDefaults(String fileName, String defaultContent) {
        try {
            Path filePath = BASE_PATH.resolve(fileName); // Resolve file in the current working directory

            // Check if file exists; if not, create it
            if (!Files.exists(filePath)) {
                createDefaultFile(filePath, defaultContent);
            }

            // Read and parse the file into a map
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, SkillRequirement>>() {
            }.getType();
            try (Reader reader = new FileReader(filePath.toFile())) {
                return gson.fromJson(reader, type);
            }

        } catch (Exception e) {
            Simpleskills.LOGGER.error("[SimpleSkills] Error processing file '{}': {}", fileName, e.getMessage(), e);
        }
        return Collections.emptyMap(); // Return an empty map if an error occurs
    }

    // Load XP restrictions from magic_xp_restrictions.json
    private static Map<Integer, Integer> loadOrGenerateXPRestrictions(String fileName, String defaultContent) {
        try {
            Path filePath = BASE_PATH.resolve(fileName);

            // If the file doesn't exist, create it with default content
            if (!Files.exists(filePath)) {
                createDefaultFile(filePath, defaultContent);
            }

            // Read and parse the JSON file into a map
            Gson gson = new Gson();
            Type type = new TypeToken<Map<Integer, Integer>>() {}.getType();
            try (Reader reader = new FileReader(filePath.toFile())) {
                return gson.fromJson(reader, type);
            }

        } catch (Exception e) {
            Simpleskills.LOGGER.error("[SimpleSkills] Error processing file '{}': {}", fileName, e.getMessage(), e);
        }
        return Collections.emptyMap(); // Use an empty map as fallback
    }

    // Create a JSON file with default content
    private static void createDefaultFile(Path filePath, String defaultContent) throws IOException {
        Files.createFile(filePath); // Create the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(defaultContent); // Write the default content
        }
        Simpleskills.LOGGER.info("[SimpleSkills] Created default file: {}", filePath.toAbsolutePath());
    }

    // Provide default JSON content for tools
    private static String getDefaultToolRequirements() {
        return """
                {
                "minecraft:wooden_pickaxe": {"skill": "Mining", "level": 0},
                "minecraft:stone_pickaxe": {"skill": "Mining", "level": 10},
                "minecraft:iron_pickaxe": {"skill": "Mining", "level": 20},
                "minecraft:diamond_pickaxe": {"skill": "Mining", "level": 45},
                "minecraft:netherite_pickaxe": {"skill": "Mining", "level": 65},
                
                "minecraft:wooden_axe": {"skill": "Woodcutting", "level": 0},
                "minecraft:stone_axe": {"skill": "Woodcutting", "level": 10},
                "minecraft:iron_axe": {"skill": "Woodcutting", "level": 20},
                "minecraft:diamond_axe": {"skill": "Woodcutting", "level": 45},
                "minecraft:netherite_axe": {"skill": "Woodcutting", "level": 65},
                
                "minecraft:wooden_shovel": {"skill": "Excavating", "level": 0},
                "minecraft:stone_shovel": {"skill": "Excavating", "level": 10},
                "minecraft:iron_shovel": {"skill": "Excavating", "level": 20},
                "minecraft:diamond_shovel": {"skill": "Excavating", "level": 45},
                "minecraft:netherite_shovel": {"skill": "Excavating", "level": 65}
                }
                """;
    }

    // Provide default JSON content for armor
    private static String getDefaultArmorRequirements() {
        return """
                {
                "minecraft:leather_helmet": { "skill": "Defense", "level": 0 },
                "minecraft:leather_chestplate": { "skill": "Defense", "level": 0 },
                "minecraft:leather_leggings": { "skill": "Defense", "level": 0 },
                "minecraft:leather_boots": { "skill": "Defense", "level": 0 },
                
                "minecraft:golden_helmet": { "skill": "Defense", "level": 10 },
                "minecraft:golden_chestplate": { "skill": "Defense", "level": 10 },
                "minecraft:golden_leggings": { "skill": "Defense", "level": 10 },
                "minecraft:golden_boots": { "skill": "Defense", "level": 10 },
                
                "minecraft:chainmail_helmet": { "skill": "Defense", "level": 13 },
                "minecraft:chainmail_chestplate": { "skill": "Defense", "level": 13 },
                "minecraft:chainmail_leggings": { "skill": "Defense", "level": 13 },
                "minecraft:chainmail_boots": { "skill": "Defense", "level": 13 },
                
                "minecraft:turtle_helmet": { "skill": "Defense", "level": 15 },
                
                "minecraft:iron_helmet": { "skill": "Defense", "level": 25 },
                "minecraft:iron_chestplate": { "skill": "Defense", "level": 25 },
                "minecraft:iron_leggings": { "skill": "Defense", "level": 25 },
                "minecraft:iron_boots": { "skill": "Defense", "level": 25 },
                
                "minecraft:diamond_helmet": { "skill": "Defense", "level": 45 },
                "minecraft:diamond_chestplate": { "skill": "Defense", "level": 45 },
                "minecraft:diamond_leggings": { "skill": "Defense", "level": 45 },
                "minecraft:diamond_boots": { "skill": "Defense", "level": 45 },
                
                "minecraft:netherite_helmet": { "skill": "Defense", "level": 65 },
                "minecraft:netherite_chestplate": { "skill": "Defense", "level": 65 },
                "minecraft:netherite_leggings": { "skill": "Defense", "level": 65 },
                "minecraft:netherite_boots": { "skill": "Defense", "level": 65 }
                }
                """;
    }

    // Provide default JSON content for weapons
    private static String getDefaultWeaponRequirements() {
        return """
                {
                "minecraft:wooden_axe": { "skill": "Slaying", "level": 0 },
                "minecraft:stone_axe": { "skill": "Slaying", "level": 10 },
                "minecraft:golden_axe": { "skill": "Slaying", "level": 12 },
                "minecraft:iron_axe": { "skill": "Slaying", "level": 20 },
                "minecraft:diamond_axe": { "skill": "Slaying", "level": 45 },
                "minecraft:netherite_axe": { "skill": "Slaying", "level": 65 },
                

                "minecraft:bow": { "skill": "Slaying", "level": 12 },
                "minecraft:mace": { "skill": "Slaying", "level": 35 },
                
                "minecraft:wooden_sword": { "skill": "Slaying", "level": 0 },
                "minecraft:stone_sword": { "skill": "Slaying", "level": 10 },
                "minecraft:golden_sword": { "skill": "Slaying", "level": 12 },
                "minecraft:iron_sword": { "skill": "Slaying", "level": 20 },
                "minecraft:diamond_sword": { "skill": "Slaying", "level": 45 },
                "minecraft:netherite_sword": { "skill": "Slaying", "level": 65 }
                }
                """;
    }

    // Provide default JSON content for magic
    private static String getDefaultMagicRequirements() {
        return """
                {
                  "block.minecraft.brewing_stand": {
                    "skill": "Magic",
                    "level": 10
                  },
                  "block.minecraft.enchanting_table": {
                    "skill": "Magic",
                    "level": 35
                  },
                  "block.minecraft.anvil": {
                    "skill": "Magic",
                    "level": 65
                  },
                  "block.minecraft.chipped_anvil": {
                    "skill": "Magic",
                    "level": 65
                  },
                  "block.minecraft.damaged_anvil": {
                    "skill": "Magic",
                    "level": 65
                  }
                }
                """;
    }

    // Getters for specific requirement types
    public static SkillRequirement getToolRequirement(String id) {
        return toolRequirements.get(id);
    }

    public static SkillRequirement getArmorRequirement(String id) {
        return armorRequirements.get(id);
    }

    public static SkillRequirement getWeaponRequirement(String id) {
        return weaponRequirements.get(id);
    }

    public static SkillRequirement getMagicRequirement(String id) {
        return magicRequirements.get(id);
    }
}