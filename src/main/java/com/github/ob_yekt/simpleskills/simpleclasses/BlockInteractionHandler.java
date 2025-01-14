package com.github.ob_yekt.simpleskills.simpleclasses;

import com.github.ob_yekt.simpleskills.*;
import com.github.ob_yekt.simpleskills.data.DatabaseManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.Items;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import static com.github.ob_yekt.simpleskills.AttributeUpdater.applyPerkAttributes;
import static com.github.ob_yekt.simpleskills.AttributeUpdater.clearPerkAttributes;
import static com.github.ob_yekt.simpleskills.AttributeUpdater.RefreshSkillAttributes;


public class BlockInteractionHandler {

    // Register the unified UseBlockCallback handler
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS; // Ignore client-side interactions

            BlockState blockState = world.getBlockState(hitResult.getBlockPos());
            Block block = blockState.getBlock();

            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            // Check if the block interaction involves a class reset (Cauldron + Feather)
            if (block instanceof AbstractCauldronBlock) {
                return handleClassRevoke(serverPlayer, blockState, world);
            }

            // Check if the block interaction involves class selection (Book + Specific Blocks)
            if (serverPlayer.getMainHandStack().getItem() == Items.BOOK) {
                return handleClassSelection(serverPlayer, blockState, block);
            }

            // Check if the block interaction involves skill restrictions (e.g., Anvil, Enchanting Table)
            if (block instanceof AnvilBlock || block instanceof EnchantingTableBlock) {
                return handleSkillRestrictions(serverPlayer, blockState, block);
            }

