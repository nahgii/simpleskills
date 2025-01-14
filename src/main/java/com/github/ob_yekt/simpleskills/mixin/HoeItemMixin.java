package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.XPManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public class HoeItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void checkToolAndSkillRequirement(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!context.getWorld().isClient() && context.getPlayer() instanceof ServerPlayerEntity player) {
            String toolName = context.getStack().getItem().toString();
            SkillRequirement requirement = RequirementLoader.getToolRequirement(toolName);

            if (requirement != null && requirement.getSkill().equalsIgnoreCase(Skills.FARMING.name())) {
                int playerLevel = XPManager.getSkillLevel(player.getUuidAsString(), Skills.FARMING);

                if (playerLevel < requirement.getLevel()) {
                    player.sendMessage(
                            Text.of("§6[simpleskills]§f You need " +
                                    Skills.FARMING.getDisplayName() + " level " +
                                    requirement.getLevel() + " to use this tool!"),
                            true
                    );
                    cir.setReturnValue(ActionResult.FAIL);
                    cir.cancel();
                }
            }
        }
    }

    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void onUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!context.getWorld().isClient() && context.getPlayer() instanceof ServerPlayerEntity player) {
            if (cir.getReturnValue() == ActionResult.SUCCESS) {
                int baseXP = ConfigLoader.getBaseXP(Skills.FARMING);
                int farmingXP = (int) (baseXP * 0.10);
                XPManager.addXPWithNotification(player, Skills.FARMING, farmingXP);
            }
        }
    }
}