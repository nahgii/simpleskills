package com.github.ob_yekt.simpleskills;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;

public class VillagerTrades {
    public static void registerCustomTrades() {

        TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 1,
                factories -> factories.add((entity, random) ->
                        new TradeOffer(
                                new TradedItem(Items.EMERALD, 3), // Cost: 3 emeralds
                                new ItemStack(Items.BLAZE_POWDER, 1), // Result: 1 blaze powder
                                16, // Max Uses
                                2, // Experience
                                0.075F // Price Multiplier
                        )));
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 1,
                factories -> factories.add((entity, random) ->
                        new TradeOffer(
                                new TradedItem(Items.EMERALD, 6), // Cost: 6 emeralds
                                new ItemStack(Items.NETHER_WART, 1), // Result: 1 nether wart
                                8, // Max Uses
                                2, // Experience
                                0.075F // Price Multiplier
                        )));
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 5,
                factories -> factories.add((entity, random) ->
                        new TradeOffer(
                                new TradedItem(Items.EMERALD, 4), // Cost: 3 emeralds
                                new ItemStack(Items.DRAGON_BREATH, 1), // Result: 1 dragon breath
                                16, // Max Uses
                                2, // Experience
                                0.075F // Price Multiplier
                        )));
    }
}