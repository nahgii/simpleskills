package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class HudRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static String cachedHudText = ""; // Stores the formatted HUD text

    public static void registerHud() {
        // Update HUD skill data every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && isSingleplayer()) {
                // Fetch skill data for the player from the database and format it
                cachedHudText = generateSkillText();
            }
        });

        // Render the HUD skills panel
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (client.player == null || !isSingleplayer()) return;

            int x = 10; // X position for rendering
            int y = 10; // Y position for rendering

            // Render each new line of the cached HUD text
            for (String line : cachedHudText.split("\n")) {
                drawContext.drawText(client.textRenderer, line, x, y, 0xFFFFFF, false);
                y += 12; // Move down after each line
            }
        });
    }

    /**
     * Generates the formatted skill text for display in the HUD.
     *
     * @return A multi-line string containing all formatted skill information.
     */
    private static String generateSkillText() {
        StringBuilder skillInfo = new StringBuilder();
        DatabaseManager db = DatabaseManager.getInstance();

        // Header bar
        skillInfo.append("§6⚔ Skills Overview ⚔\n");

        int totalLevels = 0; // To store the sum of all skill levels

        try (var rs = db.getPlayerSkills(client.player.getUuidAsString())) {
            while (rs.next()) {
                String skillName = rs.getString("skill");
                int currentLevel = rs.getInt("level");
                int currentXp = rs.getInt("xp");

                totalLevels += currentLevel; // Add current level to total

                if (currentLevel == XPManager.getMaxLevel()) {
                    // Add max-level skill
                    skillInfo.append(
                            String.format("⭐ %-15s Level 99 XP: %-10d%n", skillName, currentXp)
                    );
                } else {
                    // Calculate level progress
                    int xpForCurrentLevel = XPManager.getExperienceForLevel(currentLevel);
                    int xpToNextLevel = XPManager.getExperienceForLevel(currentLevel + 1) - xpForCurrentLevel;
                    int progressToNextLevel = currentXp - xpForCurrentLevel;

                    // Create progress bar
                    String progressBar = createProgressBar(progressToNextLevel, xpToNextLevel);

                    // Format skill data
                    skillInfo.append(
                            String.format("%-15s Level %-7d [%s] XP: %-5d/%-5d%n",
                                    skillName, currentLevel, progressBar, progressToNextLevel, xpToNextLevel
                            )
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("[SimpleSkills] Failed to fetch skills for player");
            return "Error fetching skills.";
        }

        // Add the "Total Levels" section
        skillInfo.append("---------------------------------------\n");
        skillInfo.append(String.format("§b§lTotal Level: §a%d\n", totalLevels)); // Bold + blue text for total levels

        // Normalize line endings to just '\n' to avoid stray 'CR' symbols
        return skillInfo.toString().replace("\r", "");
    }

    /**
     * Checks if the client is in singleplayer mode.
     *
     * @return true if in singleplayer mode, false otherwise.
     */
    private static boolean isSingleplayer() {
        return client.getCurrentServerEntry() == null && client.isIntegratedServerRunning();
    }

    /**
     * Creates a progress bar to represent progression toward the next level.
     *
     * @param progress Current XP progress for the next level
     * @param total    Total XP needed for the next level
     * @return A string representing the progress bar
     */
    private static String createProgressBar(int progress, int total) {
        int barLength = 10; // Length of the progress bar
        int filled = (int) ((double) progress / total * barLength);
        int empty = barLength - filled;

        return "█".repeat(filled) + "░".repeat(empty);
    }
}