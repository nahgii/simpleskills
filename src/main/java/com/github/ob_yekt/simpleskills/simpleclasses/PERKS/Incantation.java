package com.github.ob_yekt.simpleskills.simpleclasses.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;

import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Incantation {
    private static final int STICK_COOLDOWN = 20;            // COOLDOWN IN SECONDS
    private static final int BLAZE_ROD_COOLDOWN = 25;        // COOLDOWN IN SECONDS
    private static final int BREEZE_ROD_COOLDOWN = 2;        // COOLDOWN IN SECONDS
    private static final int REGENERATION_DURATION = 14;     // DURATION IN SECONDS
    private static final int REGENERATION_LEVEL = 1;         // POWER OF EFFECT (0 = I, 1 = II, 2 = III..)

    private static final Identifier GRAVITY_MODIFIER_ID = Identifier.of("simpleskills:gravity_toggle"); // Unique ID

    public static void registerIncantationEvent() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {
                Item heldItem = player.getStackInHand(hand).getItem();

                if (!isWizardItem(heldItem)) {
                    return ActionResult.PASS;
                }

                if (PerkHandler.doesPlayerHavePerk(serverPlayer, "Incantation")) {
                    castSpell(serverPlayer, heldItem);
                    return ActionResult.SUCCESS;
                } else {
                    player.sendMessage(Text.literal("§6[simpleskills]§f You need the 'Incantation' perk to cast spells!"), true);
                    return ActionResult.FAIL;
                }
            }

            return ActionResult.PASS;
        });
    }

    private static boolean isWizardItem(Item item) {
        return item.equals(Items.STICK) || item.equals(Items.BLAZE_ROD) || item.equals(Items.BREEZE_ROD);
    }

    private static void castSpell(ServerPlayerEntity player, Item heldItem) {
        if (player.getItemCooldownManager().isCoolingDown(player.getStackInHand(Hand.MAIN_HAND))) {
            player.sendMessage(Text.literal("§6[simpleskills]§f This spell isn't ready yet!"), true);
            return;
        }

        if (heldItem.equals(Items.STICK)) {
            applyRegeneration(player);
            player.getItemCooldownManager().set(player.getStackInHand(Hand.MAIN_HAND), STICK_COOLDOWN * 20);
        } else if (heldItem.equals(Items.BLAZE_ROD)) {
            castFireball(player);
            player.getItemCooldownManager().set(player.getStackInHand(Hand.MAIN_HAND), BLAZE_ROD_COOLDOWN * 20);
        } else if (heldItem.equals(Items.BREEZE_ROD)) {
            toggleGravity(player);
            player.getItemCooldownManager().set(player.getStackInHand(Hand.MAIN_HAND), BREEZE_ROD_COOLDOWN * 20);
        }
    }

    ///  STICK / REGENERATION
    private static void applyRegeneration(ServerPlayerEntity player) {
        LivingEntity target = getTargetEntity(player);
        if (target == null) {
            target = player;
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, REGENERATION_DURATION * 20, REGENERATION_LEVEL, true, true, true));

        spawnRegenerationParticles(target);
        target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0F, 1.4F);
        player.sendMessage(Text.literal("§6[simpleskills]§f You cast Regeneration on " + target.getName().getString() + "!"), true);
    }

    /**
     * Finds the friendly entity the player is looking at, within range.
     */
    private static LivingEntity getTargetEntity(ServerPlayerEntity player) {
        double maxDistance = 10.0; // Range for targeting
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d end = start.add(lookVec.multiply(maxDistance));

        Box box = player.getBoundingBox().stretch(lookVec.multiply(maxDistance)).expand(1.0, 1.0, 1.0);

        // Get all entities in the area
        List<Entity> entities = player.getWorld().getOtherEntities(player, box, entity -> {
            // Basic checks
            if (!(entity instanceof LivingEntity)) return false;
            if (entity instanceof Monster) return false;

            // Check if it's a friendly entity
            boolean isFriendly = entity instanceof PlayerEntity ||
                    entity instanceof PassiveEntity ||
                    entity instanceof GolemEntity ||
                    entity instanceof MerchantEntity ||
                    entity instanceof TameableEntity;

            if (!isFriendly) return false;

            // Only check distance
            Vec3d vec3d = entity.getPos().add(0, entity.getHeight() * 0.5, 0).subtract(start);
            double distance = vec3d.length();

            return distance <= maxDistance;
        });

        // Find the closest valid target
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : entities) {
            double distance = entity.squaredDistanceTo(player);
            if (distance < closestDistance) {
                // Check for line of sight
                BlockHitResult hitResult = player.getWorld().raycast(
                        new RaycastContext(
                                start,
                                entity.getPos().add(0, entity.getHeight() * 0.5, 0),
                                RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.NONE,
                                player
                        )
                );

                if (hitResult.getType() != HitResult.Type.MISS) {
                    continue; // Skip if there's a block in the way
                }

                closest = entity;
                closestDistance = distance;
            }
        }

        return closest instanceof LivingEntity ? (LivingEntity)closest : null;
    }

    private static void spawnRegenerationParticles(LivingEntity target) {
        if (target.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.HEART, target.getX(), target.getY(), target.getZ(), 20, 1, 1.4, 1, 0.1);
            serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getY(), target.getZ(), 70, 1.5, 1.5, 1.5, 0.1);
        }
    }

    private static void spawnGravityEnabledParticles(ServerPlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.WAX_OFF, player.getX(), player.getY(), player.getZ(), 70, 1.2, 1.4, 1.2, 0.1);
            serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY(), player.getZ(), 70, 1.5, 1.5, 1.5, 0.1);
        }
    }

    private static void spawnGravityDisabledParticles(ServerPlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.SCRAPE, player.getX(), player.getY(), player.getZ(), 70, 1.2, 1.2, 1.2, 0.1);
            serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY(), player.getZ(), 70, 1.5, 1.5, 1.5, 0.1);
        }
    }

    /// BLAZE ROD / FIREBALL
    private static void castFireball(ServerPlayerEntity player) {
        // Ensure we’re working on the server-side world
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        // Calculate direction for the fireball
        Vec3d lookDirection = player.getRotationVec(1.0F); // Get the player's looking direction
        IncantationFireball fireball = getIncantationFireball(player, serverWorld, lookDirection);

        // Spawn the fireball in the world
        serverWorld.spawnEntity(fireball);

        // Prevent fireball from setting fire explicitly
        fireball.setFireTicks(0);

        // Play a sound effect for casting the spell
        serverWorld.playSound(
                null, player.getBlockPos(),
                SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,         // Sound effect
                SoundCategory.PLAYERS, 1.0F, 1.6F      // Volume and pitch
        );

        // Notify the player they cast the spell
        player.sendMessage(Text.literal("§6[simpleskills]§f You cast Fireball!"), true);
    }

    private static @NotNull IncantationFireball getIncantationFireball(ServerPlayerEntity player, ServerWorld serverWorld, Vec3d lookDirection) {
        double velocityMultiplier = 1.1; // Adjust speed as needed

        // Create your custom fireball entity
        IncantationFireball fireball = new IncantationFireball(EntityType.SMALL_FIREBALL, serverWorld);

        // Set the fireball's position to launch from the player's point of view
        fireball.setPos(player.getX(), player.getEyeY(), player.getZ());

        // Set the initial velocity/direction of the fireball
        fireball.setVelocity(
                lookDirection.x * velocityMultiplier,  // X velocity
                lookDirection.y * velocityMultiplier,  // Y velocity
                lookDirection.z * velocityMultiplier   // Z velocity
        );
        return fireball;
    }

    /// BREEZE ROD / GRAVITY
    private static void toggleGravity(ServerPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        double VELOCITY_THRESHOLD = 0.08; // Small threshold to account for minor movements

        // Check if the player is stationary enough to toggle gravity
        if (Math.abs(velocity.x) < VELOCITY_THRESHOLD && Math.abs(velocity.y) < VELOCITY_THRESHOLD && Math.abs(velocity.z) < VELOCITY_THRESHOLD) {
            EntityAttributeInstance gravityAttribute = player.getAttributeInstance(EntityAttributes.GRAVITY);
            if (gravityAttribute != null) {
                if (gravityAttribute.hasModifier(GRAVITY_MODIFIER_ID)) {
                    // Re-enable gravity
                    gravityAttribute.removeModifier(GRAVITY_MODIFIER_ID);
                    player.sendMessage(Text.literal("§6[simpleskills]§f Gravity enabled!"), true);
                    spawnGravityEnabledParticles(player);
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0F, 1.4F);
                } else {
                    // Disable gravity and reset movement
                    player.setVelocity(0, 0, 0); // Stop all movement
                    player.fallDistance = 0.0F; // Reset fall distance
                    player.velocityModified = true; // Mark the velocity as modified to sync with the client

                    // Apply the gravity modifier
                    gravityAttribute.addPersistentModifier(new EntityAttributeModifier(GRAVITY_MODIFIER_ID, -0.08, EntityAttributeModifier.Operation.ADD_VALUE));

                    // Sync position and velocity with the client
                    Vec3d currentPos = player.getPos();
                    player.requestTeleport(currentPos.x, currentPos.y, currentPos.z);
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

                    // Notify the player and provide feedback
                    player.sendMessage(Text.literal("§6[simpleskills]§f Gravity disabled!"), true);
                    spawnGravityDisabledParticles(player);
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.0F, 1.4F);
                }
            } else {
                player.sendMessage(Text.literal("§6[simpleskills]§f Unable to toggle gravity! Attribute not found."), true);
            }
        } else {
            player.sendMessage(Text.literal("§6[simpleskills]§f You must be stationary to toggle gravity!"), true);
        }
    }
}
