package com.github.ob_yekt.simpleskills.simpleclasses.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class BottomlessBundle {
    public static void registerBottomlessBundle() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            // Ignore client-side logic
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            // Ensure the player is a server player
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            // Get the item in hand
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof BundleItem)) {
                return ActionResult.PASS;
            }

            // Check if player has the Bottomless Bundle perk
            if (!PerkHandler.doesPlayerHavePerk(serverPlayer, "Bottomless Bundle")) {
                return ActionResult.PASS;
            }

            // Open "Bottomless Bundle" inventory
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, playerEntity) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, serverPlayer.getEnderChestInventory()),
                    Text.translatable("Bottomless Bundle")
            ));

            // Play custom bundle sound
            world.playSound(
                    null,
                    player.getBlockPos(),
                    SoundEvents.ITEM_BUNDLE_DROP_CONTENTS,
                    SoundCategory.PLAYERS,
                    1.0F,
                    0.5F
            );

            // Return SUCCESS to consume the event and prevent vanilla behavior
            return ActionResult.SUCCESS;
        });
    }
}