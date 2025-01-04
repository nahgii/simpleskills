package com.github.ob_yekt.simpleskills.mixin.MAGIC;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.XPManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentScreenHandler.class)
public class EnchantingScreenHandlerMixin {
    @Inject(method = "onButtonClick", at = @At("HEAD"))
    private void onButtonClick(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayerEntity) {
            EnchantmentScreenHandler handler = (EnchantmentScreenHandler)(Object)this;
            grantEnchantingXP((ServerPlayerEntity) player, handler, id);
        }
    }

    @Unique
    private void grantEnchantingXP(ServerPlayerEntity player, EnchantmentScreenHandler handler, int buttonId) {
        // Get the enchantment level for the selected option (buttonId is 0-2)
        int level = handler.enchantmentPower[buttonId];
        if (level > 0) {
            // Grant XP based on the enchantment level
            int xp = (ConfigLoader.getBaseXp(Skills.MAGIC)) * 10 * level;
            XPManager.addXpWithNotification(player, Skills.MAGIC, xp);
        }
    }
}