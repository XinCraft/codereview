package net.xincraft.systems.match.listener;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.Objects;

public class InventoryListener implements Listener {
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getGameMode() == GameMode.CREATIVE)
            return;

        // todo: make this not gay
        if (event.getClickedInventory() != null) {
            if (Objects.equals(event.getClickedInventory().getTitle(), "Kit Layout")) {
                return;
            }
        }

        event.setCancelled(true);
    }
}
