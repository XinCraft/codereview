package net.xincraft.systems.match.match;

import lombok.Getter;
import lombok.Setter;
import net.xincraft.WorldManager;
import net.xincraft.XinCraftPlugin;
import net.xincraft.database.metric.Metric;
import net.xincraft.systems.match.BridgeMatchManager;
import net.xincraft.systems.match.listener.QuitItemListener;
import net.xincraft.systems.match.map.BridgeMap;
import net.xincraft.systems.match.runnable.MatchTimer;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public abstract class Match implements Metric, WorldManager {
    protected final int id;

    protected final World world;
    protected final BridgeMap map;

    protected final Collection<UUID> players = new HashSet<>();
    protected final Collection<UUID> spectators = new HashSet<>();
    protected final Collection<UUID> limboPlayers = new HashSet<>();

    protected final MatchTimer timer;

    protected MatchStatus status = MatchStatus.QUEUEING;

    @Setter
    protected boolean saveStats = true;

    public Match(int id, BridgeMap map, int gameLengthTicks) {
        this.id = id;
        this.map = map;
        this.timer = new MatchTimer(this, gameLengthTicks);

        world = loadWorld(id, map);
    }

    private @NotNull World loadWorld(int id, BridgeMap map) {
        // copy preset world to matches directory
        String matchDirectory = BridgeMatchManager.getMatchDir(id);

        try {
            String presetDir = getPresetDir(map);
            FileUtils.copyDirectory(new File(presetDir), new File(matchDirectory));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        WorldCreator creator = new WorldCreator(matchDirectory);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("2;0;1;");
        creator.generateStructures(false);

        World world = creator.createWorld();

        XinCraftPlugin.get().getWorldManagers().add(this);

        // initialise world settings
        world.setAutoSave(false);
        world.setGameRuleValue("randomTickSpeed", "0");
        world.setGameRuleValue("doMobSpawning", "false");
        return world;
    }

    private static @NotNull String getPresetDir(BridgeMap map) {
        return BridgeMatchManager.MATCHES_PRESET_DIRECTORY + File.separator + map.getName();
    }

    @Override
    public UUID getWorldUUID() {
        return world.getUID();
    }

    @Override
    public Collection<UUID> visiblePlayers() {
        // return both spectators and players
        Set<UUID> allPlayers = new HashSet<>(players);
        allPlayers.addAll(spectators);
        return allPlayers;
    }

    protected @NotNull List<Player> worldPlayersOf(Collection<UUID> players) {
        return world.getPlayers().stream().filter(p ->
                players.contains(p.getUniqueId())).collect(Collectors.toList());
    }

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());

        // initialise player
        player.setGameMode(GameMode.ADVENTURE);
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
    }

    abstract public void handleTimeFail();
}
