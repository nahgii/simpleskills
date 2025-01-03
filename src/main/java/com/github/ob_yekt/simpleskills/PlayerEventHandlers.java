package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PlayerEventHandlers {

    // Register all event handlers
    public static void registerEvents() {

        // Register Player Join Server events
        registerPlayerJoinEvent();

        // Register XP-related events
        registerXpGainEvent();

        // Register block-break events
        registerBlockBreakEvents();

        // Register defense-related events
        registerDefenseEvents();

        // Register slaying-related events
        registerSlayingEvents();

        // Register skill attribute update event
        registerAttributeUpdateEvent();
    }

    private static void registerPlayerJoinEvent() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player; // Get the player
            String playerUuid = player.getUuidAsString();

            DatabaseManager db = DatabaseManager.getInstance();

            // Check and initialize skills for the player
            try (var rs = db.getPlayerSkills(playerUuid)) {
                boolean hasSkills = false;

                // Check if any skills are already present for this player
                while (rs.next()) {
                    hasSkills = true;
                    break;
                }

                // If the player doesn't have skills, initialize all at level 0 and XP 0
                if (!hasSkills) {
                    for (Skills skill : Skills.values()) {
                        db.savePlayerSkill(playerUuid, skill.name(), 0, 0);
                    }

                    Simpleskills.LOGGER.info("Initialized skills for new player: {}", player.getName().getString());
                }
            } catch (Exception e) {
                Simpleskills.LOGGER.error("Error initializing skills for player {}", player.getName().getString(), e);
            }

            // Update the tab menu with the player's skills
            SkillTabMenu.updateTabMenu(player);
        });
    }

    private static void registerAttributeUpdateEvent() {
        XPManager.setOnXpChangeListener((player, skill) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                AttributeUpdater.updatePlayerAttributes(serverPlayer, skill);
            }
        });
    }

    private static void registerXpGainEvent() {
        // Whenever XP is added, refresh the player's tab menu
        XPManager.setOnXpChangeListener((player, skill) -> {
            SkillTabMenu.updateTabMenu(player); // Update the tab menu after XP gain

        });
    }

    /// Block-breaking skills logic:

    private static void registerBlockBreakEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return true; // Allow breaking if not on server or player is not a server player
            }

            String playerUuid = serverPlayer.getUuidAsString();
            String toolName = serverPlayer.getMainHandStack().getItem().toString();
            String blockTranslationKey = state.getBlock().getTranslationKey();

                // Fetch the tool requirement for the tool being used
                SkillRequirement requirement = RequirementLoader.getToolRequirement(toolName);

                if (requirement != null) {
                    // Identify which skill is required (e.g., Woodcutting, Mining, Excavating)
                    Skills requiredSkill = Skills.valueOf(requirement.getSkill().toUpperCase());
                    int playerLevel = getSkillLevel(playerUuid, requiredSkill);

                    // Compare the player's level to the required level
                    if (playerLevel < requirement.getLevel()) {
                        serverPlayer.sendMessage(
                                Text.of("[SimpleSkills] You need " +
                                        requiredSkill.getDisplayName() + " level " +
                                        requirement.getLevel() + " to break this block with your tool!"),
                                true
                        );
                        return false; // Deny the block-breaking attempt
                    }
                }

            return true; // Allow breaking for blocks without restrictions
        });

        // Triggered after a block is successfully broken
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }

            String blockTranslationKey = state.getBlock().getTranslationKey();

            // XP multipliers for ores based on rarity
            double xpMultiplier = 1.0; // Default multiplier for non-ores (10 XP)
            boolean isOre = false;

            // Check if the block is an ore and set rarity multipliers
            if (blockTranslationKey.contains("coal_ore")) {
                xpMultiplier = 2.0;  // Common but not everywhere like stone, large veins
                isOre = true;
            } else if (blockTranslationKey.contains("nether_quartz_ore")) {
                xpMultiplier = 2.2;  // Common in nether, requires nether access
                isOre = true;
            } else if (blockTranslationKey.contains("copper_ore")) {
                xpMultiplier = 2.5;  // Large veins but less common than coal
                isOre = true;
            } else if (blockTranslationKey.contains("iron_ore")) {
                xpMultiplier = 3.0;  // Essential resource, moderately common
                isOre = true;
            } else if (blockTranslationKey.contains("redstone_ore")) {
                xpMultiplier = 4.0;  // Deep-level only, medium-sized veins
                isOre = true;
            } else if (blockTranslationKey.contains("gold_ore")) {
                xpMultiplier = 5.0;  // Deep-level, rarer than iron, smaller veins
                isOre = true;
            } else if (blockTranslationKey.contains("lapis_ore")) {
                xpMultiplier = 6.0;  // Concentrated at specific heights, important for enchanting
                isOre = true;
            } else if (blockTranslationKey.contains("emerald_ore")) {
                xpMultiplier = 8.0;  // Mountain-only, single blocks rather than veins
                isOre = true;
            } else if (blockTranslationKey.contains("diamond_ore")) {
                xpMultiplier = 10.0; // Deep-level only, smallest veins, most valuable
                isOre = true;
            }

            // Check if the player is using a tool with Silk Touch, but only if the block is an ore
            boolean includesSilkTouch = false;
            if (isOre) {
                ItemStack toolStack = serverPlayer.getEquippedStack(EquipmentSlot.MAINHAND); // Get player's tool
                for (var enchantment : toolStack.getEnchantments().getEnchantments()) {
                    if (enchantment.getIdAsString().equals("minecraft:silk_touch")) {
                        includesSilkTouch = true;
                        break;
                    }
                }

                // If Silk Touch is used on an ore, do not grant XP
                if (includesSilkTouch) {
                    return;
                }
            }
            // Grant XP for ores or other blocks
            if (isOre) {
                XPManager.addXpWithNotification(serverPlayer, Skills.MINING, (int) (ConfigLoader.getBaseXp(Skills.MINING) * xpMultiplier));
            }

            else if (!blockTranslationKey.contains("button")
                    && (blockTranslationKey.contains("stone")
                    || blockTranslationKey.contains("obsidian")
                    || blockTranslationKey.contains("Netherite")
                    || blockTranslationKey.contains("Debris")
                    || blockTranslationKey.contains("prismarine")
                    || blockTranslationKey.contains("purpur")
                    || blockTranslationKey.contains("amethyst")
                    || blockTranslationKey.contains("basalt")
                    || blockTranslationKey.contains("deepslate")
                    || blockTranslationKey.contains("granite")
                    || blockTranslationKey.contains("diorite")
                    || blockTranslationKey.contains("andesite")
                    || blockTranslationKey.contains("brick")
                    || blockTranslationKey.contains("blackstone")
                    || blockTranslationKey.contains("copper")))
            {
                XPManager.addXpWithNotification(serverPlayer, Skills.MINING, (ConfigLoader.getBaseXp(Skills.MINING)));
            }
            else if (!blockTranslationKey.contains("leaves")
                    && !blockTranslationKey.contains("enchanting")
                    && !blockTranslationKey.contains("sign")
                    && !blockTranslationKey.contains("sapling")
                    && !blockTranslationKey.contains("fungus")
                    && !blockTranslationKey.contains("mangrove_roots")
                    && !blockTranslationKey.contains("propagule")
                    && !blockTranslationKey.contains("button")
                    && !blockTranslationKey.equals("block.minecraft.bamboo")
                    && !blockTranslationKey.equals("block.minecraft.hanging_roots")
                    && !blockTranslationKey.equals("block.minecraft.crimson_roots")
                    && !blockTranslationKey.equals("block.minecraft.warped_roots")
                    && (blockTranslationKey.contains("log")
                    || blockTranslationKey.contains("planks")
                    || blockTranslationKey.contains("bookshelf")
                    || blockTranslationKey.contains("root")
                    || blockTranslationKey.contains("door")
                    || blockTranslationKey.contains("barrel")
                    || blockTranslationKey.contains("chest")
                    || blockTranslationKey.contains("lectern")
                    || blockTranslationKey.contains("loom")
                    || blockTranslationKey.contains("campfire")
                    || blockTranslationKey.contains("fence")
                    || blockTranslationKey.contains("gate")
                    || blockTranslationKey.contains("wood")
                    || blockTranslationKey.contains("oak")
                    || blockTranslationKey.contains("spruce")
                    || blockTranslationKey.contains("birch")
                    || blockTranslationKey.contains("jungle")
                    || blockTranslationKey.contains("acacia")
                    || blockTranslationKey.contains("dark_oak")
                    || blockTranslationKey.contains("pale_oak")
                    || blockTranslationKey.contains("mangrove")
                    || blockTranslationKey.contains("cherry")
                    || blockTranslationKey.contains("bamboo")
                    || blockTranslationKey.contains("crimson")
                    || blockTranslationKey.contains("warped")))
            {
                XPManager.addXpWithNotification(serverPlayer, Skills.WOODCUTTING, (ConfigLoader.getBaseXp(Skills.WOODCUTTING)));
            }
            else if (blockTranslationKey.contains("dirt") || blockTranslationKey.contains("sand")
                    || blockTranslationKey.contains("gravel")
                    || blockTranslationKey.contains("clay")
                    || blockTranslationKey.contains("podzol")
                    || blockTranslationKey.contains("mycelium")
                    || blockTranslationKey.contains("farmland")
                    || blockTranslationKey.contains("concretePowder")
                    || blockTranslationKey.contains("mud")
                    || blockTranslationKey.contains("grass_block")
                    || blockTranslationKey.contains("soil")) {
                XPManager.addXpWithNotification(serverPlayer, Skills.EXCAVATING, (ConfigLoader.getBaseXp(Skills.EXCAVATING)));
            }
        });
    }


    /// Defense XP system

    private static void registerDefenseEvents() {
        // Listen for living entity damage and filter only players
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                handleDefenseXP(player, damageSource, damageAmount);
            }
            return true; // Allow the damage to proceed
        });
    }

    // Handles granting XP for defense based on damage taken
    private static void handleDefenseXP(ServerPlayerEntity player, DamageSource source, float damageAmount) {
        final float MIN_DAMAGE_THRESHOLD = 1.0F; // Ignore insignificant damage
        if (damageAmount < MIN_DAMAGE_THRESHOLD) return;

        // Prevent XP gain for invalid damage sources
        if (isInvalidDamageSource(source)) return;

        // If the player is blocking with a shield, grant shield block XP
        if (isShieldBlocking(player)) {
            if (!isInvalidShieldBlockingSource(source)) {
                float shieldXpMultiplier = 0.5f;
                int xpGained = Math.round(damageAmount * (ConfigLoader.getBaseXp(Skills.DEFENSE)) * shieldXpMultiplier);
                XPManager.addXpWithNotification(player, Skills.DEFENSE, xpGained); // Add Shield Defense XP
            }
            return; // Shield block XP granted, no further Defense XP
        }

        // Calculate the number of equipped armor pieces
        int armorCount = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot() && !player.getEquippedStack(slot).isEmpty()) {
                armorCount++;
            }
        }

        // Grant Defense XP if the player has any armor equipped
        if (armorCount > 0) {
            float armorMultiplier = 1.0f + (0.25f * armorCount); // Bonus scaling for more armor
            int xpGained = Math.round(damageAmount * (ConfigLoader.getBaseXp(Skills.DEFENSE)) * armorMultiplier);

            // Add Defense XP using the centralized method
            XPManager.addXpWithNotification(player, Skills.DEFENSE, xpGained);
        }
    }

    // Checks if the player is actively blocking with a shield
    private static boolean isShieldBlocking(ServerPlayerEntity player) {
        return player.isBlocking() && player.getActiveItem().getItem() == Items.SHIELD;
    }

    // Validates whether the damage source allows granting XP
    private static boolean isInvalidDamageSource(DamageSource source) {
        // Allow XP only for damage caused by entities or projectiles
        return !(source.getSource() instanceof net.minecraft.entity.Entity
                || source.getSource() instanceof net.minecraft.entity.projectile.ProjectileEntity);
    }

    // Validates whether the shield blocking damage source allows granting XP
    private static boolean isInvalidShieldBlockingSource(DamageSource source) {
        // Allow XP only for blocking damage caused by entities or projectiles
        return !(source.getSource() instanceof net.minecraft.entity.Entity
                || source.getSource() instanceof net.minecraft.entity.projectile.ProjectileEntity);
    }

    /// Slaying restrictions and XP system using the centralized method:

    private static final float MIN_DAMAGE_THRESHOLD = 2.0F; // Minimum damage to grant XP

    private static void registerSlayingEvents() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, damageSource, damageAmount) -> {
            if (damageSource.getAttacker() instanceof net.minecraft.server.network.ServerPlayerEntity attacker) {
                ItemStack weapon = attacker.getMainHandStack();

                if (weapon.isEmpty()) return true; // Allow attacks without granting XP for no weapon

                // Fetch the weapon's identifier
                String weaponName = Registries.ITEM.getId(weapon.getItem()).toString();
                SkillRequirement requirement = RequirementLoader.getWeaponRequirement(weaponName);

                // Check if the player meets the required level for the weapon
                if (requirement != null && "Slaying".equalsIgnoreCase(requirement.getSkill())) {
                    int requiredLevel = requirement.getLevel();
                    int playerLevel = getSkillLevel(attacker.getUuidAsString(), Skills.SLAYING);

                    if (playerLevel < requiredLevel) {
                        attacker.sendMessage(
                                Text.of("[SimpleSkills] You need Slaying level " + requiredLevel + " to use this weapon!"),
                                true
                        );
                        return false; // Block the attack due to unmet level requirements
                    }
                }

                // Now check if the damage is above the threshold (AFTER the weapon requirement is verified)
                if (damageAmount < MIN_DAMAGE_THRESHOLD) return true;

                // Grant Slaying XP if the target is NOT an Armor Stand
                if (!(target instanceof net.minecraft.entity.decoration.ArmorStandEntity)) {
                    int xpGained = Math.round(damageAmount*(ConfigLoader.getBaseXp(Skills.SLAYING))); // XP per damage point
                    XPManager.addXpWithNotification(attacker, Skills.SLAYING, xpGained); // Grant Slaying XP
                }
            }

            return true; // Allow the damage to proceed
        });
    }


    /// Query the SQL database for a player's skill level

    private static int getSkillLevel(String playerUuid, Skills skill) {
        DatabaseManager db = DatabaseManager.getInstance();
        try (var rs = db.getPlayerSkills(playerUuid)) {
            while (rs.next()) {
                if (rs.getString("skill").equals(skill.name())) {
                    return rs.getInt("level");
                }
            }
        } catch (Exception e) {
            Simpleskills.LOGGER.error("Error checking skill level for player {}", playerUuid, e);
        }
        return 0; // Default skill level
    }
}