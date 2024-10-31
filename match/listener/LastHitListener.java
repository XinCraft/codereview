package net.xincraft.systems.match.listener;

import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.BridgeMatch;
import net.xincraft.util.Utilities;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Collections;

public class LastHitListener implements Listener {
    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player &&
                (event.getDamager() instanceof Player || event.getDamager() instanceof Arrow)) {
            Player player = (Player) event.getEntity();

            Player damager;

            if (event.getDamager() instanceof Arrow) {
                damager = (Player) ((Arrow) event.getDamager()).getShooter();
            } else {
                damager = (Player) event.getDamager();
            }

            if (player == damager) {
                return;
            }

            String worldName = player.getWorld().getName();
            BridgeMatch match = BridgeUtils.tryGetBridgeMatch(worldName);
            if (match == null) {
                return;
            }

            if (player.getHealth() - event.getFinalDamage() > 0) {
                match.getHitManager().setHit(player.getUniqueId(), damager.getUniqueId());

                // text above hotbar
                int darkRed = (int) Math.round((player.getHealth() - event.getFinalDamage()) / 2.0D);
                int red = (int) Math.round(event.getFinalDamage() / 2.0D);
                int gray = 10 - darkRed - red;
                Utilities.sendAction(damager, match.getTeamHandler().getTeam(player).getChatColour() + player.getName() + " &4" + String.join("", Collections.nCopies(darkRed, "❤")) + "&c" + String.join("", Collections.nCopies(red, "❤")) + "&7" + String.join("", Collections.nCopies(gray, "❤")), 1);
            }
        }
    }
}