package com.github.ob_yekt.simpleskills.mixin.MAGIC;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.XPManager;
import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

    @Shadow
    private Property levelCost;

    @Shadow
    public abstract int getLevelCost();

    // Add constructor to satisfy the abstract class requirement
    protected AnvilScreenHandlerMixin(int syncId, PlayerEntity player) {
        super(null, syncId, player.getInventory(), null, null);
    }

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void checkMagicRequirements(CallbackInfo ci) {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
        ItemStack outputStack = handler.getSlot(2).getStack();
        PlayerEntity player = this.player; // Now we can access player through the parent class

        if (!outputStack.isEmpty() && player instanceof ServerPlayerEntity serverPlayer) {
            // Only proceed if there's an actual output and the original cost isn't 0
            if (this.getLevelCost() > 0) {
                ServerWorld world = serverPlayer.getServerWorld();
                DynamicRegistryManager registryManager = world.getRegistryManager();
                int playerMAGICLevel = XPManager.getSkillLevel(serverPlayer.getUuidAsString(), Skills.MAGIC);
                boolean requirementsNotMet = false;

                for (RegistryEntry<Enchantment> enchantmentEntry : outputStack.getEnchantments().getEnchantments()) {
                    Enchantment enchantment = enchantmentEntry.value();
                    int enchantmentLevel = outputStack.getEnchantments().getLevel(enchantmentEntry);

                    Registry<Enchantment> enchantmentRegistry = registryManager.getOrThrow(RegistryKeys.ENCHANTMENT);
                    Identifier enchantmentId = enchantmentRegistry.getId(enchantment);

                    if (enchantmentId == null) continue;

                    SkillRequirement requirement = RequirementLoader.getMAGICRequirement(enchantmentId.toString());

                    if (requirement != null &&
                            enchantmentLevel == requirement.getEnchantmentLevel() &&
                            playerMAGICLevel < requirement.getLevel()) {

                        requirementsNotMet = true;
                        serverPlayer.sendMessage(Text.literal("§6[simpleskills]§f You need MAGIC level " +
                                requirement.getLevel() + " to apply " + enchantmentId.getPath() +
                                " level " + requirement.getEnchantmentLevel() + "!"), true);
                        break;
                    }
                }

                if (requirementsNotMet) {
                    // Set an extremely high level cost (9999)
                    this.levelCost.set(9999);
                }
            }
        }
    }
}