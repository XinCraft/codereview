package net.xincraft.systems.match.listener;

import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.BridgeMatch;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;


public class PreventInteractListener implements Listener {
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block != null) {
            BridgeMatch match = BridgeUtils.tryGetBridgeMatch(player.getWorld().getName());
            if (match == null) {
            }

            // if the block is at
            //event.setCancelled(true);
        }
    }
}
