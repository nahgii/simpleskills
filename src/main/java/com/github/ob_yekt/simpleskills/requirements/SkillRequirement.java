package com.github.ob_yekt.simpleskills.requirements;

public class SkillRequirement {
    private int level;                       // For level requirements
    private String skill;                    // Skill type (e.g., Mining, Defense, etc.)
    private Integer enchantmentLevel;        // For enchantment levels

    // Getter for level requirements
    public int getLevel() {
        return level;
    }

    // New getter for skill
    public String getSkill() {
        return skill;
    }
    public Integer getEnchantmentLevel() {
        return enchantmentLevel;
    }
}