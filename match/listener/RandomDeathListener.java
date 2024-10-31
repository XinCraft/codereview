package net.xincraft.systems.match.listener;

import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.BridgeMatch;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class RandomDeathListener implements Listener {
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        // return if the entity who was damaged isn't a player - to save time
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        Player player = (Player) event.getEntity();

        // player damaging player (resulting in death) is handled in KillDeathListener - save time and return here
        if (event instanceof EntityDamageByEntityEvent &&
                (((EntityDamageByEntityEvent) event).getDamager() instanceof Player ||
                        ((EntityDamageByEntityEvent) event).getDamager() instanceof Arrow)) {
            return;
        }

        // todo: spectating
        //if (manager.inOrWatchingMatch(player) && manager.getMatch(player).isSpectating(player)) {

        // if the damage won't cause death then ignore
        if (!(player.getHealth() - event.getFinalDamage() <= 0)) {
            return;
        }

        String worldName = player.getWorld().getName();
        if (BridgeUtils.isBridgeWorld(worldName)) {
            BridgeMatch match = BridgeUtils.getBridgeMatch(worldName);
            if (match == null) {
                throw new RuntimeException("Could not get bridge match.");
            }

            match.onRandomDeath(player);
            event.setCancelled(true);
        }
    }
}
