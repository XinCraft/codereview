package net.xincraft.systems.match.listener;

import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.BridgeMatch;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ArrowDamageMessage implements Listener {
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // cap arrow damage
        if (event.getDamager() instanceof Arrow) {
            // cobalt set it as 4.6
            // bridgescrims at 5.3
            // xincraft as of 21/10/24 set to 5

            Player hitPlayer = (Player) event.getEntity();
            Arrow arrow = (Arrow) event.getDamager();

            // remove bow crits to keep bow consistency
            arrow.setCritical(false);

            if (event.getFinalDamage() > 5) {
                event.setDamage(5);
            }

            if (arrow.getShooter() == hitPlayer) {
                return;
            }

            if (arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();
                double healthAfterHit = hitPlayer.getHealth() - event.getFinalDamage();
                healthAfterHit = Math.round(healthAfterHit * 10.0) / 10.0; // Round to one decimal place

                String worldName = hitPlayer.getWorld().getName();
                BridgeMatch match = BridgeUtils.tryGetBridgeMatch(worldName);
                if (match != null) {
                    ChatColor teamColor = match.getTeamHandler().getTeam(hitPlayer).getChatColour();
                    shooter.sendMessage(teamColor + hitPlayer.getName() + ChatColor.YELLOW + " is now on " + ChatColor.AQUA + healthAfterHit + ChatColor.YELLOW + " health.");
                }
            }
        }
    }
}