package net.xincraft.systems.match.listener;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpawnProtectionListener implements Listener {
    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        // if a player hits another player while they have damage resistance, remove it
        if ((event.getDamager() instanceof Player || event.getDamager() instanceof Arrow) && event.getEntity() instanceof Player) {
            Player player;

            if (event.getDamager() instanceof Player) {
                player = (Player) event.getDamager();
            } else {
                player = (Player) ((Arrow) event.getDamager()).getShooter();
            }

            for (PotionEffect effect : player.getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE) && effect.getAmplifier() == 9) {
                    player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    player.getInventory().setHelmet(null);
                    return;
                }
            }
        }

        if (event.getEntity() instanceof Player && (event.getDamager() instanceof Player || event.getDamager() instanceof Arrow)) {
            Player player = (Player) event.getEntity();

            // we could check if they are in a bridge match or game here, but it is unnecessary as
            // there's no other places where the player can have resistance 10 but may need
            // to be changed if implementing a survival mode or something similar.

            for (PotionEffect effect : player.getActivePotionEffects()) {
                // if level 10 damage resistance, the same potion effect applied during spawn protection, cancel hit
                if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE) && effect.getAmplifier() == 9) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
