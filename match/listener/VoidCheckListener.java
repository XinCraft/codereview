package net.xincraft.systems.match.listener;

import net.xincraft.manager.LobbyWorld;
import net.xincraft.systems.match.BridgeUtils;
import net.xincraft.systems.match.match.BridgeMatch;
import net.xincraft.systems.match.match.MatchStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class VoidCheckListener implements Listener {
    private static final int VOID_LEVEL = 79;
    private static final int LOBBY_VOID_LEVEL = 0;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // only run the code if the player's y level has actually changed
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getWorld().equals(Bukkit.getWorld(LobbyWorld.SPAWN.getWorld().getUID()))) {
            if (player.getLocation().getY() < LOBBY_VOID_LEVEL) {
                player.setVelocity(player.getVelocity().zero());
                player.setFallDistance(0);
                player.teleport(LobbyWorld.SPAWN);
                return;
            }
            return;
        }


        String worldName = player.getWorld().getName();
        if (BridgeUtils.isBridgeWorld(worldName)) {
            BridgeMatch match = BridgeUtils.getBridgeMatch(worldName);
            if (match == null) {
                throw new RuntimeException("Could not get bridge match.");
            }

            double x = player.getLocation().getX();
            double z = player.getLocation().getZ();

            // if player outside the bounds of a 150 and 150 space, OR IN THE VOID, void death
            if (x < -150 || x > 150 || z < -150 || z > 150 || player.getLocation().getY() < VOID_LEVEL) {
                if (match.getStatus() != MatchStatus.PLAYING) {
                    // are both necessary?
                    player.setVelocity(player.getVelocity().zero());
                    player.setFallDistance(0);
                    // teleport player to the configured spawn position and handle failure
                    if (!player.teleport(match.getMap().getJoinSpawnPoint().toLocation(match.getWorld()))) {
                        throw new RuntimeException("Failed to teleport player");
                    }
                    return;
                }
                match.onVoidDeath(player);
            }

            /*

            for (BridgeTeam team : BridgeTeam.values()) {
                if (match.getGoalBlocks().get(team).stream().noneMatch(location -> match.inBlock(player, location)))
                    continue;

                // fix scoring after game ended
                // and scoring before cage opens
                if (match.getStatus() != MatchStatus.PLAYING && match.getCageCountdown() == null)
                    continue;

                match.getGoalCooldown().put(player, 20);
                if (team == match.getTeam(player)) {
                    Utilities.send(player, "&cYou just jumped through your own goal!");
                    Location loc = player.getLocation().clone();
                    loc.setY(match.getArena().getGoal(team).getY() - 3);
                    player.teleport(loc);
                } else if (match.getPlayers().size() > 0) {
                    match.addScore(match.getTeam(player));
                    match.getLocalStats(player).addGoal();
                    match.addGoal(player);
                    match.getPlayersAndSpectators().forEach(matchPlayer -> {
                        matchPlayer.sendMessage(Utilities.parse(match.getTeam(player).getChatColour() + player.getName() + " &7has scored!"));
                    });

                    if (match.getScore(match.getTeam(player)) != match.gameModifiers.getGoals()) {
                        match.reset(player);
                    } else {
                        HandlerList.unregisterAll(match.getMatchListener());
                        match.getPlayers(match.getTeam(player)).forEach(winningPlayer -> {
                            match.addWin(winningPlayer);
                            new Title(winningPlayer,
                                    "&a&lVICTORY!",
                                    "" + match.getTeam(player).getChatColour() + ChatColor.BOLD +
                                            match.getScore(match.getTeam(player)) + " &7&l- " +
                                            match.getTeam(player).getOpposite().getChatColour() + ChatColor.BOLD +
                                            match.getScore(match.getTeam(player).getOpposite()),
                                    20, 60, 20);
                        });
                        match.getPlayers(match.getTeam(player).getOpposite()).forEach(losingPlayer -> {
                            match.addLoss(losingPlayer);
                            new Title(losingPlayer,
                                    "&c&lDEFEAT",
                                    "" + match.getTeam(player).getChatColour() + ChatColor.BOLD +
                                            match.getScore(match.getTeam(player)) + " &7&l- " +
                                            match.getTeam(player).getOpposite().getChatColour() + ChatColor.BOLD +
                                            match.getScore(match.getTeam(player).getOpposite()),
                                    20, 60, 20);
                        });
                        match.getSpectators().forEach(spectatorPlayer -> {
                            new Title(spectatorPlayer,
                                    match.getTeam(player).getChatColour() + "&l" + match.getTeam(player).toString() + " WINS!",
                                    "" + match.getTeam(player).getChatColour() + ChatColor.BOLD +
                                            match.getScore(match.getTeam(player)) + " &7&l- " +
                                            match.getTeam(player).getOpposite().getChatColour() + ChatColor.BOLD +
                                            match.getScore(match.getTeam(player).getOpposite()),
                                    20, 60, 20);
                        });
                        match.end();
                        return;
                    }
                }
            }*/
        }
    }
}

