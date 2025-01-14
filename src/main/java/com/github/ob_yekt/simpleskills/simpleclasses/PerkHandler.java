package com.github.ob_yekt.simpleskills.simpleclasses;

import com.github.ob_yekt.simpleskills.simpleclasses.PERKS.BottomlessBundle;
import com.github.ob_yekt.simpleskills.PlayerEventHandlers;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.data.DatabaseManager;

import com.github.ob_yekt.simpleskills.simpleclasses.PERKS.*;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import java.util.*;

public class PerkHandler {

    public static void registerPerkHandlers() {
        // Register a callback for when a player attacks an entity
        AttackEntityCallback.EVENT.register((player, world, hand, target, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                // Get the player's class from the database
                String playerClass = DatabaseManager.getInstance().getPlayerClass(serverPlayer.getUuidAsString());

                if (playerClass != null) {
                    // Retrieve all perks for the player's class
                    ClassMapping.getPerksForClass(playerClass).forEach(perkName -> {
                        var perk = PerkHandler.getPerk(perkName); // Get perk by name
                        if (perk != null) {
                            perk.onAttack(serverPlayer, target); // Run the perk-specific attack logic
                        }
                    });
                }
            }
            return ActionResult.PASS;
        });
    }


    // Map of all registered perks by name
    private static final Map<String, Perk> PERKS = new HashMap<>();

    static {
        // Register perks here

        /// STEALTH
        registerPerk("Stealth", new Perk() {
            private static final double SNEAK_SPEED_BONUS = 0.7; // 70% bonus to sneaking speed, no need for swift sneak pants

            @Override
            public void onApply(ServerPlayerEntity player) {
                EntityAttributeInstance sneakSpeedAttribute = player.getAttributeInstance(EntityAttributes.SNEAKING_SPEED);
                if (sneakSpeedAttribute != null) {
                    // Clear any existing modifiers first
                    sneakSpeedAttribute.clearModifiers();

                    // Add sneaking speed bonus
                    sneakSpeedAttribute.addPersistentModifier(new EntityAttributeModifier(Identifier.of("simpleskills",
                            "stealth_sneak_speed"), SNEAK_SPEED_BONUS, EntityAttributeModifier.Operation.ADD_VALUE));
                }
            }

            @Override
            public void onSneakChange(ServerPlayerEntity player, boolean sneaking) {
                if (sneaking) {
                    // Add invisibility effect while sneaking
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0,
                            true, false, false));
                } else {
                    // Remove invisibility effect while not sneaking
                    player.removeStatusEffect(StatusEffects.INVISIBILITY);
                }
            }

            @Override
            public void onRemove(ServerPlayerEntity player) {
                EntityAttributeInstance sneakSpeedAttribute = player.getAttributeInstance(EntityAttributes.SNEAKING_SPEED);
                if (sneakSpeedAttribute != null) {
                    // Clear all modifiers
                    sneakSpeedAttribute.clearModifiers();
                }
                player.removeStatusEffect(StatusEffects.INVISIBILITY);
            }
        });

        /// POISON STRIKE
        registerPerk("Poison Strike", new Perk() {
            private static final double DAMAGE_THRESHOLD = 4.0;
            private static final int WITHER_DURATION = 4;     // DURATION IN SECONDS
            private static final int WITHER_LEVEL = 1;        // POWER OF EFFECT (0 = I, 1 = II, 2 = III..)

            @Override
            public void onAttack(ServerPlayerEntity player, Entity target) {
                if (target instanceof LivingEntity livingEntity && player.isSneaking()) {

                    // Check if the effect is already applied
                    if (livingEntity.hasStatusEffect(StatusEffects.WITHER)) {
                        return;
                    }

                    double attackDamage = Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE)).getValue();
                    if (attackDamage >= DAMAGE_THRESHOLD && PlayerEventHandlers.isWeaponAllowed(player)) {
                        var heldItem = player.getMainHandStack().getItem();
                        if (isSword(heldItem) || isAxe(heldItem)) {
                            // Apply Wither effect to the target
                            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, WITHER_DURATION * 20, WITHER_LEVEL)); // 4 seconds (4dmg/2hearts)

                            // Play a sound for poison
                            player.getWorld().playSound(
                                    null,                           // Target all nearby players to hear it
                                    player.getBlockPos(),                  // Sound plays at the player's position
                                    SoundEvents.ITEM_GLOW_INK_SAC_USE,     // Example sound when enabling gravity
                                    SoundCategory.PLAYERS,                 // Player-related sound category
                                    1.3F,                                  // Volume
                                    0.8F                                   // Pitch
                            );

                            // Obtain ServerWorld
                            ServerWorld serverWorld = (ServerWorld) player.getWorld(); // Cast to ServerWorld

                            // Get target's position
                            double x = target.getX();
                            double y = target.getY();
                            double z = target.getZ();

                            serverWorld.spawnParticles(
                                    ParticleTypes.TOTEM_OF_UNDYING, // Particle type
                                    x, y, z,
                                    80,            // Particle count (1 particle per iteration)
                                    0.4,                 // X spread (optional fine-tuning)
                                    1.5,                 // Y spread
                                    0.4,                 // Z spread
                                    0.1                  // Particle speed multiplier
                            );
                        }
                    }
                }
            }
        });

        /// FLASH POWDER
        registerPerk("Flash Powder", new Perk() {
            @Override
            public void onApply(ServerPlayerEntity player) {
                // Flash Powder logic handled in FlashPowder
                FlashPowder.registerFlashPowderEvent();
            }
        });

        /// SLIM PHYSIQUE
        registerPerk("Slim Physique", new Perk() {
            private static final double HEALTH_REDUCTION_PERCENTAGE = -0.4; // Reduces max health by 40%

            @Override
            public void onApply(ServerPlayerEntity player) {
                EntityAttributeInstance maxHealthAttribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (maxHealthAttribute != null) {
                    // Clear any existing modifiers first
                    maxHealthAttribute.clearModifiers();

                    // Add a modifier to scale the base max health down by 40%
                    maxHealthAttribute.addPersistentModifier(new EntityAttributeModifier(Identifier.of("simpleskills",
                            "slim_physique_health_reduction"), HEALTH_REDUCTION_PERCENTAGE, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

            @Override
            public void onRemove(ServerPlayerEntity player) {
                EntityAttributeInstance maxHealthAttribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (maxHealthAttribute != null) {
                    // Clear all modifiers
                    maxHealthAttribute.clearModifiers();
                }
            }
        });

        /// HAMSTRING
        registerPerk("Hamstring", new Perk() {
            private static final double DAMAGE_THRESHOLD = 4.0;
            private static final int HAMSTRING_DURATION = 4;     // DURATION IN SECONDS
            private static final int HAMSTRING_LEVEL = 1;        // POWER OF EFFECT (0 = I, 1 = II, 2 = III..)

            @Override
            public void onAttack(ServerPlayerEntity player, Entity target) {
                if (target instanceof LivingEntity livingEntity) {

                    // Check if the effect is already applied
                    if (livingEntity.hasStatusEffect(StatusEffects.SLOWNESS)) {
                        return;
                    }

                    double attackDamage = Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE)).getValue();
                    if (attackDamage >= DAMAGE_THRESHOLD && PlayerEventHandlers.isWeaponAllowed(player)) {
                        var heldItem = player.getMainHandStack().getItem();
                        if (isSword(heldItem) || isAxe(heldItem)) {
                            // Apply slowness effect
                            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, HAMSTRING_DURATION * 20, HAMSTRING_LEVEL)); // 10 seconds

                            // Obtain ServerWorld
                            ServerWorld serverWorld = (ServerWorld) player.getWorld(); // Cast to ServerWorld

                            // Get target's position
                            double x = target.getX();
                            double y = target.getY();
                            double z = target.getZ();

                            // Spawn metallic particles

                            serverWorld.spawnParticles(
                                    ParticleTypes.WAX_ON, // Particle type
                                    x, y, z,
                                    30,             // Count (1 particle per iteration)
                                    0.5,                 // X spread (optional fine-tuning)
                                    1.1,                 // Y spread
                                    0.5,                 // Z spread
                                    0.4                  // Particle speed multiplier
                            );
                        }
                    }
                }
            }
        });

        /// HEAVY BOLTS
        registerPerk("Heavy Bolts", new Perk() {
            // Logic in HeavyBoltsMixin
        });

        /// RIGID ARSENAL
        registerPerk("Rigid Arsenal", new Perk() {
            // Logic in RigidArsenalMixin
        });

        ///  PATRONAGE
        registerPerk("Patronage", new Perk() {
            // Logic in DefendersDiscountMixin
        });

        /// RUSTIC TEMPERAMENT
        registerPerk("Rustic Temperament", new Perk() {
            private static final double MAGIC_XP_MULTIPLIER = 0.7;

            @Override
            public double modifyXP(Skills skill, double baseXP) {
                if (skill == Skills.MAGIC) {
                    return baseXP * MAGIC_XP_MULTIPLIER;
                }
                return baseXP;
            }
        });

        /// FORTITUDE
        registerPerk("Fortitude", new Perk() {
            // Logic is handled in HeartyMixin
        });

        /// EXILE
        registerPerk("Exile", new Perk() {
            @Override
            public void onApply(ServerPlayerEntity player) {
                // Logic handled in Exile.class
                Exile.registerOutsider();
            }
        });

        /// SCAVENGER
        registerPerk("Scavenger", new Perk() {
            // Logic is handled in ScavengerExhaustionMixin & ScavengerStatusEffectMixin
        });

        /// BOTTOMLESS BUNDLE
        registerPerk("Bottomless Bundle", new Perk() {
            // Logic is handled in BottomlessBundle
            @Override
            public void onApply(ServerPlayerEntity player) {
                BottomlessBundle.registerBottomlessBundle();
            }
        });

        /// STRONG ARMS
        registerPerk("Strong Arms", new Perk() {
            private static final double ATTACK_SPEED_BONUS = 0.5; // Adds to the base attack speed (4.0 -> 4.5)
            private static final Identifier STRONG_ARMS_ID = Identifier.of("simpleskills", "strong_arms_attack_speed");

            @Override
            public void onApply(ServerPlayerEntity player) {
                EntityAttributeInstance attackSpeedAttribute = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
                if (attackSpeedAttribute != null) {
                    // Clear modifiers to remove duplicates or conflicts
                    attackSpeedAttribute.clearModifiers();

                    // Add a persistent modifier to increase attack speed by a flat value of 1.0
                    attackSpeedAttribute.addPersistentModifier(new EntityAttributeModifier(STRONG_ARMS_ID, // Identifier for uniqueness
                            ATTACK_SPEED_BONUS, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE // Add flat value to the attack speed
                    ));
                }
            }

            @Override
            public void onRemove(ServerPlayerEntity player) {
                EntityAttributeInstance attackSpeedAttribute = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
                if (attackSpeedAttribute != null) {
                    // Clear all modifiers to ensure Strong Arms bonus is removed
                    attackSpeedAttribute.clearModifiers();
                }
            }
        });

        /// BRUTE
        registerPerk("Brute", new Perk() {
            // Logic is handled in PlayerEventHandlers & BruteSwordMixin
        });

        /// SALVAGED BARK
        registerPerk("Salvaged Bark", new Perk() {
            // Logic is handled in SalvagedBarkMixin
        });

        /// SAFETY LAMP
        registerPerk("Safety Lamp", new Perk() {
            @Override
            public void onApply(ServerPlayerEntity player) {
                // No immediate effect; handled in SafetyLampMixin
            }

            @Override
            public void onRemove(ServerPlayerEntity player) {
                // Remove the Night Vision effect when the perk is deactivated.
                player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        });

        /// BLASTING EXPERT
        registerPerk("Blasting Expert", new Perk() {
            // Logic is handled in BlastingExpertMixin
        });

        /// VERTIGO
        registerPerk("Vertigo", new Perk() {
            private static final double VERTIGO_INCREASE = 0.2; // Adds vulnerability

            @Override
            public void onApply(ServerPlayerEntity player) {
                EntityAttributeInstance fallDamageAttribute = player.getAttributeInstance(EntityAttributes.FALL_DAMAGE_MULTIPLIER);
                if (fallDamageAttribute != null) {
                    // Add Vertigo modifier
                    fallDamageAttribute.addPersistentModifier(new EntityAttributeModifier(Identifier.of("simpleskills",
                            "vertigo_fall_damage_increase"), VERTIGO_INCREASE, EntityAttributeModifier.Operation.ADD_VALUE // Adds flat vulnerability (30%)
                    ));
                }
            }

            @Override
            public void onRemove(ServerPlayerEntity player) {
                EntityAttributeInstance fallDamageAttribute = player.getAttributeInstance(EntityAttributes.FALL_DAMAGE_MULTIPLIER);
                if (fallDamageAttribute != null) {
                    // Remove Vertigo modifier
                    fallDamageAttribute.removeModifier(Identifier.of("simpleskills", "vertigo_fall_damage_increase"));
                }
            }
        });

        /// FRAIL BODY
        registerPerk("Frail Body", new Perk() {
            private static final double NON_MAGIC_XP_MULTIPLIER = 0.9;

            @Override
            public double modifyXP(Skills skill, double baseXP) {
                if (skill != Skills.MAGIC) {
                    return baseXP * NON_MAGIC_XP_MULTIPLIER;
                }
                return baseXP;
            }
        });

        /// I PUT ON MY ROBE AND WIZARD HAT
        registerPerk("I Put on My Robe and Wizard Hat", new Perk() {
            @Override
            public void onApply(ServerPlayerEntity player) {
                // I Put on My Robe and Wizard Hat logic handled in FlashPowder
                FlashPowder.registerFlashPowderEvent();
            }
        });

        /// INCANTATION
        registerPerk("Incantation", new Perk() {
            @Override
            public void onApply(ServerPlayerEntity player) {
                // Incantation logic handled in Incantation.class & IncantationFireball.class
                Incantation.registerIncantationEvent();
            }
        });
    }

    // Use the keys from the PERKS map to list all registered perks
    public static List<String> getAvailablePerks() {
        // Return perk names exactly as they appear in the map
        return new ArrayList<>(PerkHandler.PERKS.keySet());
    }

    // Method to register a new perk
    public static void registerPerk(String name, Perk perk) {
        PERKS.put(name, perk);
    }

    // Get a perk by name
    public static Perk getPerk(String name) {
        for (Map.Entry<String, Perk> entry : PERKS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) { // Perform case-insensitive comparison
                return entry.getValue();
            }
        }
        return null; // Return null if no perk is found
    }

    public static boolean doesPlayerHavePerk(ServerPlayerEntity player, String perkName) {
        String playerClass = DatabaseManager.getInstance().getPlayerClass(player.getUuidAsString());
        return playerClass != null && ClassMapping.getPerksForClass(playerClass).contains(perkName);
    }

    // Utility method: Check if item is a sword
    public static boolean isSword(net.minecraft.item.Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD || item == Items.IRON_SWORD
                || item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }

    // Utility method: Check if item is an axe
    public static boolean isAxe(net.minecraft.item.Item item) {
        return item == Items.WOODEN_AXE || item == Items.STONE_AXE || item == Items.IRON_AXE
                || item == Items.GOLDEN_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE;
    }

}
