package com.github.ob_yekt.simpleskills;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class AttributeUpdater {

    public static void updatePlayerAttributes(ServerPlayerEntity player, Skills skill) {
        String playerUuid = player.getUuidAsString();

        switch (skill) {
            case SLAYING -> {
                EntityAttributeInstance attackDamage = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
                if (attackDamage != null) {
                    attackDamage.clearModifiers(); // Clear existing modifiers
                    int slayingLevel = XPManager.getSkillLevel(playerUuid, Skills.SLAYING);
                    if (slayingLevel >= 66) {
                        double bonusDamage = (slayingLevel - 65) * 0.01;
                        attackDamage.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:slaying_bonus"),
                                Math.min(bonusDamage, 0.33),
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ));
                    }
                }
            }
            case WOODCUTTING -> {
                EntityAttributeInstance blockRange = player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
                if (blockRange != null) {
                    blockRange.clearModifiers(); // Clear existing modifiers
                    int woodcuttingLevel = XPManager.getSkillLevel(playerUuid, Skills.WOODCUTTING);
                    if (woodcuttingLevel >= 66) {
                        // Updated logic: Maximum bonus of 7 at level 99
                        double bonusRange = 7.0 * (woodcuttingLevel - 65) / 34.0;
                        blockRange.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:woodcutting_bonus"),
                                Math.min(bonusRange, 7.0), // Cap to a maximum of 7.0
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ));
                    }
                }
            }
            case DEFENSE -> {
                EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.clearModifiers(); // Clear existing modifiers
                    int defenseLevel = XPManager.getSkillLevel(playerUuid, Skills.DEFENSE);
                    if (defenseLevel >= 66) {
                        // Updated logic: Add fixed 2 HP for specific milestone levels
                        int hearts = 0;
                        hearts++;
                        if (defenseLevel >= 77) hearts++;
                        if (defenseLevel >= 88) hearts++;
                        if (defenseLevel >= 99) hearts++;

                        // Each heart is 2 HP
                        double bonusHealth = hearts * 2.0; // E.g., 4 hearts = 8 HP
                        maxHealth.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:defense_bonus"),
                                bonusHealth,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ));
                    }
                }
            }
            case MINING -> {
                EntityAttributeInstance breakSpeed = player.getAttributeInstance(EntityAttributes.BLOCK_BREAK_SPEED);
                if (breakSpeed != null) {
                    breakSpeed.clearModifiers(); // Clear existing modifiers
                    int miningLevel = XPManager.getSkillLevel(playerUuid, Skills.MINING);
                    if (miningLevel >= 66) {
                        // Updated logic: Adjust multiplier for instant mining at level 99
                        double bonusSpeed = (miningLevel - 65) * 0.005; // Reduced multiplier
                        breakSpeed.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:mining_bonus"),
                                Math.min(bonusSpeed, 0.165), // Cap to the new maximum (0.165)
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ));
                    }
                }
            }
            case EXCAVATING -> {
                EntityAttributeInstance moveSpeed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                if (moveSpeed != null) {
                    moveSpeed.clearModifiers(); // Clear existing modifiers
                    int excavatingLevel = XPManager.getSkillLevel(playerUuid, Skills.EXCAVATING);
                    if (excavatingLevel >= 66) {
                        double bonusSpeed = (excavatingLevel - 65) * 0.01;
                        moveSpeed.addPersistentModifier(new EntityAttributeModifier(
                                Identifier.of("simpleskills:excavating_bonus"),
                                Math.min(bonusSpeed, 0.33),
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ));
                    }
                }
            }
            case MAGIC -> {
                EntityAttributeInstance fallDamageMultiplier = player.getAttributeInstance(EntityAttributes.FALL_DAMAGE_MULTIPLIER);
                if (fallDamageMultiplier != null) {
                    fallDamageMultiplier.clearModifiers(); // Clear previous modifiers
                    int magicLevel = XPManager.getSkillLevel(playerUuid, Skills.MAGIC);

                    double modifierValue;
                    if (magicLevel >= 99) {
                        modifierValue = -1.0; // Full immunity
                    } else if (magicLevel >= 86) {
                        modifierValue = -0.75; // 75% reduction
                    } else if (magicLevel >= 76) {
                        modifierValue = -0.5; // 50% reduction
                    } else if (magicLevel >= 66) {
                        modifierValue = -0.25; // 25% reduction
                    } else {
                        return; // No reduction below level 66
                    }

                    fallDamageMultiplier.addPersistentModifier(new EntityAttributeModifier(
                            Identifier.of("simpleskills:magic_fall_damage_reduction"),
                            modifierValue,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ));
                }
            }
        }
    }
}