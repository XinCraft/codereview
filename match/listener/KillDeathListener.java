package net.xincraft.systems.match.listener;

import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.BridgeMatch;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class KillDeathListener implements Listener {
    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        // if a player hit a player
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            Entity damager = event.getDamager();

            // if the damager is an arrow, set the damager to the shooter of the arrow
            if (event.getDamager() instanceof Arrow) {
                damager = (Player) ((Arrow) event.getDamager()).getShooter();
            }
            // if the entity is not an arrow nor a player, ignore
            else if (!(event.getDamager() instanceof Player)) {
                return;
            }

            String worldName = player.getWorld().getName();
            if (BridgeUtils.isBridgeWorld(worldName)) {
                BridgeMatch match = BridgeUtils.getBridgeMatch(worldName);
                if (match == null) {
                    throw new RuntimeException("Could not get bridge match.");
                }

                // and they will die
                if (player.getHealth() - event.getFinalDamage() <= 0) {
                    // cancel the death and handle it ourselves
                    event.setCancelled(true);
                    match.onKillDeath(player, (Player) damager);
                }
            }
            /*

            if (manager.inOrWatchingMatch(player)) {
                Match match = manager.getMatch(player);
                if (match.isPlaying(player)) {
                    Player damager;
                    if (event.getDamager() instanceof Arrow) {
                        damager = (Player)((Arrow)event.getDamager()).getShooter();

                        if (match.getTeam(player) == match.getTeam(damager)) {
                            damager.sendMessage(ChatColor.RED + "You cannot shoot your teammates...");
                            event.setCancelled(true);
                            return;
                        } else {
                            if (event.getFinalDamage()>4.6) event.setDamage(4.6);
                        }
                    } else {
                        damager = (Player)event.getDamager();
                    }

                    if (match.isPlaying(player) && match.isSpectating(damager)) {
                        event.setCancelled(true);
                        return;
                    }

                    if (match.getStatus() == MatchStatus.PLAYING && match.getTeam(player) != match.getTeam(damager)) {

                        for (PotionEffect pot : player.getActivePotionEffects()) {
                            if (pot.getType().getId() == 11 && pot.getAmplifier() == 9) {
                                event.setCancelled(true);
                                damager.sendMessage(ChatColor.RED + "This player is currently spawn protected...");
                                return;
                            }
                        }

                        for (PotionEffect pot : damager.getActivePotionEffects()) {
                            if (pot.getType().getId() == 11 && pot.getAmplifier() == 9) {
                                damager.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                            }
                        }

                        if (event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK) && match.gameModifiers.isOneShotOneKill()) {
                            manager.getMatch(player).onDeath(player);
                            event.setCancelled(true);
                            return;
                        }

                        if (match.gameModifiers.isAntikb()) {
                            Bukkit.getScheduler().runTaskLater(XinCraft.getInstance(), () -> {
                                event.getEntity().setVelocity(new Vector());
                            }, 1L);
                        }

                        if (match.gameModifiers.getHitDelay() != 20) {
                            ((Player)event.getDamager()).setMaximumNoDamageTicks(match.gameModifiers.getHitDelay());
                        }
*/
        }
    }
}
