package com.github.ob_yekt.simpleskills.simpleclasses;

import java.util.List;
import java.util.Map;

public class ClassMapping {
    private static final Map<String, List<String>> CLASS_TO_PERKS = Map.of(
            "PEASANT", List.of(),
            "FARMHAND", List.of("Fortitude", "Rustic Temperament"),
            "NOMAD", List.of("Scavenger", "Bottomless Bundle", "Exile"),
            "LUMBERJACK", List.of("Strong Arms", "Salvaged Bark", "Brute"),
            "MINER", List.of("Safety Lamp", "Blasting Expert", "Vertigo"),
            "WIZARD", List.of( "Incantation", "Frail Body", "I Put on My Robe and Wizard Hat"),
            "KNIGHT", List.of("Hamstring", "Rigid Arsenal", "Heavy Bolts", "Patronage"),
            "ROGUE", List.of("Stealth", "Slim Physique", "Poison Strike", "Flash Powder")
    );

    public static List<String> getPerksForClass(String className) {
        return CLASS_TO_PERKS.getOrDefault(className, List.of());
    }
}