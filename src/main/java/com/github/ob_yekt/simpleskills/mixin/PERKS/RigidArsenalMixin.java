package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.TridentItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BowItem.class, TridentItem.class})
public class RigidArsenalMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        // Run only on the server
        if (!world.isClient()) {
            // Check if the player is a ServerPlayerEntity before casting
            if (player instanceof ServerPlayerEntity serverPlayer) {
                // Check if the player has the perk
                if (PerkHandler.doesPlayerHavePerk(serverPlayer, "Rigid Arsenal")) {
                    player.sendMessage(Text.literal("§6[simpleskills]§f Rigid Arsenal prohibits you from using this weapon."), true);
                    cir.setReturnValue(ActionResult.FAIL);
                    cir.cancel();
                }
            }
        }
    }
}