package net.xincraft.systems.match.match;

import lombok.Getter;
import lombok.Setter;
import net.xincraft.XinCraftPlugin;
import net.xincraft.database.data.Cage;
import net.xincraft.database.data.stats.Stats;
import net.xincraft.database.data.stats.StatsType;
import net.xincraft.database.metric.MetricCollector;
import net.xincraft.systems.match.CooldownManager;
import net.xincraft.systems.match.countdown.CageCountdown;
import net.xincraft.systems.match.countdown.MatchFinishCountdown;
import net.xincraft.systems.match.countdown.StartingCountdown;
import net.xincraft.systems.match.hits.HitManager;
import net.xincraft.systems.match.hits.LastHit;
import net.xincraft.systems.match.listener.QuitItemListener;
import net.xincraft.systems.match.map.BridgeMap;
import net.xincraft.systems.match.runnable.BridgeTimer;
import net.xincraft.systems.party.Party;
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
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// sitting on top of the white clay the player will sit at 93 y level

// facing south will be the red team

// facing north will be the blue team

// player always spawns on the red team
@Getter
public class BridgeMatch extends Match implements ObjectiveWorld {
    private static final int SPAWN_RESISTANCE_TICKS = 2 * 20;

    private BridgeTimer timer;

    private final Map<UUID, BridgeTeam> teams = new HashMap<>();
    private final Map<BridgeTeam, Integer> scores = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> goals = new HashMap<>();

    private final TeamConfiguration teamConfig;

    private boolean isOngoing = false;
    private boolean isEnding = false;

    @Setter
    private Countdown startingCountdown;
    private final Map<BridgeTeam, CageCountdown> cageCountdowns = new HashMap<>();
    private final Map<BridgeTeam, net.xincraft.systems.match.cage.Cage> cages = new HashMap<>();
    private final CooldownManager arrowManager = new CooldownManager();
    private final HitManager hitManager = new HitManager();
    private final Set<UUID> goalProcessors = new HashSet<>();

    @Setter
    private boolean saveStats = true;
    @Setter
    private boolean privateParty = false;

