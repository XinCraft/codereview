package net.xincraft.systems.match.listener;

import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.BridgeMatch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BridgeMatch match = BridgeUtils.getBridgeMatch(event.getPlayer().getWorld().getName());

        if (match != null) {
            match.handleLeave(event.getPlayer());
        }

        XinCraftPlugin.get().getSidebarUpdater().updateLobby();
    }
}
