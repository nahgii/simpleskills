package com.github.ob_yekt.simpleskills.simpleclasses;

public enum PlayerClass {
    PEASANT("Peasant", "NONE", 0, 1.0),
    KNIGHT("Knight", "DEFENSE", 15, 1.2),
    ROGUE("Rogue", "SLAYING", 15, 1.2),
    NOMAD("Nomad", "EXCAVATING", 15, 1.2),
    FARMHAND("Farmhand", "FARMING", 15, 1.2),
    LUMBERJACK("Lumberjack", "WOODCUTTING", 15, 1.2),
    MINER("Miner", "MINING", 15, 1.2),
    WIZARD("Wizard", "MAGIC", 15, 1.2);


    private final String displayName;
    private final String primarySkill;
    private final int startingLevel;
    private final double XPBonusMultiplier;


    PlayerClass(String displayName, String primarySkill, int startingLevel, double XPBonusMultiplier) {
        this.displayName = displayName;
        this.primarySkill = primarySkill;
        this.startingLevel = startingLevel;
        this.XPBonusMultiplier = XPBonusMultiplier;

    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrimarySkill() {
        return primarySkill;
    }

    public int getStartingLevel() {
        return startingLevel;
    }

    public double getXPBonusMultiplier() {
        return XPBonusMultiplier;
    }
}