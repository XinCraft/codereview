package net.xincraft.systems.match.listener;

import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.BridgeMatch;
import net.xincraft.systems.match.match.MatchStatus;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    private static final int MAX_BRIDGE_LENGTH = 20;

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        BridgeMatch match = BridgeUtils.tryGetBridgeMatch(player.getWorld().getName());
        if (match == null)
            return;

        // allow creative players to do what they want
        if (player.getGameMode() == GameMode.CREATIVE)
            return;

        // player shouldn't be able to break blocks if the match is not ongoing so we can save time here
        if (match.getStatus() != MatchStatus.PLAYING) {
            return;
        }

        Location block = event.getBlock().getLocation();

        // if the block is a part of the bridge we can skip the logic here to save time
        if (block.getBlockX() == 0 && block.getBlockZ() >= -MAX_BRIDGE_LENGTH && block.getBlockZ() <= MAX_BRIDGE_LENGTH) {
            return;
        }

        //  && !match.gameModifiers.isBreakEverything()) {
        if (!block.getBlock().hasMetadata("playerPlaced")) {
            event.getPlayer().sendMessage(ChatColor.RED + "You can only break blocks placed by a player!");
            event.setCancelled(true);
        }
    }
}

