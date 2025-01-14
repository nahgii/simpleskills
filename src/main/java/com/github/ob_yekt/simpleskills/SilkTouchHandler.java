package com.github.ob_yekt.simpleskills;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class SilkTouchHandler {

    // Checks if the player's equipped tool has the Silk Touch enchantment
    public static boolean hasSilkTouch(ServerPlayerEntity player) {
        // Get the tool equipped in the player's main hand
        ItemStack toolStack = player.getEquippedStack(EquipmentSlot.MAINHAND);

        // Loop through the enchantments on the tool and check for Silk Touch
        for (var enchantment : toolStack.getEnchantments().getEnchantments()) {
            if (enchantment.getIdAsString().equals("minecraft:silk_touch")) {
                return true; // Silk Touch is present
            }
        }
        return false; // No Silk Touch found
    }
}