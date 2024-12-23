package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public class PotionEffectHandler {
    private static final int MAX_POTION_CAP = 60 * 60 * 20; // 60 minutes in ticks
    private static boolean processing = false;

    public static boolean isProcessing() {
        return processing;
    }

    public static void handleEffectApplication(ServerPlayerEntity player, StatusEffectInstance effect) {
        // First check if it's a tracked effect type at all
        if (!isTrackedEffectType(effect)) {
            return;
        }

        // Skip beacon (or similar) effects, which are ambient
        if (effect.isAmbient()) {
            return;
        }

        try {
            processing = true;
            // Grant XP for any level of potion
            grantMagicXP(player);

            // Only extend duration for level I effects
            if (effect.getAmplifier() == 0) {
                extendPotionEffect(player, effect);
            }
        } finally {
            processing = false;
        }
    }

    private static boolean isTrackedEffectType(StatusEffectInstance effect) {
        return effect.getEffectType() == StatusEffects.FIRE_RESISTANCE ||
                effect.getEffectType() == StatusEffects.STRENGTH ||
                effect.getEffectType() == StatusEffects.SPEED ||
                effect.getEffectType() == StatusEffects.NIGHT_VISION ||
                effect.getEffectType() == StatusEffects.INVISIBILITY ||
                effect.getEffectType() == StatusEffects.WATER_BREATHING ||
                effect.getEffectType() == StatusEffects.JUMP_BOOST;
    }

    private static void grantMagicXP(ServerPlayerEntity player) {
        XPManager.addXpWithNotification(player, Skills.MAGIC, (ConfigLoader.getBaseXp(Skills.DEFENSE))*50);
    }

    private static void extendPotionEffect(ServerPlayerEntity player, StatusEffectInstance newEffect) {
        int magicLevel = XPManager.getSkillLevel(player.getUuidAsString(), Skills.MAGIC);
        int maxDuration = calculateMaxPotionDuration(magicLevel);

        StatusEffectInstance existingEffect = player.getStatusEffect(newEffect.getEffectType());

        int totalDuration;
        if (existingEffect != null) {
            totalDuration = Math.min(existingEffect.getDuration() + newEffect.getDuration(), maxDuration);
        } else {
            totalDuration = Math.min(newEffect.getDuration(), maxDuration);
        }

        // Set processing flag to prevent infinite recursion
        player.removeStatusEffect(newEffect.getEffectType());
        player.addStatusEffect(new StatusEffectInstance(
                newEffect.getEffectType(),
                totalDuration,
                newEffect.getAmplifier(),
                newEffect.isAmbient(),
                newEffect.shouldShowParticles(),
                newEffect.shouldShowIcon()
        ));
    }

    private static int calculateMaxPotionDuration(int magicLevel) {
        int capInTicks = Math.min(magicLevel * 60 * 20, MAX_POTION_CAP);
        return Math.max(capInTicks, 1 * 60 * 20); // Minimum cap of 1 minute
    }
}