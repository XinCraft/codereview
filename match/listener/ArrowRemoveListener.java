package net.xincraft.systems.match.listener;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class ArrowRemoveListener implements Listener {
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        // if the arrow hits the ground, remove it
        if (event.getEntity().getType() == EntityType.ARROW) {
            event.getEntity().remove();
        }
    }
}