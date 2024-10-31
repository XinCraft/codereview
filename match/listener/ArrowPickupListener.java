package net.xincraft.systems.match.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class ArrowPickupListener implements Listener {
    @EventHandler
    public void onArrowPickup(PlayerPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.ARROW) {
            event.setCancelled(true);
        }
    }
}
