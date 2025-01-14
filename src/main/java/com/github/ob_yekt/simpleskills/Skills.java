package com.github.ob_yekt.simpleskills;

public enum Skills {
    MINING("Mining"),
    WOODCUTTING("Woodcutting"),
    EXCAVATING("Excavating"),
    DEFENSE("Defense"),
    SLAYING("Slaying"),
    FARMING("Farming"),
    MAGIC("Magic");

    private final String displayName;

    Skills(String displayName) {
        this.displayName = displayName; // Keep the original casing
    }

    public String getDisplayName() {
        return displayName;
    }
}