package net.xincraft.systems.match.match;

import net.xincraft.XinCraftPlugin;
import net.xincraft.database.data.Cage;
import net.xincraft.systems.match.cage.CageCache;
import net.xincraft.systems.match.cage.CageStructure;
import net.xincraft.systems.match.cage.ScorerPlayer;
import net.xincraft.systems.match.countdown.CageCountdown;
import net.xincraft.systems.match.map.BridgeMap;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class CageHandler {
    public static Map<String, CageStructure> cageStructures = new HashMap<>();

    static {
        for (Cage cage : Cage.values()) {
            String cageName = cage.getId();

            if (cageName.equals("default")) {
                cageStructures.put("red", CageStructure.readJson("red"));
                cageStructures.put("blue", CageStructure.readJson("blue"));
                continue;
            }

            cageStructures.put(cageName, CageStructure.readJson(cageName));
        }
    }

    private final TeamHandler teamHandler;

    private final Map<BridgeTeam, CageStructure> cages = new HashMap<>();
    private final Map<BridgeTeam, CageCache> cageCaches = new HashMap<>();

    private CageCountdown cageCountdown;

    public CageHandler(TeamHandler teamHandler) {
        this.teamHandler = teamHandler;
    }

    public void loadCache(BridgeTeam team, Location location) {
        // cache the original blocks so we can reset the cage
        CageCache cache = new CageCache(location);
        cageCaches.put(team, cache);
    }

    public void loadCages() {
        for (BridgeTeam bridgeTeam : BridgeTeam.values()) {
            String cageName = getHighestPriorityCage(teamHandler, bridgeTeam);

            // hardcode default to swap to red or blue
            if (Objects.equals(cageName, Cage.DEFAULT.name())) {
                cageName = bridgeTeam.name();
            }

            cageName = cageName.toLowerCase();

            cages.put(bridgeTeam, cageStructures.get(cageName));
        }
    }

    private static @NotNull String getHighestPriorityCage(TeamHandler teamHandler, BridgeTeam bridgeTeam) {
        return teamHandler.getPlayers(bridgeTeam).stream()
                .map(player -> XinCraftPlugin.get().getDatabaseManager().getPlayer(player).getSettings().getCage())
                .max(Comparator.comparingInt(Cage::getPriority))
                .map(Cage::name)
                .orElse("");
    }

    public void cageTeams(BridgeMap map, World world, ScorerPlayer scorer, Consumer<Player> callback) {
        cageCountdown = new CageCountdown(this, scorer);

        for (BridgeTeam bridgeTeam : BridgeTeam.values()) {
            // send each team to their cage
            Location cageLocation = map.getTeamSpawnpoints().get(bridgeTeam).toTeamSpawn(world, bridgeTeam)
                    .add(0, 5 - getYPosition(bridgeTeam), 0);
            cageTeam(bridgeTeam, cageLocation, callback);
        }
    }

    private void cageTeam(BridgeTeam bridgeTeam, Location cageLocation, Consumer<Player> callback) {
        cages.get(bridgeTeam).generate(cageLocation);

        // loop through all players in the team
        for (Player player : teamHandler.getPlayers(bridgeTeam)) {
            // set the player to adventure mode
            player.setGameMode(GameMode.ADVENTURE);
            cageCountdown.addPlayer(player);
            // respawn the player
            callback.accept(player);
        }
    }

    public void undoCages() {
        for (BridgeTeam bridgeTeam : BridgeTeam.values()) {
            cageCaches.get(bridgeTeam).generate();
        }
    }

    public void finish() {
        // set the cages back to before the match with the cache and end the countdown
        cageCountdown.reset();
    }

    public boolean playersCaged() {
        return cageCountdown != null;
    }

    public void addPlayer(Player player) {
        cageCountdown.addPlayer(player);
    }

    public void removePlayer(Player player) {
        cageCountdown.removePlayer(player);
    }

    public int getYPosition(BridgeTeam bridgeTeam) {
        return cages.get(bridgeTeam).getYPosition();
    }
}
