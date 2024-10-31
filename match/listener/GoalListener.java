package net.xincraft.systems.match.listener;

import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.BridgeMatch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public class GoalListener implements Listener {
    @EventHandler
    public void onPlayerPortal(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        String worldName = player.getWorld().getName();
        if (BridgeUtils.isBridgeWorld(worldName)) {
            BridgeMatch match = BridgeUtils.getBridgeMatch(worldName);

            if (match == null) {
                throw new RuntimeException("Could not get bridge match: " + worldName);
            }

            match.handleGoal(player, event.getLocation());
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        event.setCancelled(true);
    }
}