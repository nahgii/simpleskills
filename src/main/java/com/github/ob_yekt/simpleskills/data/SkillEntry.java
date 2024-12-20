package com.github.ob_yekt.simpleskills.data;

public class SkillEntry {
    private final String playerUuid;
    private final String skillName;
    private final int level;

    public SkillEntry(String playerUuid, String skillName, int level) {
        this.playerUuid = playerUuid;
        this.skillName = skillName;
        this.level = level;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getSkillName() {
        return skillName;
    }

    public int getLevel() {
        return level;
    }
}