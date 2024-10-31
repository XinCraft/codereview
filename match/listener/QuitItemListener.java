package net.xincraft.systems.match.listener;

import net.xincraft.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class QuitItemListener implements Listener {
    public static final ItemStack QUIT = (new ItemBuilder(Material.BED)).setName(ChatColor.RED + "Quit").toItemStack();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null) {
            if (item.isSimilar(QUIT)) {
                event.setCancelled(true);
                event.getPlayer().performCommand("quit");
            }
        }
    }
}
