package net.xincraft.systems.match.listener;

import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.map.BlockLimitRule;
import net.xincraft.systems.match.match.BridgeMatch;
import net.xincraft.util.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class BlockPlaceListener implements Listener {
    private static final int SIDED_BRIDGE_LENGTH = 20;
    private static final int HEIGHT_LIMIT = 99;
    private static final int LOWEST_HEIGHT = 84;

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // height limit
        if (block.getY() > HEIGHT_LIMIT || block.getY() < LOWEST_HEIGHT) {
            Utilities.send(event.getPlayer(), ChatColor.RED + "You can't place blocks here!");
            event.setCancelled(true);
            return;
        }

        // sideways limit
        if (block.getX() > SIDED_BRIDGE_LENGTH || block.getX() < -SIDED_BRIDGE_LENGTH) {
            Utilities.send(event.getPlayer(), ChatColor.RED + "You can't place blocks here!");
            event.setCancelled(true);
            return;
        }

        BridgeMatch match = BridgeUtils.tryGetBridgeMatch(player.getWorld().getName());
        if (match != null) {

            int blockLimit = match.getMap().getBlockLimit();

            if (!match.getMap().getAdvancedBlockLimitRules().isEmpty()) {
                int deviation = Math.abs(block.getX());
                for (BlockLimitRule rule : match.getMap().getAdvancedBlockLimitRules()) {
                    if (deviation > rule.getDeviation()) {
                        blockLimit = rule.getBlockLimit();
                        break;
                    }
                }
            }

            if (block.getZ() > SIDED_BRIDGE_LENGTH + blockLimit || block.getZ() < -(SIDED_BRIDGE_LENGTH + blockLimit)) {
                Utilities.send(event.getPlayer(), ChatColor.RED + "You can't place blocks here!");
                event.setCancelled(true);
                return;
            }


            // set playerPlaced metadata for later
            event.getBlock().setMetadata("playerPlaced", new FixedMetadataValue(XinCraftPlugin.get(), true));
        }

            /*

                if (match.gameModifiers.isSuperboom() && event.getBlockPlaced().getType().equals(Material.TNT)) {
                    player.setVelocity(player.getLocation().getDirection().multiply(-0.83D).setY(1));
                    Arrow arrow = (Arrow)event.getBlock().getWorld().spawnEntity(event.getBlockPlaced().getLocation(), EntityType.ARROW);
                    Iterator var9 = arrow.getNearbyEntities(5.0D, 5.0D, 5.0D).iterator();

                    while(var9.hasNext()) {
                        Entity nearbyEntity = (Entity)var9.next();
                        Vector v = nearbyEntity.getLocation().getDirection().multiply(-0.83D).setY(1);
                        nearbyEntity.setVelocity(v);
                    }

                    arrow.remove();
                    event.setCancelled(true);
                    event.getBlock().getWorld().playEffect(event.getBlockPlaced().getLocation(), Effect.EXPLOSION_HUGE, 0, 1);
                    return;
                }

            }

        }*/
    }
}