package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
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
        registerXPGainEvent();

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

            // Assign Peasant as the default class if none is set
            String playerClass = db.getPlayerClass(playerUuid);
            if (playerClass == null || playerClass.isEmpty() || playerClass.equalsIgnoreCase("NONE")) {
                db.setPlayerClass(playerUuid, "PEASANT");
                Simpleskills.LOGGER.info("Defaulted player {} to 'PEASANT' class.", player.getName().getString());
            }

            // Update the tab menu with the player's skills
            SkillTabMenu.updateTabMenu(player);
        });
    }

    private static void registerAttributeUpdateEvent() {
        XPManager.setOnXPChangeListener((player, skill) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                AttributeUpdater.updatePlayerAttributes(serverPlayer, skill);
            }
        });
    }

    private static void registerXPGainEvent() {
        // Whenever XP is added, refresh the player's tab menu
        XPManager.setOnXPChangeListener((player, skill) -> {
            SkillTabMenu.updateTabMenu(player); // Update the tab menu after XP gain

        });
    }

    /// Block-breaking skills logic

    private static void registerBlockBreakEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return true;
            }

            String blockTranslationKey = state.getBlock().getTranslationKey();
            String toolName = serverPlayer.getMainHandStack().getItem().toString();

            // Get the relevant skill for this block
            Skills relevantSkill = getRelevantSkill(blockTranslationKey);

            // Only check tool requirements if the block is related to a skill
            if (relevantSkill != null) {
                // Special handling for crops - don't require tools
                if (isCrop(blockTranslationKey)) {
                    return true;
                }

                SkillRequirement requirement = RequirementLoader.getToolRequirement(toolName);
                if (requirement != null) {
                    // Make sure the requirement matches the relevant skill
                    if (requirement.getSkill().equalsIgnoreCase(relevantSkill.name())) {
                        int playerLevel = getSkillLevel(serverPlayer.getUuidAsString(), relevantSkill);
                        if (playerLevel < requirement.getLevel()) {
                            serverPlayer.sendMessage(Text.of("§6[simpleskills]§f You need " + relevantSkill.getDisplayName() + " level " + requirement.getLevel() + " to break this block with your tool!"), true);
                            return false;
                        }
                    }
                }
            }

            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }

            String blockTranslationKey = state.getBlock().getTranslationKey();
            Skills relevantSkill = getRelevantSkill(blockTranslationKey);

            // Skip if no relevant skill is associated with the block
            if (relevantSkill == null) {
                return;
            }

            // Handle farming crops and blocks
            if (relevantSkill == Skills.FARMING) {
                // First check for crop-specific XP
                grantFarmingXP((ServerWorld) world, serverPlayer, state, blockTranslationKey);

                // Check for farming blocks
                if (isFarmingBlock(blockTranslationKey)) {
                    // Check for both Silk Touch and Shears
                    ItemStack mainHandItem = serverPlayer.getMainHandStack();
                    if (SilkTouchHandler.hasSilkTouch(serverPlayer) || mainHandItem.isOf(Items.SHEARS)) {
                        return; // No XP for blocks harvested with Silk Touch or Shears
                    }

                    int baseXP = ConfigLoader.getBaseXP(relevantSkill);
                    int reducedXP = (int) (baseXP * 0 + 2 ); // Grant 1 XP
                    XPManager.addXPWithNotification(serverPlayer, relevantSkill, reducedXP);
                }
            }
            // Handle mining skill XP with ore multipliers
            else if (relevantSkill == Skills.MINING) {
                double XPMultiplier = getOreMultiplier(blockTranslationKey);
                boolean isOre = XPMultiplier > 1.0;

                // Check Silk Touch for ores
                if (isOre && SilkTouchHandler.hasSilkTouch(serverPlayer)) {
                    return; // No XP for ores mined with Silk Touch
                }

                XPManager.addXPWithNotification(serverPlayer, Skills.MINING, (int) (ConfigLoader.getBaseXP(Skills.MINING) * XPMultiplier));
            }
            // Handle woodcutting and excavating XP
            else {
                XPManager.addXPWithNotification(serverPlayer, relevantSkill, ConfigLoader.getBaseXP(relevantSkill));
            }
        });
    }

    // Helper method to determine which skill (if any) a block is related to
    private static Skills getRelevantSkill(String blockTranslationKey) {

        // Check crops first
        if (isCrop(blockTranslationKey)) {
            return Skills.FARMING;
        }
        // Then check other farming blocks
        if (isFarmingBlock(blockTranslationKey)) {
            return Skills.FARMING;
        }
        // Check mining blocks
        if (isMiningBlock(blockTranslationKey)) {
            return Skills.MINING;
        }
        // Check woodcutting blocks
        if (isWoodcuttingBlock(blockTranslationKey)) {
            return Skills.WOODCUTTING;
        }
        // Check excavating blocks
        if (isExcavatingBlock(blockTranslationKey)) {
            return Skills.EXCAVATING;
        }

        return null;
    }

    private static boolean isCrop(String blockTranslationKey) {
        return blockTranslationKey.contains("wheat") || blockTranslationKey.contains("carrots")
                || blockTranslationKey.contains("potatoes") || blockTranslationKey.contains("beetroots") || blockTranslationKey.contains("nether_wart")
                || blockTranslationKey.contains("cocoa") || blockTranslationKey.contains("melon");
    }

    private static boolean isFarmingBlock(String blockTranslationKey) {
        return blockTranslationKey.contains("sculk") || blockTranslationKey.contains("wart_block")
                || blockTranslationKey.contains("leaves") || blockTranslationKey.contains("shroomlight") || blockTranslationKey.contains("sponge")
                || blockTranslationKey.contains("hay_block") || blockTranslationKey.contains("target") || blockTranslationKey.contains("dried_kelp_block")
                || blockTranslationKey.contains("moss_block") || blockTranslationKey.contains("moss_carpet");
    }

    private static boolean isMiningBlock(String blockTranslationKey) {
        return blockTranslationKey.contains("_ore") || (!blockTranslationKey.contains("button")
                && !blockTranslationKey.contains("grindstone") && !blockTranslationKey.contains("pressure") &&

                (blockTranslationKey.contains("stone") || blockTranslationKey.contains("obsidian")
                        || blockTranslationKey.contains("netherite") || blockTranslationKey.contains("debris") || blockTranslationKey.contains("tuff")
                        || blockTranslationKey.contains("prismarine") || blockTranslationKey.contains("purpur") || blockTranslationKey.contains("amethyst")
                        || blockTranslationKey.contains("basalt") || blockTranslationKey.contains("deepslate") || blockTranslationKey.contains("granite")
                        || blockTranslationKey.contains("diorite") || blockTranslationKey.contains("andesite") || blockTranslationKey.contains("brick")
                        || blockTranslationKey.contains("blackstone") || blockTranslationKey.contains("copper")));
    }

    private static boolean isWoodcuttingBlock(String blockTranslationKey) {
        return !blockTranslationKey.contains("leaves") && !blockTranslationKey.contains("enchanting")
                && !blockTranslationKey.contains("sign") && !blockTranslationKey.contains("sapling") && !blockTranslationKey.contains("fungus")
                && !blockTranslationKey.contains("mangrove_roots") && !blockTranslationKey.contains("propagule") && !blockTranslationKey.contains("button")
                && !blockTranslationKey.contains("pressure") && !blockTranslationKey.equals("block.minecraft.bamboo")
                && !blockTranslationKey.equals("block.minecraft.hanging_roots") && !blockTranslationKey.equals("block.minecraft.crimson_roots")
                && !blockTranslationKey.equals("block.minecraft.warped_roots") &&

                (blockTranslationKey.contains("log") || blockTranslationKey.contains("planks")
                        || blockTranslationKey.contains("fence") || blockTranslationKey.contains("gate") || blockTranslationKey.contains("wood")
                        || blockTranslationKey.contains("oak") || blockTranslationKey.contains("spruce") || blockTranslationKey.contains("birch")
                        || blockTranslationKey.contains("jungle") || blockTranslationKey.contains("acacia") || blockTranslationKey.contains("dark_oak")
                        || blockTranslationKey.contains("pale_oak") || blockTranslationKey.contains("mangrove") || blockTranslationKey.contains("cherry")
                        || blockTranslationKey.contains("bamboo") || blockTranslationKey.contains("crimson") || blockTranslationKey.contains("warped"));
    }

    private static boolean isExcavatingBlock(String blockTranslationKey) {
        return blockTranslationKey.contains("dirt") || blockTranslationKey.contains("sand")
                || blockTranslationKey.contains("gravel") || blockTranslationKey.contains("clay") || blockTranslationKey.contains("podzol")
                || blockTranslationKey.contains("mycelium") || blockTranslationKey.contains("farmland") || blockTranslationKey.contains("concretePowder")
                || blockTranslationKey.contains("mud") || blockTranslationKey.contains("grass_block") || blockTranslationKey.contains("soil");
    }

    private static double getOreMultiplier(String blockTranslationKey) {
        if (blockTranslationKey.contains("nether_quartz_ore")) return 1.5;
        if (blockTranslationKey.contains("coal_ore")) return 2.0;
        if (blockTranslationKey.contains("copper_ore")) return 2.5;
        if (blockTranslationKey.contains("iron_ore")) return 3.0;
        if (blockTranslationKey.contains("redstone_ore")) return 4.0;
        if (blockTranslationKey.contains("gold_ore")) return 5.0;
        if (blockTranslationKey.contains("lapis_ore")) return 5.5;
        if (blockTranslationKey.contains("emerald_ore")) return 8.0;
        if (blockTranslationKey.contains("diamond_ore")) return 10.0;
        return 1.0; // Default multiplier for non-ores
    }

    ///  Farming Crop XP system

    private static void grantFarmingXP(ServerWorld world, ServerPlayerEntity serverPlayer, BlockState state, String blockTranslationKey) {
        // Handle fully grown crops with growth stages
        if (state.contains(Properties.AGE_7)) {
            int age = state.get(Properties.AGE_7);
            // Full XP for mature crops
            if (age == 7 && (blockTranslationKey.contains("wheat") || blockTranslationKey.contains("carrots") || blockTranslationKey.contains("potatoes"))) {
                XPManager.addXPWithNotification(serverPlayer, Skills.FARMING, ConfigLoader.getBaseXP(Skills.FARMING));
            }
        } else if (state.contains(Properties.AGE_3)) {
            int age = state.get(Properties.AGE_3);
            // Handle mature beetroots and nether wart
            if (age == 3 && (blockTranslationKey.contains("beetroots") || blockTranslationKey.contains("nether_wart"))) {
                XPManager.addXPWithNotification(serverPlayer, Skills.FARMING, ConfigLoader.getBaseXP(Skills.FARMING));
            }
        } else if (state.contains(Properties.AGE_2)) {
            int age = state.get(Properties.AGE_2);
            // Handle mature cocoa pods
            if (age == 2 && blockTranslationKey.contains("cocoa")) {
                XPManager.addXPWithNotification(serverPlayer, Skills.FARMING, ConfigLoader.getBaseXP(Skills.FARMING));
            }
        }

        // Handle special crops that don't use age properties
        if (blockTranslationKey.contains("melon") || blockTranslationKey.contains("pumpkin")) {
            if (!SilkTouchHandler.hasSilkTouch(serverPlayer)) {
                XPManager.addXPWithNotification(serverPlayer, Skills.FARMING, ConfigLoader.getBaseXP(Skills.FARMING));
            }
        }
    }

    /// Defense XP system

    private static void registerDefenseEvents() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                handleDefenseXP(player, damageSource, damageAmount);
            }
            return true; // Allow the damage to proceed
        });
    }

    private static void handleDefenseXP(ServerPlayerEntity player, DamageSource source, float damageAmount) {
        final float MIN_DAMAGE_THRESHOLD = 2.0F; // Ignore insignificant damage
        if (damageAmount < MIN_DAMAGE_THRESHOLD) return;

        // Prevent XP gain for invalid damage sources
        if (isInvalidDamageSource(source)) return;

        // If the player is blocking with a shield, grant shield block XP
        if (isShieldBlocking(player)) {
            if (!isInvalidShieldBlockingSource(source)) {
                float shieldXPMultiplier = 0.3f; // Shields only grant 30% XP
                int XPGained = Math.round(damageAmount * (ConfigLoader.getBaseXP(Skills.DEFENSE)) * shieldXPMultiplier);
                XPManager.addXPWithNotification(player, Skills.DEFENSE, XPGained); // Add Shield Defense XP
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

        // Calculate the XP multiplier based on the number of armor pieces equipped
        float armorMultiplier = 0.25f * armorCount; // Each armor piece adds 0.25 to the multiplier

        // Grant Defense XP if the player has any armor equipped
        if (armorMultiplier > 0) {
            int XPGained = Math.round(damageAmount * ConfigLoader.getBaseXP(Skills.DEFENSE) * armorMultiplier);
            XPManager.addXPWithNotification(player, Skills.DEFENSE, XPGained);
        }
    }

    private static boolean isShieldBlocking(ServerPlayerEntity player) {
        return player.isBlocking() && player.getActiveItem().getItem() == Items.SHIELD;
    }

    private static boolean isInvalidDamageSource(DamageSource source) {
        // Check for explosion damage types
        if (source.isOf(net.minecraft.entity.damage.DamageTypes.EXPLOSION) || source.isOf(net.minecraft.entity.damage.DamageTypes.PLAYER_EXPLOSION)) {
            return true;
        }

        // Allow XP only for damage caused by entities or projectiles
        return !(source.getSource() instanceof net.minecraft.entity.Entity || source.getSource() instanceof net.minecraft.entity.projectile.ProjectileEntity);
    }

    private static boolean isInvalidShieldBlockingSource(DamageSource source) {
        // Check for explosion damage types
        if (source.isOf(net.minecraft.entity.damage.DamageTypes.EXPLOSION) || source.isOf(net.minecraft.entity.damage.DamageTypes.PLAYER_EXPLOSION)) {
            return true;
        }

        // Allow XP only for blocking damage caused by entities or projectiles
        return !(source.getSource() instanceof net.minecraft.entity.Entity || source.getSource() instanceof net.minecraft.entity.projectile.ProjectileEntity);
    }

    /// Slaying restrictions and XP system using the centralized method:

    private static final float MIN_DAMAGE_THRESHOLD = 4.0F; // Minimum damage to grant XP

    private static void registerSlayingEvents() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, damageSource, damageAmount) -> {
            if (damageSource.getAttacker() instanceof net.minecraft.server.network.ServerPlayerEntity attacker) {
                // Get the weapon/item from the player's main hand
                ItemStack itemStack = attacker.getMainHandStack();

                if (itemStack.isEmpty()) return true; // Allow attacks without granting XP for empty hands

                // Get the item's registry name
                String itemName = Registries.ITEM.getId(itemStack.getItem()).toString();

                // Check if the item is a sword
                boolean isSword = PerkHandler.isSword(itemStack.getItem());

                // Check if the item is an axe
                boolean isAxe = PerkHandler.isAxe(itemStack.getItem());

                // Lookup the weapon's requirements dynamically
                SkillRequirement weaponRequirement = RequirementLoader.getWeaponRequirement(itemName);

                if (weaponRequirement != null && "Slaying".equalsIgnoreCase(weaponRequirement.getSkill())) {
                    int requiredLevel = weaponRequirement.getLevel();
                    int playerLevel;

                    // Handle custom behavior for "Brute" perk holders
                    if (PerkHandler.doesPlayerHavePerk(attacker, "Brute")) {

                        // Use Woodcutting level to meet requirements for axes
                        if (isAxe) {
                            playerLevel = getSkillLevel(attacker.getUuidAsString(), Skills.WOODCUTTING);

                            if (playerLevel < requiredLevel) {
                                attacker.sendMessage(Text.of("§6[simpleskills]§f You need Woodcutting level " + requiredLevel + " to use this axe!"), true);
                                return false; // Block the attack due to insufficient Woodcutting level
                            }
                        } else {
                            // Non-axes fallback to normal Slaying level
                            playerLevel = getSkillLevel(attacker.getUuidAsString(), Skills.SLAYING);

                            if (playerLevel < requiredLevel) {
                                attacker.sendMessage(Text.of("§6[simpleskills]§f You need Slaying level " + requiredLevel + " to use this weapon!"), true);
                                return false; // Block the attack due to insufficient Slaying level
                            }
                        }
                    } else {
                        // Non-Brute players always use Slaying level for weapon checks
                        playerLevel = getSkillLevel(attacker.getUuidAsString(), Skills.SLAYING);

                        if (playerLevel < requiredLevel) {
                            attacker.sendMessage(Text.of("§6[simpleskills]§f You need Slaying level " + requiredLevel + " to use this weapon!"), true);
                            return false; // Block the attack due to insufficient Slaying level
                        }
                    }
                }

                // Check if the damage is above the minimum threshold
                if (damageAmount < MIN_DAMAGE_THRESHOLD) return true;

                // Grant XP for Slaying after the attack
                if (!(target instanceof net.minecraft.entity.decoration.ArmorStandEntity)) {
                    // Scale XP by the damage dealt and add a base XP value
                    int XPGained = Math.round(damageAmount / 2 + ConfigLoader.getBaseXP(Skills.SLAYING)); // XP based on damage and base XP

                    // Add the XP to the player
                    XPManager.addXPWithNotification(attacker, Skills.SLAYING, XPGained);
                }

            }

            return true; // Allow the attack to proceed
        });
    }

    public static boolean isWeaponAllowed(ServerPlayerEntity attacker) {
        ItemStack weapon = attacker.getMainHandStack();

        // Allow attacks without a weapon (hand-to-hand combat)
        if (weapon.isEmpty()) return true;


        // Validate skill requirements for weapon
        String weaponName = Registries.ITEM.getId(weapon.getItem()).toString();
        SkillRequirement requirement = RequirementLoader.getWeaponRequirement(weaponName);
        if (requirement != null && "Slaying".equalsIgnoreCase(requirement.getSkill())) {
            int requiredLevel = requirement.getLevel();
            int playerLevel = getSkillLevel(attacker.getUuidAsString(), Skills.SLAYING);

            if (playerLevel < requiredLevel) {
                attacker.sendMessage(Text.of("§6[simpleskills]§f You need Slaying level " + requiredLevel + " to use this weapon!"), true);
                return false; // Block usage
            }
        }

        return true; // Allow weapon if all conditions pass
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