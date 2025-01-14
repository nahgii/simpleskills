package com.github.ob_yekt.simpleskills.mixin.PERKS;

import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.entity.ItemEntity;

import java.util.Optional;

@Mixin(AxeItem.class)
public class SalvagedBarkMixin {

    @Inject(method = "tryStrip", at = @At("RETURN"))
    private void onStripLog(World world, BlockPos pos, PlayerEntity player, BlockState state,
                            CallbackInfoReturnable<Optional<BlockState>> cir) {
        // Only proceed if we're on the server side and have a valid player
        if (!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity)) {
            return;
        }

        Optional<BlockState> strippedState = cir.getReturnValue();

        // Check if stripping was successful and the player has the SalvagedBark perk
        if (strippedState.isPresent() &&
                PerkHandler.doesPlayerHavePerk((ServerPlayerEntity) player, "Salvaged Bark")) {

            Block plankBlock = getPlankFromLog(state.getBlock());
            if (plankBlock != Blocks.AIR) {
                // Determine plank count based on block type
                int plankCount = getPlankCount(state.getBlock());
                ItemStack plankStack = new ItemStack(plankBlock, plankCount);

                // Create the item entity with minimal vertical offset
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5;
                ItemEntity itemEntity = new ItemEntity(world, x, y, z, plankStack);
                itemEntity.setVelocity(0, 0.1, 0); // Minimal upward velocity
                world.spawnEntity(itemEntity);
            }
        }
    }

    @Unique
    private int getPlankCount(Block block) {
        Identifier blockId = Registries.BLOCK.getId(block);
        // Return 1 for bamboo blocks, 2 for all other valid blocks
        return blockId.toString().contains("bamboo") ? 1 : 2;
    }

    @Unique
    private Block getPlankFromLog(Block logBlock) {
        // Using the registry identifier is more reliable than toString()
        Identifier blockId = Registries.BLOCK.getId(logBlock);
        return switch (blockId.toString()) {
            case "minecraft:oak_log", "minecraft:stripped_oak_log", "minecraft:oak_wood",
                 "minecraft:stripped_oak_wood" -> Blocks.OAK_PLANKS;

            case "minecraft:spruce_log", "minecraft:stripped_spruce_log", "minecraft:spruce_wood",
                 "minecraft:stripped_spruce_wood" -> Blocks.SPRUCE_PLANKS;

            case "minecraft:birch_log", "minecraft:stripped_birch_log", "minecraft:birch_wood",
                 "minecraft:stripped_birch_wood" -> Blocks.BIRCH_PLANKS;

            case "minecraft:jungle_log", "minecraft:stripped_jungle_log", "minecraft:jungle_wood",
                 "minecraft:stripped_jungle_wood" -> Blocks.JUNGLE_PLANKS;

            case "minecraft:acacia_log", "minecraft:stripped_acacia_log", "minecraft:acacia_wood",
                 "minecraft:stripped_acacia_wood" -> Blocks.ACACIA_PLANKS;

            case "minecraft:dark_oak_log", "minecraft:stripped_dark_oak_log", "minecraft:dark_oak_wood",
                 "minecraft:stripped_dark_oak_wood" -> Blocks.DARK_OAK_PLANKS;

            case "minecraft:mangrove_log", "minecraft:stripped_mangrove_log", "minecraft:mangrove_wood",
                 "minecraft:stripped_mangrove_wood" -> Blocks.MANGROVE_PLANKS;

            case "minecraft:cherry_log", "minecraft:stripped_cherry_log", "minecraft:cherry_wood",
                 "minecraft:stripped_cherry_wood" -> Blocks.CHERRY_PLANKS;

            case "minecraft:pale_oak_log", "minecraft:stripped_pale_oak_log", "minecraft:pale_oak_wood",
                 "minecraft:stripped_pale_oak_wood" -> Blocks.PALE_OAK_PLANKS;

            case "minecraft:bamboo_block", "minecraft:stripped_bamboo_block" -> Blocks.BAMBOO_PLANKS;

            case "minecraft:crimson_stem", "minecraft:stripped_crimson_stem", "minecraft:crimson_hyphae",
                 "minecraft:stripped_crimson_hyphae" -> Blocks.CRIMSON_PLANKS;

            case "minecraft:warped_stem", "minecraft:stripped_warped_stem", "minecraft:warped_hyphae",
                 "minecraft:stripped_warped_hyphae" -> Blocks.WARPED_PLANKS;
            default -> Blocks.AIR; // Fallback for unsupported logs
        };
    }
}