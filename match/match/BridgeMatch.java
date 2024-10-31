package net.xincraft.systems.match.match;

import lombok.Getter;
import lombok.Setter;
import net.xincraft.XinCraftPlugin;
import net.xincraft.database.data.stats.Stats;
import net.xincraft.database.data.stats.StatsType;
import net.xincraft.database.metric.MetricCollector;
import net.xincraft.systems.match.CooldownManager;
import net.xincraft.systems.match.cage.ScorerPlayer;
import net.xincraft.systems.match.countdown.MatchFinishCountdown;
import net.xincraft.systems.match.countdown.StartingCountdown;
import net.xincraft.systems.match.hits.HitManager;
import net.xincraft.systems.match.hits.LastHit;
import net.xincraft.systems.match.map.BridgeMap;
import net.xincraft.systems.match.runnable.MatchTimer;
import net.xincraft.systems.title.TitleUtils;
import net.xincraft.ui.nametag.BridgeMatchNametag;
import net.xincraft.ui.nametag.QueueingNametag;
import net.xincraft.ui.objective.ObjectiveWorld;
import net.xincraft.ui.sidebar.MatchSidebar;
import net.xincraft.ui.title.Title;
import net.xincraft.util.Countdown;
import org.apache.commons.lang.StringEscapeUtils;
import org.bson.Document;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criterias;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class BridgeMatch extends Match implements ObjectiveWorld {
    private static final int SPAWN_RESISTANCE_TICKS = 2 * 20;

    private final TeamHandler teamHandler;
    private final CageHandler cageHandler;

    private final Map<BridgeTeam, Integer> scores = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> goals = new HashMap<>();

    @Setter
    private Countdown startingCountdown;

    private final CooldownManager arrowManager = new CooldownManager();
    private final HitManager hitManager = new HitManager();
    private final Set<UUID> goalProcessors = new HashSet<>();

    public BridgeMatch(int id, BridgeMap map, TeamConfiguration teamConfiguration) {
        super(id, map, MatchTimer.FINAL_GAME_LENGTH_TICKS);
        this.teamHandler = new TeamHandler(teamConfiguration);
        this.cageHandler = new CageHandler(teamHandler);

        for (BridgeTeam team : BridgeTeam.values()) {
            // get location of where the cage will be placed
            Location location = map.getTeamSpawnpoints().get(team).toTeamSpawn(world, team);
            cageHandler.loadCache(team, location);
            // set the score to 0
            scores.put(team, 0);
        }
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(player);

        // magic naming
        new QueueingNametag().applyTo(world, player);
        player.setPlayerListName(ChatColor.MAGIC + player.getName());

        for (Player worldPlayer : world.getPlayers()) {
            // broadcast player joining
            worldPlayer.sendMessage(
                    ChatColor.GRAY.toString() + ChatColor.MAGIC + player.getName() +
                            ChatColor.YELLOW + " has joined (" +
                            ChatColor.AQUA + players.size() +
                            ChatColor.YELLOW + "/" +
                            ChatColor.AQUA + teamHandler.getConfig().getMaxPlayers() +
                            ChatColor.YELLOW + ")!");
        }

        // scoreboard handling
        XinCraftPlugin.get().getSidebarUpdater()
                .swapSidebar(player, new MatchSidebar(this, player));
        XinCraftPlugin.get().getSidebarUpdater().update(world);

        XinCraftPlugin.get().getSidebarUpdater().updateLobby();

        // check if the game is ready to start
        checkStart();
    }

    public void rejoinPlayer(Player player) {
        if (!limboPlayers.contains(player.getUniqueId())) {
            throw new RuntimeException("Trying to rejoin a player who isn't in limbo for that match.");
        } else {
            limboPlayers.remove(player.getUniqueId());
        }

        players.add(player.getUniqueId());

        // initialise player
        player.setAllowFlight(false);
        player.setFlying(false);

        // front-end
        // setup nametags for team colors
        new BridgeMatchNametag(this).applyTo(world, player);

        registerObjectives(player);

        // set tablist colors
        BridgeTeam team = teamHandler.getTeam(player);

        player.setPlayerListName(team.getChatColour() + player.getName());


        for (Player matchPlayer : world.getPlayers()) {
            // broadcast player joining
            matchPlayer.sendMessage(team.getChatColour() + player.getName() + ChatColor.GRAY + " has rejoined!");
        }

        // scoreboard handling
        XinCraftPlugin.get().getSidebarUpdater()
                .swapSidebar(player, new MatchSidebar(this, player));
        XinCraftPlugin.get().getSidebarUpdater().update(world);

        XinCraftPlugin.get().getSidebarUpdater().updateLobby();


        // ensure the rejoined player sees the titles shown during cage countdown
        if (cageHandler.playersCaged()) {
            cageHandler.addPlayer(player);
        }

        respawn(player);
    }

    public void addSpectator(Player player) {
        spectators.add(player.getUniqueId());

        // initialise player
        player.setGameMode(GameMode.SPECTATOR);
        player.getActivePotionEffects().forEach((effect) -> player.removePotionEffect(effect.getType()));

        // clear inventory and set quit item
        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        // teleport player to the configured spawn position and handle failure
        if (!player.teleport(map.getJoinSpawnPoint().toLocation(world))) {
            player.sendMessage(ChatColor.RED + "Failed to teleport you to the match, please rejoin.");
            XinCraftPlugin.get().getLobbyWorld().setup(player);
            throw new RuntimeException("Failed to teleport player");
        }

        registerObjectives(player);

        for (Player player_ : world.getPlayers()) {
            // broadcast player joining
            player_.sendMessage(
                    ChatColor.GRAY + player.getName() +
                            ChatColor.YELLOW + " has joined to spectate.");
        }

        // scoreboard handling
        XinCraftPlugin.get().getSidebarUpdater()
                .swapSidebar(player, new MatchSidebar(this, player));
        XinCraftPlugin.get().getSidebarUpdater().update(world);
    }

    private void checkStart() {
        // if there isn't enough players to start the match, return
        if (players.size() != teamHandler.getConfig().getMaxPlayers()) {
            return;
        }

        // run a countdown of 5 seconds running every 20 ticks (1 second)
        startingCountdown = new StartingCountdown(this);
    }

    public void startMatch() {
        status = MatchStatus.PLAYING;
        timer.start();

        teamHandler.setupTeams(worldPlayersOf(players), !saveStats);

        cageHandler.loadCages();

        for (Player player : worldPlayersOf(players)) {
            // setup nametags for team colors
            new BridgeMatchNametag(this).applyTo(world, player);

            registerObjectives(player);

            // set tablist colors
            player.setPlayerListName(teamHandler.getTeam(player).getChatColour() + player.getName());

            // set kills, goals to 0
            goals.put(player.getUniqueId(), 0);
            kills.put(player.getUniqueId(), 0);
        }

        // benchmark this:
        Instant startTime = Instant.now();

        // send player to cage
        cageHandler.cageTeams(map, world, null, (this::respawn));

        Duration timeTaken = Duration.between(startTime, Instant.now());
        XinCraftPlugin.get().getLogger().info("Generated cages for each team (took " + timeTaken.toMillis() + "ms)");
    }

    public boolean checkFinish() {
        XinCraftPlugin.get().getLogger().info("Checking if match " + id + " is ready to finish.");

        if (players.isEmpty()) {
            XinCraftPlugin.get().getLogger().info("Ending match " + id + " after checkFinish as players is empty.");
            endMatch();
            return false;
        }

        // if we are waiting for players, we never need to run an ending countdown if someone leaves.
        if (status == MatchStatus.QUEUEING) {
            return false;
        }


        // if any team has 0 players, finish the match
        for (BridgeTeam bridgeTeam : BridgeTeam.values()) {
            if (world.getPlayers().stream().filter(p ->
                    players.contains(p.getUniqueId())).noneMatch(p ->
                    teamHandler.getTeam(p.getUniqueId()).equals(bridgeTeam))) {

                finishMatch();
                return true;
            }
        }

        return false;
    }

    public void handleLeave(Player player) {
        XinCraftPlugin.get().getLogger().info("Handling leave for " + player.getName() + " in match " + id);

        if (spectators.contains(player.getUniqueId())) {
            world.getPlayers().forEach(player_ -> {
                player_.sendMessage(player.getName() + ChatColor.GRAY + " left the game.");
            });

            removeSpectator(player);
            return;
        }

        // if before or after game
        if (status.notOngoing()) {
            removePlayer(player);

            // if player leaves during starting countdown
            if (startingCountdown != null) {
                startingCountdown.cancel();
                startingCountdown = null;

                XinCraftPlugin.get().getSidebarUpdater().update(world);

                for (Player worldPlayer : world.getPlayers()) {
                    worldPlayer.sendMessage(ChatColor.RED + "We don't have enough players! Start cancelled.");
                    new Title(ChatColor.RED + ChatColor.BOLD.toString() + "CANCELLED", "", 0, 20, 10).send(worldPlayer);
                }
            } else {
                XinCraftPlugin.get().getSidebarUpdater().update(world);

                if (status != MatchStatus.ENDING) {
                    checkFinish();
                }
            }
        } else {
            BridgeTeam losingBridgeTeam = teamHandler.getTeam(player.getUniqueId());
            BridgeTeam winningBridgeTeam = losingBridgeTeam == BridgeTeam.RED ? BridgeTeam.BLUE : BridgeTeam.RED;

            // get opposite team

            world.getPlayers().forEach(player_ -> player_.sendMessage(losingBridgeTeam.getChatColour() + player.getName() + ChatColor.GRAY + " left the game."));

            limboPlayers.add(player.getUniqueId());

            removePlayer(player);

            if (status == MatchStatus.ENDING) {
                return;
            }

            XinCraftPlugin.get().getLogger().info("Running checkFinish inside handleLeave for " + player.getName());
            if (checkFinish()) {
                world.getPlayers().forEach(player_ -> {
                    new Title(winningBridgeTeam.getChatColour() + winningBridgeTeam.name() + " WINS!").send(player_);
                });

                if (this.saveStats) {
                    for (UUID playerId : players) {
                        Player player_ = Bukkit.getPlayer(playerId);

                        System.out.println("Player: " + player_.getName() + " Team: " + teamHandler.getTeam(playerId) + " Winning Team: " + winningBridgeTeam);
                        if (teamHandler.getTeam(playerId) == winningBridgeTeam) {
                            this.getGlobalStats(player_).addWin();
                            this.getOverallStats(player_).addWin();
                            TitleUtils.updateTitle(player_);
                        } else {
                            this.getGlobalStats(player_).addLoss();
                            this.getOverallStats(player_).addLoss();
                        }
                    }
                }
            }

            removePlayer(player);
        }
    }

    public void finishMatch() {
        status = MatchStatus.ENDING;

        timer.getCountdown().setPaused(true);

        cageHandler.finish();

        // teleport players to the map spawn point
        for (Player player : worldPlayersOf(players)) {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.teleport(map.getJoinSpawnPoint().toLocation(world));
        }

        new MatchFinishCountdown(this);
    }

    public void endMatch() {
        XinCraftPlugin.get().getLogger().info("Ending match " + id + " with players at size " + players.size());

        // collect match metrics before deleting its existence
        new MetricCollector(this).collect();

        world.getPlayers().forEach((player) -> {
            XinCraftPlugin.get().getLobbyWorld().setup(player);
            // if player, remove player, else if spectator, remove spectator
            if (players.contains(player.getUniqueId())) {
                removePlayer(player);
            } else if (spectators.contains(player.getUniqueId())) {
                removeSpectator(player);
            }
        });

        XinCraftPlugin.get().getMatchManager().removeMatch(BridgeMatch.this);
    }

    public void removePlayer(Player player) {
        System.out.println("Removing player " + player.getName() + " from match " + id);

        // if player is removed within a game, count as a death
        if (status == MatchStatus.PLAYING && saveStats) {
            this.getGlobalStats(player).addDeath();
            this.getOverallStats(player).addDeath();
        }

        // remove them from the hit manager
        if (hitManager.getHit(player.getUniqueId()) != null) {
            hitManager.removeHit(player.getUniqueId());
        }

        cageHandler.removePlayer(player);

        players.remove(player.getUniqueId());
    }

    public void removeSpectator(Player player) {
        spectators.remove(player.getUniqueId());
    }


    public void onVoidDeath(Player player) {
        respawn(player);
        player.playSound(player.getLocation(), Sound.HURT_FLESH, 1, 1);

        goalProcessors.remove(player.getUniqueId());

        if (this.saveStats) {
            this.getGlobalStats(player).addDeath();
            this.getOverallStats(player).addDeath();
        }

        if (hitManager.isHitRecent(player.getUniqueId())) {
            LastHit hit = hitManager.getHit(player.getUniqueId());

            Player killer = Bukkit.getPlayer(hit.getDamager());
            killer.playSound(killer.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);

            handleKill(killer);

            world.getPlayers().forEach((player_) -> player_.sendMessage(
                    teamHandler.getTeam(player).getChatColour() + player.getName() +
                            ChatColor.GRAY + " was knocked into the void by " +
                            teamHandler.getTeam(hit.getDamager()).getChatColour() + killer.getName() + ChatColor.GRAY + "."));

            return;
        }

        world.getPlayers().forEach((player_) -> player_.sendMessage(teamHandler.getTeam(player).getChatColour() + player.getName() + ChatColor.GRAY + " fell in the void."));

    }

    public void onKillDeath(Player player, Player killer) {
        respawn(player);
        player.playSound(player.getLocation(), Sound.HURT_FLESH, 1, 1);
        killer.playSound(killer.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);

        if (this.saveStats) {
            this.getGlobalStats(player).addDeath();
            this.getOverallStats(player).addDeath();
        }

        handleKill(killer);

        world.getPlayers().forEach((player_) -> player_.sendMessage(teamHandler.getTeam(player).getChatColour() + player.getName() + ChatColor.GRAY + " was killed by " + teamHandler.getTeam(killer.getUniqueId()).getChatColour() + killer.getName() + ChatColor.GRAY + "."));
    }

    public void onRandomDeath(Player player) {
        respawn(player);

        world.getPlayers().forEach((player_) -> player_.sendMessage(teamHandler.getTeam(player).getChatColour() + player.getName() + ChatColor.GRAY + " died."));

        if (this.saveStats) {
            this.getGlobalStats(player).addDeath();
            this.getOverallStats(player).addDeath();
        }
    }

    private void respawn(Player player) {
        goalProcessors.remove(player.getUniqueId());

        player.setItemOnCursor(null);
        // are these necessary?
        player.setSaturation(20);
        player.setHealth(20);

        // inventory management
        XinCraftPlugin.get().getDatabaseManager().getPlayer(player).getKit().setTo(player, teamHandler.getTeam(player.getUniqueId()));

        arrowManager.setCooldown(player.getUniqueId(), Duration.ofMillis(0));
        arrowManager.endCountdown(player.getUniqueId());

        // is this necessary? could we just remove gapple effect
        player.getActivePotionEffects().forEach((effect) -> player.removePotionEffect(effect.getType()));

        // add spawn protection
        player.getInventory().setHelmet(new ItemStack(Material.GLASS));
        // potion effect event was added later
        Bukkit.getScheduler().runTaskLater(XinCraftPlugin.get(), () -> {
            player.getInventory().setHelmet(null);
        }, SPAWN_RESISTANCE_TICKS);

        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, SPAWN_RESISTANCE_TICKS, 9));

        // teleport the player to their cage while making sure they don't die on impact
        player.setFallDistance(0);
        player.setVelocity(player.getVelocity().zero());

        BridgeTeam bridgeTeam = teamHandler.getTeam(player.getUniqueId());
        Location location = map.getTeamSpawnpoints().get(bridgeTeam).toTeamSpawn(world, bridgeTeam);

        if (cageHandler.playersCaged()) {
            location.add(0, 5 + 1, 0);
        }

        player.teleport(location);
    }

    @Override
    public void handleTimeFail() {
        int redScore = scores.get(BridgeTeam.RED);
        int blueScore = scores.get(BridgeTeam.BLUE);

        BridgeTeam winningBridgeTeam = redScore == blueScore ? null
                : redScore > blueScore ? BridgeTeam.RED : BridgeTeam.BLUE;

        BridgeTeam losingBridgeTeam = winningBridgeTeam == BridgeTeam.RED ? BridgeTeam.BLUE : BridgeTeam.RED;

        // if its not a draw
        if (winningBridgeTeam != null) {
            for (Player player : worldPlayersOf(players)) {
                if (teamHandler.getTeam(player.getUniqueId()) == winningBridgeTeam) {
                    handleWin(player);
                    new Title(ChatColor.GREEN + "" + ChatColor.BOLD + "VICTORY! " + ChatColor.GRAY + ChatColor.ITALIC + "(Time Limit)",
                            winningBridgeTeam.getChatColour() + "" + ChatColor.BOLD + " - " + losingBridgeTeam.getChatColour() + ChatColor.BOLD,
                            20, 60, 20).send(player);
                } else {
                    handleLoss(player);
                    new Title(ChatColor.RED + "" + ChatColor.BOLD + "DEFEAT! " + ChatColor.GRAY + ChatColor.ITALIC + "(Time Limit)",
                            losingBridgeTeam.getChatColour() + "" + ChatColor.BOLD + " - " + winningBridgeTeam.getChatColour() + ChatColor.BOLD,
                            20, 60, 20).send(player);
                }
            }
        } else {
            for (Player player : worldPlayersOf(players)) {
                new Title(ChatColor.YELLOW + "" + ChatColor.BOLD + "DRAW", ChatColor.YELLOW + "Reached time limit!", 20, 60, 20).send(player);
                handleDraw(player);
            }
        }

        finishMatch();

    }

    public void handleGoal(Player player, Location goalLocation) {
        // if the game isn't currently ongoing, return early
        if (status != MatchStatus.PLAYING) {
            return;
        }

        // if the player is already being processed for a goal
        if (goalProcessors.contains(player.getUniqueId())) {
            return;
        }

        // process the player for the goal
        goalProcessors.add(player.getUniqueId());

        // if z is positive, red team, otherwise blue team - hardcode
        BridgeTeam goalTeam = goalLocation.getBlockZ() > 0 ? BridgeTeam.RED : BridgeTeam.BLUE;
        BridgeTeam playerTeam = teamHandler.getTeam(player.getUniqueId());

        XinCraftPlugin.get().getLogger().info("Handling goal for " + player.getName() + ". " +
                "Goal is on the side of team " + goalTeam + " and player is on team " + playerTeam);

        // jumping through own goal
        if (goalTeam == playerTeam) {
            return;
        }

        int score = scores.get(playerTeam) + 1;
        scores.put(playerTeam, score);

        int goal = goals.get(player.getUniqueId()) + 1;
        goals.put(player.getUniqueId(), goal);

        // update goals on the sidebar
        XinCraftPlugin.get().getSidebarUpdater().update(world);

        // broadcast the player scoring the goal
        world.getPlayers().forEach((player_) ->
                player_.sendMessage(playerTeam.getChatColour() + player.getName() + ChatColor.GRAY + " has scored!"));

        // stats
        if (saveStats) {
            getGlobalStats(player).addGoal();
            getOverallStats(player).addGoal();
        }

        // if not a winning goal
        if (score < 5) {
            cageHandler.cageTeams(map, world, new ScorerPlayer(player.getName(), playerTeam), (this::respawn));
            return;
        }

        // 5-1 or 2-4, etc
        String subtitle = getEndingSubtitle();

        // loop through the actual players playing the game
        for (Player matchPlayer : worldPlayersOf(players)) {
            // if the match player is on the team that scored and won
            if (teamHandler.getTeam(matchPlayer.getUniqueId()) == playerTeam) {
                XinCraftPlugin.get().getLogger().info("Handling win for " + matchPlayer.getName());

                handleWin(matchPlayer);
                // send the player a victory title
                new Title(
                        ChatColor.BOLD.toString() + ChatColor.GREEN + "VICTORY!",
                        subtitle,
                        20, 60, 20).send(matchPlayer);
            } else {
                XinCraftPlugin.get().getLogger().info("Handling loss for " + matchPlayer.getName());

                handleLoss(matchPlayer);
                // send the player a defeat title
                new Title(
                        ChatColor.BOLD.toString() + ChatColor.RED + "DEFEAT",
                        subtitle,
                        20, 60, 20).send(matchPlayer);
            }
        }

        finishMatch();
    }

    private String getEndingSubtitle() {
        StringBuilder subtitle = new StringBuilder();
        for (BridgeTeam bridgeTeam : scores.keySet()) {
            subtitle
                    .append(bridgeTeam.getChatColour())
                    .append(ChatColor.BOLD)
                    .append(scores.get(bridgeTeam))
                    .append(" ")
                    .append(ChatColor.GRAY)
                    .append(ChatColor.BOLD)
                    .append("- ");
        }
        // remove the last "- " at the end
        subtitle.delete(subtitle.length() - 2, subtitle.length());
        return subtitle.toString();
    }

    private void handleKill(Player player) {
        kills.replace(player.getUniqueId(), kills.get(player.getUniqueId()) + 1);

        if (this.saveStats) {
            this.getGlobalStats(player).addKill();
            this.getOverallStats(player).addKill();
        }
    }

    public Stats getGlobalStats(Player player) {
        StatsType statsType = teamHandler.getConfig().getStatsType();
        return XinCraftPlugin.get().getDatabaseManager().getPlayer(player).getStats(statsType);
    }

    private Stats getOverallStats(Player player) {
        return XinCraftPlugin.get().getDatabaseManager().getPlayer(player).getStats(StatsType.Overall);
    }

    public void handleWin(Player player) {
        if (this.saveStats) {
            this.getGlobalStats(player).addWin();
            this.getOverallStats(player).addWin();
            TitleUtils.updateTitle(player);
        }
    }


    public void handleLoss(Player player) {
        if (this.saveStats) {
            this.getGlobalStats(player).addLoss();
            this.getOverallStats(player).addLoss();
        }
    }


    protected void handleDraw(Player player) {
        if (this.saveStats) {
            this.getGlobalStats(player).addDraw();
            this.getOverallStats(player).addDraw();
        }
    }

    public boolean isFull() {
        return players.size() == teamHandler.getConfig().getMaxPlayers();
    }

    @Override
    public Document getMetricData() {
        int ticksLeft = timer != null ? timer.getCountdown().getTicksLeft() : -1;

        return new Document()
                .append("id", id)
                .append("map", map.getName())
                .append("team-size", teamHandler.getConfig().getTeamSize())
                .append("scores", parseMap(scores))
                .append("kills", deidentifyMap(kills))
                .append("goals", deidentifyMap(goals))
                .append("time-left", ticksLeft)
                .append("save-stats", saveStats);
    }

    private Map<String, Integer> parseMap(Map<BridgeTeam, Integer> originalMap) {
        return originalMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        Map.Entry::getValue,
                        Integer::sum, // sum values for duplicate keys
                        HashMap::new
                ));
    }

    private Map<String, Integer> deidentifyMap(Map<UUID, Integer> originalMap) {
        return originalMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> teamHandler.getTeam(entry.getKey()).name(),
                        Map.Entry::getValue,
                        Integer::sum, // sum values for duplicate keys
                        HashMap::new
                ));
    }


    @Override
    public void registerObjectives(Player player) {
        Scoreboard scoreboard = player.getScoreboard();

        Objective nametag = scoreboard.registerNewObjective("nametag-health", Criterias.HEALTH);

        nametag.setDisplaySlot(DisplaySlot.BELOW_NAME);
        nametag.setDisplayName(ChatColor.RED + StringEscapeUtils.unescapeJava("❤"));

        Objective tab = scoreboard.getObjective("tab-health");

        if (tab == null) {
            tab = scoreboard.registerNewObjective("tab-health", Criterias.HEALTH);
        }

        tab.setDisplaySlot(DisplaySlot.PLAYER_LIST);

        // set scores to fix issue of showing health at 0
        for (Player playerScore : world.getPlayers()) {
            nametag.getScore(playerScore.getName()).setScore((int) playerScore.getHealth());
            tab.getScore(playerScore.getName()).setScore((int) playerScore.getHealth());
        }
    }
}
