package net.xincraft.systems.match.listener;

import net.xincraft.systems.match.match.BridgeMatch;
import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.MatchStatus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PregameHitListener implements Listener {
    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        // prevent players from hitting each other before and after matches
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // todo: reform
            String worldName = player.getWorld().getName();
            if (BridgeUtils.isBridgeWorld(worldName)) {
                BridgeMatch match = BridgeUtils.getBridgeMatch(worldName);
                if (match == null) {
                    throw new RuntimeException("Could not get bridge match.");
                }

                if (match.getStatus() == MatchStatus.PLAYING) {
                    return;
                }

                // only cancel hits in pregame and postgame
                event.setCancelled(true);
            }
        }
    }
}
