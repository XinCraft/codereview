package net.xincraft.systems.match.countdown;

import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.match.BridgeMatch;
import net.xincraft.ui.title.Title;
import net.xincraft.util.Countdown;
import net.xincraft.util.Utilities;
import org.bukkit.ChatColor;

public class StartingCountdown extends Countdown {
    private final BridgeMatch match;
    public StartingCountdown(BridgeMatch match) {
        super(5, 20);
        this.match = match;
        start();
    }

    public void run(int count) {
        XinCraftPlugin.get().getSidebarUpdater().update(match.getWorld());

        // if a player leaves during the countdown, cancel the countdown
        if (match.getPlayers().size() < match.getTeamHandler().getConfig().getMaxPlayers()) {
            match.getWorld().getPlayers().forEach((player) -> {
                player.sendMessage(ChatColor.RED + "We don't have enough players! Start cancelled.");
                new Title(ChatColor.RED + ChatColor.BOLD.toString() + "CANCELLED", 0, 20, 10).send(player);
            });

            // is this correctly disposing?
            this.cancel();
            match.setStartingCountdown(null);
        }
        // if game is full, start the countdown - we assume our code is good here and that too many people weren't able to join
        else {
            match.getWorld().getPlayers().forEach((player) -> {
                player.sendMessage(ChatColor.YELLOW + "The game starts in " + ChatColor.RED + count + ChatColor.YELLOW + " seconds!");

                Utilities.playTickSound(player);

                if (count > 3) {
                    new Title(ChatColor.YELLOW.toString() + count, 0, 100, 0).send(player);
                } else {
                    new Title(ChatColor.RED.toString() + count, 0, 100, 0).send(player);
                }
            });
        }
    }

    public void end() {
        XinCraftPlugin.get().getSidebarUpdater().update(match.getWorld());
        // is this correct disposing?
        match.setStartingCountdown(null);

        // we assume our code is good and that player counts aren't cooked
        match.startMatch();
    }
}
