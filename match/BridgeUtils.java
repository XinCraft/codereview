package net.xincraft.systems.match;

import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.match.BridgeMatch;

import java.io.File;

public class BridgeUtils {
    public static boolean isBridgeWorld(String worldName) {
        return worldName.startsWith("matches" + File.separator);
    }

    public static BridgeMatch getBridgeMatch(String worldName) {
        // if not a match world
        if (!worldName.startsWith("matches" + File.separator + "match-"))
            return null;

        // todo: is there a better way of getting the match id other than substring from the world name?
        return XinCraftPlugin.get().getMatchManager().getMatch(Integer.parseInt(worldName.substring(14)));
    }

    public static BridgeMatch tryGetBridgeMatch(String worldName) {
        if (BridgeUtils.isBridgeWorld(worldName)) {
            BridgeMatch match = BridgeUtils.getBridgeMatch(worldName);
            if (match == null) {
                throw new RuntimeException("Could not get bridge match.");
            }
            return match;
        }
        return null;
    }
}
