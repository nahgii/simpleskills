package com.github.ob_yekt.simpleskills.simpleclasses.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class Exile {
    public static void registerOutsider() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Ignore client-side logic
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            // Ensure the player is a server player
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            // Check if player has the Exile perk
            if (!PerkHandler.doesPlayerHavePerk(serverPlayer, "Exile")) {
                return ActionResult.PASS;
            }

            // Check if the entity is a villager or wandering trader
            if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
                // Cancel the interaction
                player.sendMessage(Text.literal("§6[simpleskills]§f Villagers shun you due to your Exile perk, refusing to trade."), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
}