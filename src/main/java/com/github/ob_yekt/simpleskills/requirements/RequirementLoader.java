package com.github.ob_yekt.simpleskills.requirements;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RequirementLoader {
    private static final Path BASE_PATH = Paths.get(System.getProperty("user.dir"), "mods", "simpleskills");
    private static Map<String, SkillRequirement> toolRequirements = new ConcurrentHashMap<>();
    private static Map<String, SkillRequirement> armorRequirements = new ConcurrentHashMap<>();
    private static Map<String, SkillRequirement> weaponRequirements = new ConcurrentHashMap<>();
    private static Map<String, SkillRequirement> magicRequirements = new ConcurrentHashMap<>();

    // Load all JSON configuration files
    public static void loadRequirements() {
        // Ensure that the "config/simpleskills" folder exists
        if (!Files.exists(BASE_PATH)) {
            try {
                Files.createDirectories(BASE_PATH);
            } catch (IOException e) {
                Simpleskills.LOGGER.error("[simpleskills] Failed to create directory: {}", BASE_PATH, e);
            }
        }

        toolRequirements = loadOrGenerateDefaults("tool_requirements.json", getDefaultToolRequirements());
        armorRequirements = loadOrGenerateDefaults("armor_requirements.json", getDefaultArmorRequirements());
        weaponRequirements = loadOrGenerateDefaults("weapon_requirements.json", getDefaultWeaponRequirements());
        magicRequirements = loadOrGenerateDefaults("magic_requirements.json", getDefaultMAGICRequirements());
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
            Type type = new TypeToken<Map<String, SkillRequirement>>() {}.getType();
            try (Reader reader = new FileReader(filePath.toFile())) {
                String jsonContent = new BufferedReader(reader).lines().collect(Collectors.joining(System.lineSeparator()));
                return gson.fromJson(jsonContent, type);
            } catch (JsonSyntaxException e) {
                Simpleskills.LOGGER.error("[simpleskills] JSON syntax error in '{}': {}", fileName, e.getMessage());
            }

        } catch (Exception e) {
            Simpleskills.LOGGER.error("[simpleskills] Error processing file '{}': {}", fileName, e.getMessage(), e);
        }
        return Collections.emptyMap(); // Return an empty map if an error occurs
    }

    // Create a JSON file with default content
    private static void createDefaultFile(Path filePath, String defaultContent) throws IOException {
        Files.createFile(filePath); // Create the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(defaultContent); // Write the default content
        }
        Simpleskills.LOGGER.info("[simpleskills] Created default file: {}", filePath.toAbsolutePath());
    }

    // Provide default JSON content for tools
    private static String getDefaultToolRequirements() {
        return """
{
  "minecraft:wooden_pickaxe": {"skill": "MINING", "level": 0},
  "minecraft:stone_pickaxe": {"skill": "MINING", "level": 10},
  "minecraft:golden_pickaxe": {"skill": "MINING", "level": 15},
  "minecraft:iron_pickaxe": {"skill": "MINING", "level": 30},
  "minecraft:diamond_pickaxe": {"skill": "MINING", "level": 60},
  "minecraft:netherite_pickaxe": {"skill": "MINING", "level": 75},

  "minecraft:wooden_axe": {"skill": "WOODCUTTING", "level": 0},
  "minecraft:stone_axe": {"skill": "WOODCUTTING", "level": 10},
  "minecraft:golden_axe": {"skill": "WOODCUTTING", "level": 15},
  "minecraft:iron_axe": {"skill": "WOODCUTTING", "level": 30},
  "minecraft:diamond_axe": {"skill": "WOODCUTTING", "level": 60},
  "minecraft:netherite_axe": {"skill": "WOODCUTTING", "level": 75},

  "minecraft:wooden_shovel": {"skill": "EXCAVATING", "level": 0},
  "minecraft:stone_shovel": {"skill": "EXCAVATING", "level": 10},
  "minecraft:golden_shovel": {"skill": "EXCAVATING", "level": 15},
  "minecraft:iron_shovel": {"skill": "EXCAVATING", "level": 30},
  "minecraft:diamond_shovel": {"skill": "EXCAVATING", "level": 60},
  "minecraft:netherite_shovel": {"skill": "EXCAVATING", "level": 75},

  "minecraft:wooden_hoe": {"skill": "FARMING", "level": 0},
  "minecraft:stone_hoe": {"skill": "FARMING", "level": 10},
  "minecraft:golden_hoe": {"skill": "FARMING", "level": 15},
  "minecraft:iron_hoe": {"skill": "FARMING", "level": 30},
  "minecraft:diamond_hoe": {"skill": "FARMING", "level": 60},
  "minecraft:netherite_hoe": {"skill": "FARMING", "level": 75}
}
                """;
    }

    // Provide default JSON content for armor
    private static String getDefaultArmorRequirements() {
        return """
                {
                  "minecraft:leather_helmet": { "skill": "DEFENSE", "level": 0 },
                  "minecraft:leather_chestplate": { "skill": "DEFENSE", "level": 0 },
                  "minecraft:leather_leggings": { "skill": "DEFENSE", "level": 0 },
                  "minecraft:leather_boots": { "skill": "DEFENSE", "level": 0 },

                  "minecraft:golden_helmet": { "skill": "DEFENSE", "level": 15 },
                  "minecraft:golden_chestplate": { "skill": "DEFENSE", "level": 15 },
                  "minecraft:golden_leggings": { "skill": "DEFENSE", "level": 15 },
                  "minecraft:golden_boots": { "skill": "DEFENSE", "level": 15 },

                  "minecraft:chainmail_helmet": { "skill": "DEFENSE", "level": 15 },
                  "minecraft:chainmail_chestplate": { "skill": "DEFENSE", "level": 15 },
                  "minecraft:chainmail_leggings": { "skill": "DEFENSE", "level": 15 },
                  "minecraft:chainmail_boots": { "skill": "DEFENSE", "level": 15 },

                  "minecraft:turtle_helmet": { "skill": "DEFENSE", "level": 35 },

                  "minecraft:iron_helmet": { "skill": "DEFENSE", "level": 35 },
                  "minecraft:iron_chestplate": { "skill": "DEFENSE", "level": 35 },
                  "minecraft:iron_leggings": { "skill": "DEFENSE", "level": 35 },
                  "minecraft:iron_boots": { "skill": "DEFENSE", "level": 35 },

                  "minecraft:diamond_helmet": { "skill": "DEFENSE", "level": 60 },
                  "minecraft:diamond_chestplate": { "skill": "DEFENSE", "level": 60 },
                  "minecraft:diamond_leggings": { "skill": "DEFENSE", "level": 60 },
                  "minecraft:diamond_boots": { "skill": "DEFENSE", "level": 60 },

                  "minecraft:netherite_helmet": { "skill": "DEFENSE", "level": 75 },
                  "minecraft:netherite_chestplate": { "skill": "DEFENSE", "level": 75 },
                  "minecraft:netherite_leggings": { "skill": "DEFENSE", "level": 75 },
                  "minecraft:netherite_boots": { "skill": "DEFENSE", "level": 75 },

                  "minecraft:elytra": { "skill": "MAGIC", "level": 65 }
                }
                """;
    }

    // Provide default JSON content for weapons
    private static String getDefaultWeaponRequirements() {
        return """
                {
                  "minecraft:wooden_sword": { "skill": "SLAYING", "level": 0 },
                  "minecraft:stone_sword": { "skill": "SLAYING", "level": 10 },
                  "minecraft:golden_sword": { "skill": "SLAYING", "level": 15 },
                  "minecraft:iron_sword": { "skill": "SLAYING", "level": 30 },
                  "minecraft:diamond_sword": { "skill": "SLAYING", "level": 60 },
                  "minecraft:netherite_sword": { "skill": "SLAYING", "level": 75 },

                  "minecraft:crossbow": { "skill": "SLAYING", "level": 5 },
                  "minecraft:bow": { "skill": "SLAYING", "level": 20 },
                  "minecraft:mace": { "skill": "SLAYING", "level": 35 },

                  "minecraft:wooden_axe": {"skill": "SLAYING", "level": 0},
                  "minecraft:stone_axe": {"skill": "SLAYING", "level": 10},
                  "minecraft:golden_axe": {"skill": "SLAYING", "level": 15},
                  "minecraft:iron_axe": {"skill": "SLAYING", "level": 30},
                  "minecraft:diamond_axe": {"skill": "SLAYING", "level": 60},
                  "minecraft:netherite_axe": {"skill": "SLAYING", "level": 75}
                }
                """;
    }

    // Provide default JSON content for magic
    private static String getDefaultMAGICRequirements() {
        return """
                {
                  "minecraft:fortune": {
                    "skill": "MAGIC",
                    "level": 35,
                    "enchantmentLevel": 3
                  },

                  "minecraft:protection": {
                    "skill": "MAGIC",
                    "level": 50,
                    "enchantmentLevel": 4
                  },

                  "minecraft:efficiency": {
                    "skill": "MAGIC",
                    "level": 55,
                    "enchantmentLevel": 5
                  },

                  "minecraft:mending": {
                    "skill": "MAGIC",
                    "level": 60,
                    "enchantmentLevel": 1
                  },

                  "block.minecraft.enchanting_table": {
                    "skill": "MAGIC",
                    "level": 20
                  },

                  "block.minecraft.anvil": {
                    "skill": "MAGIC",
                    "level": 10
                  },
                  "block.minecraft.chipped_anvil": {
                    "skill": "MAGIC",
                    "level": 10
                  },
                  "block.minecraft.damaged_anvil": {
                    "skill": "MAGIC",
                    "level": 10
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

    public static SkillRequirement getMAGICRequirement(String id) {
        return magicRequirements.get(id);
    }
}
