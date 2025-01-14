package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.requirements.ConfigLoader;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;

public class VillagerTrades {
    public static void registerCustomTrades() {
        // Check if the villager trades feature is enabled
        if (ConfigLoader.isFeatureEnabled("villager_trades")) {
            // Register trades for CLERIC profession if the feature is enabled
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 3,
                    factories -> factories.add((entity, random) ->
                            new TradeOffer(
                                    new TradedItem(Items.EMERALD, 3), // Cost: 3 emeralds
                                    new ItemStack(Items.BLAZE_POWDER, 1), // Result: 1 blaze powder
                                    8, // Max Uses
                                    10, // Experience
                                    0.05F // Price Multiplier
                            )));
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 4,
                    factories -> factories.add((entity, random) ->
                            new TradeOffer(
                                    new TradedItem(Items.EMERALD, 6), // Cost: 6 emeralds
                                    new ItemStack(Items.NETHER_WART, 1), // Result: 1 nether wart
                                    12, // Max Uses
                                    15, // Experience
                                    0.05F // Price Multiplier
                            )));
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 5,
                    factories -> factories.add((entity, random) ->
                            new TradeOffer(
                                    new TradedItem(Items.EMERALD, 8), // Cost: 8 emeralds
                                    new ItemStack(Items.DRAGON_BREATH, 1), // Result: 1 dragon breath
                                    8, // Max Uses
                                    30, // Experience
                                    0.05F // Price Multiplier
                            )));
        }
    }
}
