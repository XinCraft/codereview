package net.xincraft.systems.match.countdown;

import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.match.BridgeMatch;
import net.xincraft.util.Countdown;
import org.bukkit.Sound;

public class MatchFinishCountdown extends Countdown {

    private final BridgeMatch match;

    public MatchFinishCountdown(BridgeMatch match) {
        super(5, 20);
        this.match = match;
        start();
    }

    public void run(int count) {
        //Match.this.getArrowCooldowns().values().forEach(BukkitRunnable::cancel);
        // if the players leave themselves, end the match early
        // todo: maybe just do playerleaveevent?
        if (match.getWorld().getPlayers().isEmpty()) {
            this.end();
            this.cancel();
        } else {
            match.getWorld().getPlayers().forEach((player) -> player.playSound(player.getLocation(), Sound.FIREWORK_LAUNCH, 100, 1));
        }
    }

    public void end() {
        XinCraftPlugin.get().getLogger().info("Ending match " + match.getId() + " due to match finish countdown finishing.");
        match.endMatch();
    }
}