    public BridgeMatch(int id, BridgeMap map, TeamConfiguration teamConfiguration) {
        super(id, map);
        this.teamConfig = teamConfiguration;
    }

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());

        // initialise player
        player.setGameMode(GameMode.ADVENTURE);
        player.setExp(0);
        player.setLevel(0);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getActivePotionEffects().forEach((effect) -> player.removePotionEffect(effect.getType()));

        // clear inventory and set quit item
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setItem(8, QuitItemListener.QUIT);

        // teleport player to the configured spawn position and handle failure
        if (!player.teleport(map.getJoinSpawnPoint().toLocation(world))) {
            player.sendMessage(ChatColor.RED + "Failed to teleport you to the match, please rejoin.");
            XinCraftPlugin.get().getLobbyWorld().setup(player);
            throw new RuntimeException("Failed to teleport player");
        }

        // magic naming
        new QueueingNametag().applyTo(world, player);
        player.setPlayerListName(ChatColor.MAGIC + player.getName());

        for (Player matchPlayer : world.getPlayers()) {
            // broadcast player joining
            matchPlayer.sendMessage(
                    ChatColor.GRAY.toString() + ChatColor.MAGIC + player.getName() +
                            ChatColor.YELLOW + " has joined (" +
                            ChatColor.AQUA + players.size() +
                            ChatColor.YELLOW + "/" +
                            ChatColor.AQUA + teamConfig.getMaxPlayers() +
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
        player.setPlayerListName(getTeam(player.getUniqueId()).getChatColour() + player.getName());


        for (Player matchPlayer : world.getPlayers()) {
            // broadcast player joining
            matchPlayer.sendMessage(teams.get(player.getUniqueId()).getChatColour() + player.getName() + ChatColor.GRAY + " has rejoined!");
        }

        // scoreboard handling
        XinCraftPlugin.get().getSidebarUpdater()
                .swapSidebar(player, new MatchSidebar(this, player));
        XinCraftPlugin.get().getSidebarUpdater().update(world);

        XinCraftPlugin.get().getSidebarUpdater().updateLobby();

        CageCountdown countdown = cageCountdowns.get(getTeam(player.getUniqueId()));

        if (countdown == null) {
            respawn(player, false);
        } else {
            countdown.addPlayer(player);
            respawn(player, true);
        }
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
        if (players.size() != teamConfig.getMaxPlayers()) {
            return;
        }

        // run a countdown of 5 seconds running every 20 ticks (1 second)
        startingCountdown = new StartingCountdown(this);
    }

    public void startMatch() {
        isOngoing = true;
        timer = new BridgeTimer(this);

        BridgeTeam[] bridgeTeams = BridgeTeam.values();

        // if in a private party set teams based on parties, for example everyone from one party will go on red
        // and everyone on the other will go on blue
        if (privateParty) {
            // loop through the players and get their party. if they are the owner, add the party to a list
            List<Party> parties = new ArrayList<>();
            for (Player player : worldPlayersOf(players)) {
                Party party = XinCraftPlugin.get().getPartyManager().getParty(player);
                if (party.isOwner(player)) {
                    parties.add(party);
                }
            }

            // get the players in each party
            List<List<Player>> partyPlayers = parties.stream().map(Party::getPlayers).collect(Collectors.toList());

            // set the teams
            for (int i = 0; i < partyPlayers.size(); i++) {
                for (Player player : partyPlayers.get(i)) {
                    this.teams.put(player.getUniqueId(), bridgeTeams[i]);
                }
            }
        } else {
            // setup teams for all players
            int teamIndex = 0;

            for (Player player : worldPlayersOf(players)) {
                // team handling
                BridgeTeam bridgeTeam = bridgeTeams[teamIndex];
                this.teams.put(player.getUniqueId(), bridgeTeam);
                // Cycle to the next team
                teamIndex = (teamIndex + 1) % bridgeTeams.length;
            }
        }

        for (BridgeTeam bridgeTeam : bridgeTeams) {
            scores.put(bridgeTeam, 0);
        }

        for (Player player : worldPlayersOf(players)) {

            // setup nametags for team colors
            new BridgeMatchNametag(this).applyTo(world, player);

            registerObjectives(player);

            // set tablist colors
            player.setPlayerListName(getTeam(player.getUniqueId()).getChatColour() + player.getName());

            // set kills, goals to 0
            goals.put(player.getUniqueId(), 0);
            kills.put(player.getUniqueId(), 0);
        }

        // benchmark this:
        Instant startTime = Instant.now();
        cageAndCacheTeamsAsync();
        Duration timeTaken = Duration.between(startTime, Instant.now());
        XinCraftPlugin.get().getLogger().info("Cached and generated cages for each team (took " + timeTaken.toMillis() + "ms)");
    }

    public boolean checkFinish() {
        XinCraftPlugin.get().getLogger().info("Checking if match " + id + " is ready to finish.");

        if (players.isEmpty()) {
            XinCraftPlugin.get().getLogger().info("Ending match " + id + " after checkFinish as players is empty.");
            endMatch();
            return false;
        }

        // if we are waiting for players, we never need to run an ending countdown if someone leaves.
        if (!isEnding && !isOngoing) {
            return false;
        }


        // if any team has 0 players, finish the match
        for (BridgeTeam bridgeTeam : BridgeTeam.values()) {
            if (world.getPlayers().stream().filter(p ->
                    players.contains(p.getUniqueId())).noneMatch(p ->
                    teams.get(p.getUniqueId()).equals(bridgeTeam))) {

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
        if (!isOngoing) {
            removePlayer(player);

            // if player leaves during starting countdown
            if (startingCountdown != null) {
                startingCountdown.cancel();
                startingCountdown = null;

                XinCraftPlugin.get().getSidebarUpdater().update(world);

                for (Player worldPlayer : world.getPlayers()) {
                    worldPlayer.sendMessage(ChatColor.RED + "We don't have enough players! Start cancelled.");
                    new Title(ChatColor.RED + "" + ChatColor.BOLD + "CANCELLED", "", 0, 20, 10).send(worldPlayer);
                }
            } else {
                XinCraftPlugin.get().getSidebarUpdater().update(world);

                if (!isEnding) {
                    checkFinish();
                }
            }
        } else {
            BridgeTeam losingBridgeTeam = getTeam(player.getUniqueId());
            BridgeTeam winningBridgeTeam = losingBridgeTeam == BridgeTeam.RED ? BridgeTeam.BLUE : BridgeTeam.RED;

            // get opposite team

            world.getPlayers().forEach(player_ -> {
                player_.sendMessage(losingBridgeTeam.getChatColour() + player.getName() + ChatColor.GRAY + " left the game.");
            });

            limboPlayers.add(player.getUniqueId());

            removePlayer(player);

            if (isEnding) {
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

                        System.out.println("Player: " + player_.getName() + " Team: " + getTeam(playerId) + " Winning Team: " + winningBridgeTeam);
                        if (getTeam(playerId) == winningBridgeTeam) {
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
        isOngoing = false;
        isEnding = true;

        timer.getCountdown().setPaused(true);

        // for all cage countdowns, reset them
        for (CageCountdown cageCountdown : cageCountdowns.values()) {
            cageCountdown.reset();
        }

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
        if (isOngoing && saveStats) {
            this.getGlobalStats(player).addDeath();
            this.getOverallStats(player).addDeath();
        }

        // remove them from the hit manager
        if (hitManager.getHit(player.getUniqueId()) != null) {
            hitManager.removeHit(player.getUniqueId());
        }

        BridgeTeam team = teams.get(player.getUniqueId());

        if (cageCountdowns.get(team) != null) {
            cageCountdowns.get(team).removePlayer(player);
        }

        players.remove(player.getUniqueId());
    }

    public void removeSpectator(Player player) {
        spectators.remove(player.getUniqueId());
    }

    private void cacheTeamCage(BridgeTeam bridgeTeam) {
        String cageName = "";

        // find the player who has the highest priority cage
        // loop through every player in our team
        int highestPriority = -1;
        for (Player player : world.getPlayers().stream().filter(p ->
                        players.contains(p.getUniqueId()) && teams.get(p.getUniqueId()) == bridgeTeam)
                .collect(Collectors.toList())) {
            Cage cageSetting = XinCraftPlugin.get().getDatabaseManager().getPlayer(player).getSettings().getCage();
            if (cageSetting.getPriority() > highestPriority) {
                highestPriority = cageSetting.getPriority();
                cageName = cageSetting.name();
            }
        }

        // hardcode default
        if (Objects.equals(cageName, Cage.DEFAULT.name())) {
            cageName = bridgeTeam.name();
        }

        cageName = cageName.toLowerCase();

        cages.put(bridgeTeam, net.xincraft.systems.match.cage.Cage.readJson(cageName));

        Location cageLocation = map.getTeamSpawnpoints().get(bridgeTeam).toTeamSpawn(world, bridgeTeam)
                .add(0, 5 - cages.get(bridgeTeam).getYPosition(), 0);

        cages.get(bridgeTeam).cache(cageLocation);
        System.out.println("Cage cached for " + bridgeTeam + ": " + cages.get(bridgeTeam).getName());
    }

    private CompletableFuture<Void> cacheTeamCageAsync(BridgeTeam bridgeTeam) {
        String cageName = "";

        // find the player who has the highest priority cage
        // loop through every player in our team
        int highestPriority = -1;
        for (Player player : world.getPlayers().stream().filter(p ->
                        players.contains(p.getUniqueId()) && teams.get(p.getUniqueId()) == bridgeTeam)
                .collect(Collectors.toList())) {
            Cage cageSetting = XinCraftPlugin.get().getDatabaseManager().getPlayer(player).getSettings().getCage();
            if (cageSetting.getPriority() > highestPriority) {
                highestPriority = cageSetting.getPriority();
                cageName = cageSetting.name();
            }
        }

        // hardcode default
        if (Objects.equals(cageName, Cage.DEFAULT.name())) {
            cageName = bridgeTeam.name();
        }

        cageName = cageName.toLowerCase();

        String finalCageName = cageName;
        return CompletableFuture.supplyAsync(() -> net.xincraft.systems.match.cage.Cage.readJson(finalCageName))
                .thenAcceptAsync(cage -> {
                    cages.put(bridgeTeam, cage);

                    Location cageLocation = map.getTeamSpawnpoints().get(bridgeTeam).toTeamSpawn(world, bridgeTeam)
                            .add(0, 5 - cages.get(bridgeTeam).getYPosition(), 0);

                    cages.get(bridgeTeam).cache(cageLocation);
                    System.out.println("Cage cached for " + bridgeTeam + ": " + cages.get(bridgeTeam).getName());
                }).exceptionally(throwable -> {
                    throw new RuntimeException("Failed to cache cage for " + bridgeTeam, throwable);
                });
    }

    public void cageAndCacheTeams() {
        for (BridgeTeam bridgeTeam : BridgeTeam.values()) {
            // cache cages so we don't have to lookup json every time
            cacheTeamCage(bridgeTeam);
            // send whole team to cage
            cageTeam(bridgeTeam, null);
        }
    }

    public void cageAndCacheTeamsAsync() {
        // Cache cages asynchronously
        CompletableFuture<Void> redTeamCacheFuture = cacheTeamCageAsync(BridgeTeam.RED);
        CompletableFuture<Void> blueTeamCacheFuture = cacheTeamCageAsync(BridgeTeam.BLUE);

        CompletableFuture<Void> allCachesCompleted = CompletableFuture.allOf(redTeamCacheFuture, blueTeamCacheFuture);

        allCachesCompleted.thenRun(() -> {
            System.out.println("All cages cached, now running generation code");
            // Generate cages asynchronously
            CompletableFuture<Void> redTeamCageFuture = CompletableFuture.runAsync(() -> {
                Location redCageLocation = map.getTeamSpawnpoints().get(BridgeTeam.RED).toTeamSpawn(world, BridgeTeam.RED)
                        .add(0, 5 - cages.get(BridgeTeam.RED).getYPosition(), 0);
                System.out.println("Generating red cage at " + redCageLocation);
                cages.get(BridgeTeam.RED).generate(redCageLocation);
                System.out.println("Red cage generated");
            });

            CompletableFuture<Void> blueTeamCageFuture = CompletableFuture.runAsync(() -> {
                Location blueCageLocation = map.getTeamSpawnpoints().get(BridgeTeam.BLUE).toTeamSpawn(world, BridgeTeam.BLUE)
                        .add(0, 5 - cages.get(BridgeTeam.BLUE).getYPosition(), 0);
                System.out.println("Generating blue cage at " + blueCageLocation);
                cages.get(BridgeTeam.BLUE).generate(blueCageLocation);
                System.out.println("Blue cage generated");
            });

            CompletableFuture<Void> allCagesGenerated = CompletableFuture.allOf(redTeamCageFuture, blueTeamCageFuture);

            allCagesGenerated.thenRun(() -> {
                System.out.println("All cages generated, now sending players to cages");
                Bukkit.getScheduler().runTask(XinCraftPlugin.get(), () -> {
                    for (BridgeTeam bridgeTeam : BridgeTeam.values()) {
                        for (Player player : world.getPlayers().stream().filter(p ->
                                players.contains(p.getUniqueId()) &&
                                        teams.get(p.getUniqueId()).equals(bridgeTeam)).collect(Collectors.toList())) {
                            player.setGameMode(GameMode.ADVENTURE);
                            respawn(player, true);

                            List<Player> players = new ArrayList<>();

                            for (Player player_ : worldPlayersOf(this.players)) {
                                if (teams.get(player_.getUniqueId()) == bridgeTeam) {
                                    players.add(player_);
                                }
                            }

                            Location cageLocation = map.getTeamSpawnpoints().get(bridgeTeam).toTeamSpawn(world, bridgeTeam)
                                    .add(0, 5 - cages.get(bridgeTeam).getYPosition(), 0);
                            cageCountdowns.put(bridgeTeam, new CageCountdown(this, null, players, bridgeTeam, cageLocation));
                        }
                    }
                });
            }).exceptionally(throwable -> {
                throw new RuntimeException("Failed to generate cages", throwable);
            });
        }).exceptionally(throwable -> {
            throw new RuntimeException("Failed to cache cages", throwable);
        });
    }

    public void cageTeam(BridgeTeam bridgeTeam, Player scorer) {

        // cage is 3 blocks above spawnpoint.
        // player is spawned 3 blocks above cage so 6 blocks above spawn
        // generate the cage and then send the player to the cage
        Location cageLocation = map.getTeamSpawnpoints().get(bridgeTeam).toTeamSpawn(world, bridgeTeam)
                .add(0, 5 - cages.get(bridgeTeam).getYPosition(), 0);
        // dojo the y pos is 97
        // default cage spawn pos is 3 blocks above that
        // cage spawns 5 blocks above y pos
        // so when player is standing on the cage they should be at 102
        // and the bottom block of the cage should sit at 99

        cages.get(bridgeTeam).generate(cageLocation);

        // loop through all players in the world who are playing and on the same team
        for (Player player : world.getPlayers().stream().filter(p ->
                players.contains(p.getUniqueId()) &&
                        teams.get(p.getUniqueId()).equals(bridgeTeam)).collect(Collectors.toList())) {
            player.setGameMode(GameMode.ADVENTURE);
            respawn(player, true);

            List<Player> players = new ArrayList<>();

            for (Player player_ : worldPlayersOf(this.players)) {
                if (teams.get(player_.getUniqueId()) == bridgeTeam) {
                    players.add(player_);
                }
            }

            // cage countdown for each player
            cageCountdowns.put(bridgeTeam, new CageCountdown(this, scorer, players, bridgeTeam, cageLocation));
        }
    }

    public void onVoidDeath(Player player) {
        respawn(player, false);
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
                    getTeam(player.getUniqueId()).getChatColour() + player.getName() +
                            ChatColor.GRAY + " was knocked into the void by " +
                            getTeam(hit.getDamager()).getChatColour() + killer.getName() + ChatColor.GRAY + "."));

            return;
        }

        world.getPlayers().forEach((player_) -> player_.sendMessage(getTeam(player.getUniqueId()).getChatColour() + player.getName() + ChatColor.GRAY + " fell in the void."));

    }

    public void onKillDeath(Player player, Player killer) {
        respawn(player, false);
        player.playSound(player.getLocation(), Sound.HURT_FLESH, 1, 1);
        killer.playSound(killer.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);

        if (this.saveStats) {
            this.getGlobalStats(player).addDeath();
            this.getOverallStats(player).addDeath();
        }

        handleKill(killer);

        world.getPlayers().forEach((player_) -> player_.sendMessage(getTeam(player.getUniqueId()).getChatColour() + player.getName() + ChatColor.GRAY + " was killed by " + getTeam(killer.getUniqueId()).getChatColour() + killer.getName() + ChatColor.GRAY + "."));
    }

    public BridgeTeam getTeam(UUID uuid) {
        return teams.get(uuid);
    }

    public void onRandomDeath(Player player) {
        respawn(player, false);

        world.getPlayers().forEach((player_) -> player_.sendMessage(getTeam(player.getUniqueId()).getChatColour() + player.getName() + ChatColor.GRAY + " died."));

        if (this.saveStats) {
            this.getGlobalStats(player).addDeath();
            this.getOverallStats(player).addDeath();
        }
    }

    private void respawn(Player player, boolean cage) {
        player.setItemOnCursor(null);
        // are these necessary?
        player.setSaturation(20);
        player.setHealth(20);

        // inventory management
        XinCraftPlugin.get().getDatabaseManager().getPlayer(player).getKit().setTo(player, getTeam(player.getUniqueId()));

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

        BridgeTeam bridgeTeam = getTeam(player.getUniqueId());
        Location location = map.getTeamSpawnpoints().get(bridgeTeam).toTeamSpawn(world, bridgeTeam);
        if (cage) {
            location.add(0, 5 + 1, 0);
        }

        player.teleport(location);
    }

    public void handleTimeFail() {
        int redScore = scores.get(BridgeTeam.RED);
        int blueScore = scores.get(BridgeTeam.BLUE);

        BridgeTeam winningBridgeTeam = redScore == blueScore ? null
                : redScore > blueScore ? BridgeTeam.RED : BridgeTeam.BLUE;

        BridgeTeam losingBridgeTeam = winningBridgeTeam == BridgeTeam.RED ? BridgeTeam.BLUE : BridgeTeam.RED;

        // if its not a draw
        if (winningBridgeTeam != null) {
            for (Player player : worldPlayersOf(players)) {
                if (getTeam(player.getUniqueId()) == winningBridgeTeam) {
                    handleWin(player);
                    new Title("&a&lVICTORY! &7&o(Time Limit)",
                            "" + winningBridgeTeam.getChatColour() + ChatColor.BOLD + //match.getScore(winningTeam)
                                    " &7&l- " + losingBridgeTeam.getChatColour() + ChatColor.BOLD
                            //+ match.getScore(losingTeam)
                            ,
                            20, 60, 20).send(player);
                } else {
                    handleLoss(player);
                    new Title("&c&lDEFEAT &7&o(Time Limit)",
                            "" + losingBridgeTeam.getChatColour() + ChatColor.BOLD + //match.getScore(losingTeam)
                                    " &7&l- " + winningBridgeTeam.getChatColour() + ChatColor.BOLD
                            //match.getScore(winningTeam),
                            ,
                            20, 60, 20).send(player);
                }

            }
        } else {
            for (Player player : worldPlayersOf(players)) {
                new Title("&e&lDRAW", "&eReached time limit!", 20, 60, 20).send(player);
                handleDraw(player);
            }
        }
    }

    public void handleGoal(Player player, Location goalLocation) {
        // if the game isn't currently ongoing, return early
        if (!isOngoing) {
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
        BridgeTeam playerTeam = getTeam(player.getUniqueId());

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
            // send everyone to their cages
            for (BridgeTeam bridgeTeam : BridgeTeam.values()) {
                cageTeam(bridgeTeam, player);
            }

            return;
        }

        // 5-1 or 2-4, etc
        String subtitle = getEndingSubtitle();

        // loop through the actual players playing the game
        for (Player matchPlayer : worldPlayersOf(players)) {
            // if the match player is on the team that scored and won
            if (getTeam(matchPlayer.getUniqueId()) == playerTeam) {
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

    private @NotNull List<Player> worldPlayersOf(Collection<UUID> players) {
        return world.getPlayers().stream().filter(p ->
                players.contains(p.getUniqueId())).collect(Collectors.toList());
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
        StatsType statsType = teamConfig.getStatsType();
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
        return players.size() == teamConfig.getMaxPlayers();
    }

    @Override
    public Document getMetricData() {
        int ticksLeft = timer != null ? timer.getCountdown().getTicksLeft() : -1;

        return new Document()
                .append("id", id)
                .append("map", map.getName())
                .append("team-size", teamConfig.getTeamSize())
                .append("scores", parseMap(scores))
                .append("kills", deidentifyMap(kills))
                .append("goals", deidentifyMap(goals))
                .append("time-left", ticksLeft)
                .append("save-stats", saveStats)
                .append("private-party", privateParty);
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
                        entry -> getTeam(entry.getKey()).name(),
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
