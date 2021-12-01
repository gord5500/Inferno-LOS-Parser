package net.runelite.client.plugins.infernolosparser;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class LOSWaveOverlay extends OverlayPanel {

    private Client client;
    private LOSPlugin plugin;
    private LOSConfig config;

    @Inject
    private LOSWaveOverlay(final Client client, final LOSPlugin plugin,
                       final LOSConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Current Wave overlay"));

    }

    @Override
    public Dimension render(Graphics2D graphics)
    {

        // Don't show infobox if we're not in inferno
        if (!plugin.isInCaves()) {
            return null;
        }

        // Checks from the config if we should show the current wave or logout status
        if ((config.showCurrentWave() || config.showLogoutPressed()) && plugin.getOriginTile() != null && client.getGameState().equals(GameState.LOGGED_IN)) {

            int currentWave = plugin.getCurrentWave();
            String waveString = "Current Wave: " + (currentWave == -1 ? "?" : currentWave);

            if (config.showCurrentWave()) {

                panelComponent.getChildren().add(TitleComponent.builder()
                        .text(waveString)
                        .color(config.getWaveColor())
                        .build());
            }

            // Varbit 1548 is for logging out in inferno
            // 0 - no logout request
            // 1 - pause  requested
            // 2 - wave is paused
            if (client.getVarbitValue(1548) == 2 && config.showWavePaused()){
                panelComponent.getChildren().add(TitleComponent.builder()
                        .text("Paused")
                        .color(Color.GREEN)
                        .build());
            } else if (client.getVarbitValue(1548) == 1 && config.showLogoutPressed()){
                panelComponent.getChildren().add(TitleComponent.builder()
                        .text("Logout Queued")
                        .color(Color.GREEN)
                        .build());
            } else if (client.getVarbitValue(1548) == 0 && config.showNotLoggedOut()){
                panelComponent.getChildren().add(TitleComponent.builder()
                        .text("Not Pausing")
                        .color(Color.GREEN)
                        .build());
            }

            // Adds the pickup pots text
            if (plugin.isWaveFinished() && config.hoverGroundItems() && !plugin.getGroundItems().isEmpty()
            && client.getVarbitValue(1548) == 2) {

                panelComponent.getChildren().add(TitleComponent.builder()
                        .text("PICKUP POTIONS")
                        .color(Color.RED)
                        .build());
            }

            panelComponent.setPreferredSize(new Dimension(
                    graphics.getFontMetrics().stringWidth(waveString) + 10,
                    0));

            return super.render(graphics);
        }

        return null;
    }

}
