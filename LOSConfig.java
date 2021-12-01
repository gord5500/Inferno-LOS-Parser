package net.runelite.client.plugins.infernolosparser;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("InfernoLOSParser")
public interface LOSConfig extends Config {

    @ConfigItem(
            position = 1,
            keyName = "showCount",
            name = "Show Tick Count",
            description = "Just for Naabe to make sure it works if you logout tick perfect"
    ) default boolean showTickCount() { return false; }

    @ConfigItem(
            position = 2,
            keyName = "showWave",
            name = "Show Current Wave",
            description = "Infobox for the current wave"
    ) default boolean showCurrentWave() { return false; }

    @Alpha
    @ConfigItem(
            position = 3,
            keyName = "waveColor",
            name = "Wave Text Color",
            description = "Color of the text"
    )
    default Color getWaveColor()
    {
        return Color.PINK;
    }

    @ConfigItem(
            position = 4,
            keyName = "showGroundItems",
            name = "Show ground items after wave",
            description = "Highlights items on the ground so you don't forget"
    ) default boolean hoverGroundItems() { return true; }

    @ConfigItem(
            position = 5,
            keyName = "showNotLoggedOut",
            name = "Show not logged out",
            description = "Shows a line of text to say you haven't tried to pause the wave yet"
    ) default boolean showNotLoggedOut() { return true; }

    @ConfigItem(
            position = 6,
            keyName = "showLogoutPressed",
            name = "Show logout confirmation",
            description = "Shows a line of text to say you've pressed the logout button and the wave " +
                    "will pause after completion"
    ) default boolean showLogoutPressed() { return true; }

    @ConfigItem(
            position = 7,
            keyName = "showWavePaused",
            name = "Show wave is paused",
            description = "Shows a line of text to say the wave is now paused"
    ) default boolean showWavePaused() { return true; }

    @ConfigItem(
            position = 8,
            keyName = "showPotionText",
            name = "Show text on potions",
            description = "Draws text on top of potions"
    ) default boolean showPotionText() { return true; }
}
