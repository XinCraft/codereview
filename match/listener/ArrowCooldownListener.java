package net.xincraft.systems.match.listener;

import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.CooldownManager;
import net.xincraft.systems.match.countdown.ArrowCountdown;
import net.xincraft.systems.match.match.BridgeMatch;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

import java.time.Duration;

public class ArrowCooldownListener implements Listener {
    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        BridgeMatch match = BridgeUtils.tryGetBridgeMatch(entity.getWorld().getName());
        if (match == null)
            return;

        Player player = (Player) event.getEntity();

        // save cooldowns so we can remove them when player respawns but this could be performant.
        match.getArrowManager().setCooldown(player.getUniqueId(), Duration.ofMillis(CooldownManager.ARROW_COOLDOWN));
        match.getArrowManager().getCountdowns().put(player.getUniqueId(), new ArrowCountdown(match, player));
    }
}