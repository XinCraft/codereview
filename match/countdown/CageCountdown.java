package net.xincraft.systems.match.countdown;

import net.xincraft.systems.match.cage.ScorerPlayer;
import net.xincraft.systems.match.match.CageHandler;
import net.xincraft.ui.title.Title;
import net.xincraft.util.Countdown;
import net.xincraft.util.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

// todo: only run one per team
public class CageCountdown extends Countdown {
    private final CageHandler handler;

    private final ScorerPlayer scorer;
    private final List<Player> players = new ArrayList<>();

    public CageCountdown(CageHandler handler, ScorerPlayer scorer) {
        super(5, 20);
        this.handler = handler;
        this.scorer = scorer;
        start();
    }

    public void run(int count) {
        String title = "";
        if (scorer != null) {
            for (Player player : players) {
                player.playSound(player.getLocation(), Sound.FIREWORK_LAUNCH, 100, 1);
            }
            title = scorer.getTeam().getChatColour() + scorer.getName() + " scored!";
        }

        for (Player player : players) {
            new Title(title,
                    ChatColor.GRAY + "Cages open in " +
                            ChatColor.GREEN + count + "s" +
                            ChatColor.GRAY + "...", 0, 100, 0).send(player);
            Utilities.playTickSound(player);
        }
    }

    public void end() {
        // undo the cage and return to normal
        handler.undoCages();

        for (Player player : players) {
            player.setGameMode(GameMode.SURVIVAL);

            Utilities.ding(player);
            new Title("", ChatColor.GREEN + "Fight!", 0, 20, 10).send(player);
        }
    }

    public void reset() {
        // undo the cage and return to normal
        handler.undoCages();
        cancel();
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public void addPlayer(Player player) {
        players.add(player);
    }
}
