package net.xincraft.systems.match.listener;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ArrowHitListener implements Listener {
    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        // prevent bow boosting
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (!(event.getDamager() instanceof Arrow)) {
            return;
        }

        Arrow arrow = (Arrow) event.getDamager();
        if (arrow.getShooter() instanceof Player) {
            Player damager = (Player) arrow.getShooter();
            if (player.getUniqueId().equals(damager.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