            return ActionResult.PASS;
        });
    }

    // Handles resetting a class to Peasant using a Cauldron and Feather
    private static ActionResult handleClassRevoke(ServerPlayerEntity serverPlayer, BlockState blockState, World world) {
        ItemStack heldItem = serverPlayer.getMainHandStack();

        // Check if the player is holding a Feather to revoke the class
        if (heldItem.getItem() == Items.FEATHER) {
            String playerUuid = serverPlayer.getUuidAsString();
            String currentClass = DatabaseManager.getInstance().getPlayerClass(playerUuid);

            // Check if classes are enabled in config.json
            if (!ConfigLoader.isFeatureEnabled("classes")) {
                serverPlayer.sendMessage(Text.literal("§6[simpleskills]§f Classes are currently disabled by the configuration."), false);
                return ActionResult.FAIL;
            }

            // Prevent revocation if the player is already a Peasant
            if (currentClass == null || currentClass.trim().isEmpty() || currentClass.equalsIgnoreCase(PlayerClass.PEASANT.name())) {
                return ActionResult.FAIL;
            }

            // Reset the primary skill associated with the player's current class
            PlayerClass classBeingRevoked = PlayerClass.valueOf(currentClass);
            String primarySkillName = classBeingRevoked.getPrimarySkill();

            if (primarySkillName != null && !primarySkillName.isEmpty()) {
                // Use XPManager to calculate experience and set level to 0
                Skills primarySkill;
                try {
                    primarySkill = Skills.valueOf(primarySkillName.toUpperCase());
                    int newXP = XPManager.getExperienceForLevel(0); // 0 XP for level 0

                    // Save the reset skill level to the database
                    DatabaseManager.getInstance().savePlayerSkill(playerUuid, primarySkill.name(), newXP, 0);

                    // Send a message to the player about the skill reset
                    serverPlayer.sendMessage(Text.literal("§6[simpleskills]§f Your primary skill '" + primarySkill.getDisplayName() + "' has been reset."), false);


                } catch (IllegalArgumentException e) {
                    serverPlayer.sendMessage(Text.literal("§6[simpleskills]§f Failed to reset the skill associated with your class."), false);
                }
            }

            // Clear Perk Attributes for the player's previous class
            clearPerkAttributes(serverPlayer);

            // Reset class to Peasant
            DatabaseManager.getInstance().setPlayerClass(playerUuid, PlayerClass.PEASANT.name());

            // Update the player's skill attributes
            RefreshSkillAttributes(serverPlayer);

            // Notify the player about the class reset
            serverPlayer.sendMessage(Text.literal("§6[simpleskills]§f Your class has been revoked, and you are now a Peasant."), false);
            SkillTabMenu.updateTabMenu(serverPlayer);

            // Consume one Feather from the player's inventory
            heldItem.decrement(1);

            // Generate particle and sound effects for the reset
            double x = serverPlayer.getX();
            double y = serverPlayer.getY() + 1; // Slightly above the player
            double z = serverPlayer.getZ();

            if (world instanceof ServerWorld serverWorld) {
                // Smoky dissipation effect
                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,  // Symbolizing class loss
                        x, y, z,
                        40,                  // Number of particles
                        0.7,                 // Spread in X
                        1.5,                 // Spread in Y
                        0.7,                 // Spread in Z
                        0.1                  // Speed multiplier
                );

                // Mystical fade effect
                serverWorld.spawnParticles(
                        ParticleTypes.SOUL,  // Symbolizing the spirit of the old class fading
                        x, y, z,
                        20,                  // Number of particles
                        0.4,                 // Spread in X
                        1.5,                 // Spread in Y
                        0.4,                 // Spread in Z
                        0.05                 // Speed multiplier
                );

                // Play sound effect for the class reset
                world.playSound(
                        null,                                             // Target all nearby players
                        serverPlayer.getBlockPos(),                      // Location of the player
                        SoundEvents.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, // Example sound for mysticism
                        SoundCategory.PLAYERS,                           // Player-related sounds
                        1.0F,                                            // Volume
                        1.0F                                             // Pitch
                );
            }

            return ActionResult.SUCCESS;
        }

        // Default return if the player is not holding a Feather
        return ActionResult.PASS;
    }

    // Handles class selection for specific blocks using a Book
    private static ActionResult handleClassSelection(ServerPlayerEntity serverPlayer, BlockState blockState, Block block) {
        String playerUuid = serverPlayer.getUuidAsString();
        String currentClass = DatabaseManager.getInstance().getPlayerClass(playerUuid);

        // Check if classes are enabled in config.json
        if (!ConfigLoader.isFeatureEnabled("classes")) {
            serverPlayer.sendMessage(Text.literal("§6[simpleskills]§f Classes are currently disabled by the configuration."), false);
            return ActionResult.FAIL;
        }

        // Determine the class based on the block type
        PlayerClass assignedClass = determineClassFromBlock(block, blockState);
        if (assignedClass == null) return ActionResult.PASS;

        // Restrict class selection to Peasant players only
        if (currentClass != null && !currentClass.equalsIgnoreCase(PlayerClass.PEASANT.name())) {
            serverPlayer.sendMessage(Text.literal("§6[simpleskills]§f You must reset your class to Peasant before choosing a new class."), false);
            return ActionResult.FAIL;
        }

        // Assign the player to the selected class
        assignClassWithLoreBook(serverPlayer, assignedClass);

        // Apply Perk & Skill Attributes
        applyPerkAttributes(serverPlayer);
        RefreshSkillAttributes(serverPlayer);

        // Retrieve the primary skill of the newly assigned class
        String primarySkillName = assignedClass.getPrimarySkill();
        if (primarySkillName != null && !primarySkillName.isEmpty()) {
            try {
                Skills primarySkill = Skills.valueOf(primarySkillName.toUpperCase());
                int currentSkillLevel = XPManager.getSkillLevel(playerUuid, primarySkill);

                if (currentSkillLevel < 10) {
                    int XPForLevel10 = XPManager.getExperienceForLevel(10);
                    DatabaseManager.getInstance().savePlayerSkill(playerUuid, primarySkill.name(), XPForLevel10, 10);
                    // Refresh SkillTabMenu
                    SkillTabMenu.updateTabMenu(serverPlayer);
                }
            } catch (IllegalArgumentException e) {
                Simpleskills.LOGGER.error("Failed to assign primary skill '{}' for class '{}'", primarySkillName, assignedClass.name(), e);
            }
        }

        return ActionResult.SUCCESS;
    }

    // Handles skill restrictions for specific blocks like Anvils and Enchanting Tables
    private static ActionResult handleSkillRestrictions(ServerPlayerEntity serverPlayer, BlockState blockState, Block block) {
        String blockID = blockState.getBlock().getTranslationKey();
        SkillRequirement requirement = RequirementLoader.getMAGICRequirement(blockID);

        if (requirement == null) return ActionResult.PASS;

        int playerMAGICLevel = XPManager.getSkillLevel(serverPlayer.getUuidAsString(), Skills.MAGIC);

        if (playerMAGICLevel < requirement.getLevel()) {
            serverPlayer.sendMessage(Text.literal("§6[simpleskills]§f You need MAGIC level " + requirement.getLevel()
                    + " to use this block!"), true);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Helper method to determine the player's class based on the block
    private static PlayerClass determineClassFromBlock(Block block, BlockState blockState) {
        if (block instanceof SmithingTableBlock) return PlayerClass.KNIGHT;
        if (block instanceof GrindstoneBlock) return PlayerClass.ROGUE;
        if (block instanceof LoomBlock) return PlayerClass.NOMAD;
        if (block instanceof ComposterBlock) return PlayerClass.FARMHAND;
        if (block instanceof FletchingTableBlock) return PlayerClass.LUMBERJACK;
        if (block instanceof BlastFurnaceBlock) return PlayerClass.MINER;
        if (block == Blocks.BOOKSHELF) return PlayerClass.WIZARD;
        return null;
    }

    // Assign the player a class and generate a lore book
    private static void assignClassWithLoreBook(ServerPlayerEntity player, PlayerClass playerClass) {
        String playerUuid = player.getUuidAsString();
        DatabaseManager.getInstance().setPlayerClass(playerUuid, playerClass.name());
        player.sendMessage(Text.literal("§6[simpleskills]§f You have chosen the " + playerClass.getDisplayName() + " class!"), false);

        // Skill Attribute update
        RefreshSkillAttributes(player);

        // Get the position to spawn particles and play sound
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Spawn particles in the server world (only if the world is an instance of ServerWorld)
        if (player.getServerWorld() instanceof ServerWorld serverWorld) {

            serverWorld.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    x, y, z,
                    60,
                    0.4,
                    1.5,
                    0.4,
                    0.05
            );

            serverWorld.spawnParticles(
                    ParticleTypes.SNEEZE,
                    x, y, z,
                    20,
                    0.8,
                    1.5,
                    0.8,
                    0.05
            );

            serverWorld.spawnParticles(
                    ParticleTypes.GLOW,
                    x, y, z,
                    40,
                    0.5,
                    1.5,
                    0.5,
                    0.05
            );
        }

        // Play sound to inform the player of class selection
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.0F, 1.0F);


        // Handle the lore book creation
        ItemStack heldItem = player.getMainHandStack(); // Current book stack
        ItemStack loreBook = new ItemStack(Items.WRITTEN_BOOK);

        // Custom dynamic title and content for books based on class
        String bookTitle = generateCustomTitle(playerClass); // Custom title for the book
        String author = ""; // Author of the book (can be changed to "simpleskills" if necessary)

        // Generate lore book contents, now as multiple pages
        List<String> lorePages = generateClassLore(playerClass);
        List<RawFilteredPair<Text>> pageContents = new ArrayList<>();
        for (String pageText : lorePages) {
            // Convert the String to a Text object before passing it to RawFilteredPair
            pageContents.add(RawFilteredPair.of(Text.of(pageText))); // Convert String to Text here
        }

        // Here, we're keeping the title as a String since it seems the API expects it
        WrittenBookContentComponent bookContent = new WrittenBookContentComponent(
                RawFilteredPair.of(bookTitle), // Keep as String if that's what the API expects
                author,
                0, // Page number starts from 0
                pageContents, // All pages of lore as Text
                true
        );

        loreBook.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, bookContent);

        // Consume one book and drop remaining stack
        if (heldItem.getCount() > 1) {
            heldItem.decrement(1); // Decrease stack size by 1
            ItemStack remainingBooks = new ItemStack(Items.BOOK, heldItem.getCount());
            player.dropItem(remainingBooks, false); // Drop remaining books onto the ground
        } else {
            player.getInventory().removeStack(player.getInventory().getSlotWithStack(heldItem)); // Remove last book from inventory
        }

        // Replace book in hand with the lore book
        player.setStackInHand(player.getActiveHand(), loreBook);
    }

    // Generate custom titles based on the player's class
    private static String generateCustomTitle(PlayerClass playerClass) {
        return switch (playerClass) {
            case KNIGHT -> "Pristine Codex";
            case ROGUE -> "Mysterious Note";
            case NOMAD -> "Brittle Scroll";
            case FARMHAND -> "Scented Almanac";
            case LUMBERJACK -> "Worn Journal";
            case MINER -> "Soot-covered Diary";
            case WIZARD -> "Shimmering Grimoire";
            default -> "Unknown Tome";
        };
    }

    // Generate detailed custom lore body text for the player's class
    private static List<String> generateClassLore(PlayerClass playerClass) {
        String loreText = switch (playerClass) {
            case KNIGHT -> """
                    The Knight’s Codex, bound in iron-studded leather, holds thick, cream-colored pages with little wear. Written by a nameless protector, it tells of generations devoted to shielding the weak and upholding order, asking nothing in return. [PAGE_BREAK]
                    Primary skill:
                    •Sworn protectors, Knights earn 25% more experience in defense.
                    
                    Perks:
                    •Hamstring: No man escapes retribution. Sword and axe strikes apply Slowness for 4 seconds. [PAGE_BREAK]
                    •Patronage: Your assuring presence puts Villagers at ease, granting you better deals.
                    
                    •Heavy Bolts: The Knight’s crossbow strikes with devastating force, dealing 1.45x more damage. [PAGE_BREAK]
                    •Rigid Arsenal: A Knight’s pride disdains the use of fragile bows or intricate tridents, relying rather on the brute force of their crossbow.
                    """;
            case ROGUE -> """
                    This brittle, yellowed note is filled with crude tallies. Turning it over reveals a sharp edge that cuts your hand — the blood quickly absorbed into its fibers. [PAGE_BREAK]
                    Primary skill:
                    •Thriving amidst violence, Rogues earn 25% more experience in slaying.
                    
                    Perks:
                    •Stealth: When sneaking, you become invisible with unimpeded speed. [PAGE_BREAK]
                    •Poison Strike: While invisible, you apply Wither II for 4 seconds with weapons.
                    
                    •Flash Powder: Breaks mob aggro in an 8-block radius (12s cooldown, costs 1 Glowstone Dust). [PAGE_BREAK]
                    
                    •Slim Physique: The gift of the shadow demands a price, reducing your maximum health by 40%.
                    """;
            case NOMAD -> """
                    The Nomad's scroll is aged and weathered, its edges worn from travel. Adorned with maps and enigmatic writings, it tells of distant lands. Stained by dust, it whispers secrets known only to the wind and stars. [PAGE_BREAK]
                    Primary skill:
                    •Self-reliant wanderers, Nomads earn 20% more experience in Excavation.
                    
                    Perks:
                    •Scavenger: Food saturation decays 25% slower, and you are immune to hunger effects. [PAGE_BREAK]
                    •Bottomless Bundle: Right-click with a Bundle to access your Ender Chest inventory.
                    
                    •Outsider: Villages view Nomads with suspicion, refusing to trade.
                    """;
            case FARMHAND -> """
                    The Farmer’s Almanac wafts around an aroma of dew-covered wheat fields as you open it. Its pages, rich with dirt-stained wisdom, weave the rhythms of the seasons and secrets passed down through generations.[PAGE_BREAK]
                    Primary skill:
                    •Skilled cultivators, Farmhands earn 25% more experience in farming.
                    
                    Perks:
                    •Rustic Temperament: A Farmhand cares little for the arcane, receiving 30% less experience in magic. [PAGE_BREAK]
                    •Nourished: A Farmhand's diligence ensures they will never starve, nor tire. Your hunger never drops below 7, ensuring you are always ready for the next day’s work.
                    """;
            case LUMBERJACK -> """
                    The Lumberjack’s journal bears scars of the forest, its leather binding cracked, and its pages sticky with sap. Each entry is concise yet impactful, resonating with the rhythm of axe strikes that carry with them whispers of ancient groves. [PAGE_BREAK]
                    •Primary skill:
                    •Unparalleled woodcutters, Lumberjacks earn 25% more experience in woodcutting.
                    
                    Perks:
                    •Strong Arms: The Lumberjack’s vigor lends incredible speed to your swings, granting increased attack speed. [PAGE_BREAK]
                    •Salvaged Bark: Mastery of woodworking allows you to gain 2 planks when you strip a log.
                    
                    •Brute: The axe is your tool of choice for both slaying and woodcutting.
                    """;
            case MINER -> """
                    The Miner’s diary is bound with rough leather and secured with solid iron buckles. The cracks on the cover are veined with shimmering dust and its pages are streaked with soot. It feels heavy, as though it carries the weight of mountains. [PAGE_BREAK]
                    •Primary skill: Mining (Starts at level 15 and gains +20% XP towards this skill).
                    
                    Perks:
                    •Safety Lamp: see in the dark when a torch/lantern is equipped in the off-hand. [PAGE_BREAK]
                    
                    •Blasting Expert: 20% less damage from explosions (Stacks with Blast Protection IV for 80%).
                    •Vertigo: 20% more fall damage.
                    """;
            case WIZARD -> """
                    The Wizard’s Grimoire glows faintly, its silvered runes shifting like liquid light. Its pages, filled with cryptic diagrams and ancient incantations, seem to pulse with life, waiting for those bold enough to unravel its mysteries. [PAGE_BREAK]
                    Primary skill:
                    •Scholars of the mystic arts, Wizards earn 25% more experience in magic. [PAGE_BREAK]
                    Perks:
                    •Incantation: Wizards casts spells based on their wand:
                    
                    -Stick: Heals self or targeted player.
                    
                    -Blaze Rod: Fires a damaging fireball.
                    
                    -Breeze Rod: Enables horizontal levitation. [PAGE_BREAK]
                    
                    •Frail Body: A Wizard’s dedication to magic weakens their frames, receiving 20% less experience in all other skills.
                    
                    •I Put on My Robe and Wizard Hat: Wizards can only wear Leather Armor, Elytra, and Turtle Helmets.
                    """;
            default -> "Unknown class or lore not yet written.";
        };

        // Split the lore text into chunks of 256 characters or fewer (Minecraft's page limit)
        return splitTextIntoPages(loreText);
    }

    // Helper method to split text into pages of a specified max length
    private static List<String> splitTextIntoPages(String text) {
        List<String> pages = new ArrayList<>();

        // Split based on the custom page break marker
        String[] splitByMarker = text.split("\\[PAGE_BREAK\\]");
        for (String section : splitByMarker) {
            // Further split sections that exceed the character limit
            while (section.length() > 256) {
                pages.add(section.substring(0, 256).trim());
                section = section.substring(256);
            }
            if (!section.isEmpty()) {
                pages.add(section.trim());
            }
        }

        return pages;
    }
}