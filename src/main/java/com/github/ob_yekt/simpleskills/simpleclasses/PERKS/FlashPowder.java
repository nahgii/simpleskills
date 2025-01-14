package com.github.ob_yekt.simpleskills.simpleclasses.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class FlashPowder {
    private static final int COOLDOWN_SECONDS = 12;
    private static final double RADIUS = 8.0;

    public static void registerFlashPowderEvent() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            // Ignore client-side logic
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            // Ensure the player is a server player
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            // Check if player has the Flash Powder perk
            if (!PerkHandler.doesPlayerHavePerk(serverPlayer, "Flash Powder")) {
                return ActionResult.PASS;
            }

            // Retrieve the item in hand
            ItemStack itemStack = serverPlayer.getStackInHand(hand);
            Item heldItem = itemStack.getItem();

            // Ensure the item is Gunpowder or Blaze Powder
            if (!heldItem.equals(Items.GLOWSTONE_DUST)) { //!heldItem.equals(Items.GUNPOWDER))&& !heldItem.equals(Items.BLAZE_POWDER)) {
                return ActionResult.PASS;
            }

            // Ensure the player is invisible
            if (!serverPlayer.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                serverPlayer.sendMessage(
                        Text.literal("You must be invisible to use Flash Powder!")
                                .formatted(Formatting.RED),
                        true
                );
                return ActionResult.FAIL;
            }

            // Check the cooldown for the held item
            if (player.getItemCooldownManager().isCoolingDown(player.getStackInHand(Hand.MAIN_HAND))) {
                serverPlayer.sendMessage(
                        Text.literal("This item is on cooldown!"),
                        true
                );
                return ActionResult.FAIL;
            }

            // Apply cooldown to the held item
            player.getItemCooldownManager().set(player.getStackInHand(Hand.MAIN_HAND), COOLDOWN_SECONDS * 20);

            // Trigger the Flash Powder effect
            applyFlashPowderEffect(world, serverPlayer);

            // Consume one unit of the held item
            itemStack.decrement(1);

            // Indicate success
            return ActionResult.SUCCESS;
        });
    }

    private static void applyFlashPowderEffect(World world, ServerPlayerEntity player) {
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            // Define the area of effect
            Box box = new Box(player.getBlockPos()).expand(RADIUS);

            // Get entities near the player
            for (Entity entity : world.getOtherEntities(player, box, e -> e instanceof MobEntity)) {
                MobEntity mob = (MobEntity) entity;

                // Clear target if the mob is targeting the player
                LivingEntity currentTarget = mob.getTarget();
                if (currentTarget == player) {
                    mob.setTarget(null);
                }
            }

            // Play sound effect during activation
            world.playSound(
                    null,
                    player.getBlockPos(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE,
                    SoundCategory.PLAYERS,
                    0.8F,
                    0.5F
            );

            // Get the player's position
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            // Black smoke
            serverWorld.spawnParticles(
                    ParticleTypes.LARGE_SMOKE,
                    x, y, z,
                    60,
                    0.5,
                    1.2,
                    0.5,
                    0.5
            );

            // Grey smoke
            serverWorld.spawnParticles(
                    ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    x, y, z,
                    30,
                    0.5,
                    1.2,
                    0.5,
                    1.0
            );
        }
    }
}