package net.runelite.client.plugins.infernolosparser;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;

public class LOSOverlay extends Overlay {

    private Client client;
    private LOSPlugin plugin;
    private LOSConfig config;

    @Inject
    private LOSOverlay(final Client client, final LOSPlugin plugin,
                                   final LOSConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGHEST);

    }

    @Override
    public Dimension render(Graphics2D graphics) {

        if (!plugin.isInCaves()) {
            return null;
        }

        //OverlayUtil.renderActorOverlay(graphics, client.getLocalPlayer(), "" + (plugin.hitsplats), Color.RED);

        // Renders the tick counter above our head so we can scout well for debugging
        if (config.showTickCount() && plugin.getOriginTile() != null && client.getGameState().equals(GameState.LOGGED_IN)) {
            int count = client.getTickCount() - plugin.getLastTick();
            if (count < 15) {
                OverlayUtil.renderActorOverlay(graphics, client.getLocalPlayer(), "" + (count), Color.RED);
            }

        }


        if (config.hoverGroundItems() && plugin.isWaveFinished() && plugin.getOriginTile() != null && client.getGameState().equals(GameState.LOGGED_IN) && client.getVarbitValue(1548) == 2) {

            ArrayList<Tile> tiles = plugin.getGroundItems();
            if (tiles != null) {
                for (Tile tile : tiles) {
                    if (tile != null) {

                        OverlayUtil.renderPolygon(graphics, Perspective.getCanvasTilePoly(client, tile.getLocalLocation()), Color.GREEN);
                    }
                }
            }
        }
        return null;
    }
}
