package com.github.ob_yekt.simpleskills.simpleclasses;

import com.github.ob_yekt.simpleskills.Skills;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public interface Perk {
    default void onSneakChange(ServerPlayerEntity player, boolean sneaking) {
    }

    default void onAttack(ServerPlayerEntity player, Entity target) {
    }

    default void onApply(ServerPlayerEntity player) {
    }

    default void onRemove(ServerPlayerEntity player) {
    }

    default double modifyXP(Skills skill, double baseXP) {
        return baseXP; // Default implementation does nothing
    }
}