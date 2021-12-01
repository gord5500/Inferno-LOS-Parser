package net.runelite.client.plugins.infernolosparser;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;

@Slf4j
@PluginDescriptor(
        name = "[N] Inferno LOS Parser",
        enabledByDefault = true,
        description = "Formats the wave for the LOS tool"
)
public class LOSPlugin extends Plugin {

    private final int PORTAL_ID = 30283;
    private WorldPoint originTile = null;
    private boolean checkNextTick = false;
    private int lastTick = 0;
    private int currentWave = -1;
    private boolean waveFinished = false;
    public int hitsplats = 0;

    private long timeLastTick = 0;

    private ArrayList<Tile> groundItems = new ArrayList<Tile>();

    @Inject
    public Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private LOSConfig config;

    @Inject
    private LOSOverlay overlay;

    @Inject
    private LOSWaveOverlay waveOverlay;

    private Connection connection;
    private Statement statement;

    private String pattern = "MM-dd-yyyy HH:mm:ss";
    private SimpleDateFormat format = new SimpleDateFormat(pattern);


    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {

        if (hitsplatApplied.getActor().getName().contains("Rocky")) {
            hitsplats += hitsplatApplied.getHitsplat().getAmount();
        }
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        overlayManager.add(waveOverlay);

    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        overlayManager.remove(waveOverlay);
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {

        String message = chatMessage.getMessage();

        // When we see the chat message with the wave number, the next tick the npcs spawn
        if (message.contains("Wave") && originTile != null && !message.contains("complete")) {

            try {

                message = message.replace("Wave: ", "");
                message = message.replace("<col=ef1020>", "");
                message = message.replace("</col>", "");

                currentWave = Integer.parseInt(message);

            } catch (NumberFormatException e) {

                log.info("Something weird with getting our current wave");
            }
            checkNextTick = true;
            waveFinished = false;
        } else if (message.contains("Wave") && originTile != null && message.contains("complete")) {

            waveFinished = true;
        }
        
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {

        // Just for debugging and to show the tick count for scouting
        if (!event.getGameState().equals(GameState.LOGGED_IN)) {

            if (client.getTickCount() == 0) {
                lastTick = -1;
            } else {
                lastTick = client.getTickCount();
            }

            waveFinished = false;
            groundItems.clear();
        } else if (event.getGameState().equals(GameState.LOGIN_SCREEN)) {
            timeLastTick = 0;
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {

        if (checkNextTick == true) {

            List<NPC> npcs = client.getNpcs();
            StringBuilder url = new StringBuilder("https://ifreedive-osrs.github.io/?");

            npcs.sort((npc1, npc2) -> {
                /*if (npc1.getCombatLevel() < npc2.getCombatLevel()) {
                    return 1;
                } else if (npc2.getCombatLevel() < npc1.getCombatLevel()) {
                    return -1;
                } else {
                    return 0;
                }

                 */

                if (npc1.getIndex() < npc2.getIndex()) {
                    return -1;
                } else if (npc2.getIndex() < npc1.getIndex()) {
                    return 1;
                } else {
                    return 0;
                }
            });
            // Loop through all the npcs. If they are ones we want, we find their location distance to
            // The origin which is the most northwest square in the area. That gives us the value to put in freedive's
            // tool
            for (NPC npc : npcs) {
                if (npc != null && npc.getName() != null) {
                    int losID = getNPCID(npc.getName());
                    if (losID != -1) {

                        WorldPoint npcTile = npc.getWorldLocation();
                        if (npcTile != null) {

                            int dx = npcTile.getX() - originTile.getX();
                            int dy = originTile.getY() - npcTile.getY();

                            String dxString = (dx < 10 ? "0" : "") + String.valueOf(dx);
                            String dyString = (dy < 10 ? "0" : "") + String.valueOf(dy);

                            // Add the npc to our list
                            url.append(dxString);
                            url.append(dyString);
                            url.append(losID);
                            url.append(".");
                        }
                    }
                }
            }
            log.info("Copied '" + url.toString() +"' to the clipboard.");

            StringSelection selection = new StringSelection(url.toString());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);

            checkNextTick = false;
        }
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event) {

        if (event.getItem() != null && isInCaves()) {
            if (!client.getItemDefinition(event.getItem().getId()).getName().contains("arrow")
                    && !client.getItemDefinition(event.getItem().getId()).getName().contains("dart")) {
                groundItems.add(event.getTile());
            }
        }
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned event) {

        if (event.getItem() != null && isInCaves()) {

            if (!client.getItemDefinition(event.getItem().getId()).getName().contains("arrow")
            && !client.getItemDefinition(event.getItem().getId()).getName().contains("dart")) {
                groundItems.remove(event.getTile());
            }
        }
    }

    public ArrayList<Tile> getGroundItems() {
        return groundItems;
    }

    public int getNPCID(String name) {

        // Freedive's website uses 1-7 for npc types. There are 2 different ones for blobs but
        // It's easier to just use one
        switch(name) {
            case "Jal-MejRah":
                return 1;
            case "Jal-Ak":
                return 2;
            case "Jal-ImKot":
                return 5;
            case "Jal-Xil":
                return 6;
            case "Jal-Zek":
                return 7;
            default:
                return -1;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned object) {

        // Find the portal which we use as our anchor. From that we figure out the origin tile
        // We're in an instance so good odds the value is not the same as last time
        if (object.getGameObject().getId() == PORTAL_ID) {

            WorldPoint portalTile = object.getGameObject().getWorldLocation();
            int x = portalTile.getX();
            int y = portalTile.getY();

            int shiftedX = x - 14;
            int shiftedY = y + 32;

             originTile = new WorldPoint(shiftedX, shiftedY, 0);
        }
    }

    public int getLastTick() {
        return lastTick;
    }

    public WorldPoint getOriginTile() {
        return originTile;
    }

    public boolean isInCaves() { return client.getVarbitValue(11878) == 1; }

    public int getCurrentWave() { return currentWave; }

    public boolean isWaveFinished() { return waveFinished; }

    @Provides
    LOSConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(LOSConfig.class);
    }

}
