package net.xincraft.systems.match.match;

import lombok.Getter;
import net.xincraft.WorldManager;
import net.xincraft.XinCraftPlugin;
import net.xincraft.database.metric.Metric;
import net.xincraft.systems.match.BridgeMatchManager;
import net.xincraft.systems.match.map.BridgeMap;
import org.apache.commons.io.FileUtils;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public abstract class Match implements Metric, WorldManager {
    protected final int id;

    protected final World world;
    protected final BridgeMap map;

    protected final Collection<UUID> players = new HashSet<>();
    protected final Collection<UUID> spectators = new HashSet<>();
    protected final Collection<UUID> limboPlayers = new HashSet<>();

    public Match(int id, BridgeMap map) {
        this.id = id;
        this.map = map;

        // copy preset world to matches directory
        String matchDirectory = BridgeMatchManager.MATCHES_DIRECTORY + File.separator + "match-" + id;
        try {
            FileUtils.copyDirectory(new File(BridgeMatchManager.MATCHES_PRESET_DIRECTORY + File.separator + map.getName()),
                    new File(matchDirectory));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        WorldCreator creator = new WorldCreator(matchDirectory);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("2;0;1;");
        creator.generateStructures(false);

        world = creator.createWorld();

        if (world == null) {
            throw new RuntimeException("Failed to create world");
        }

        XinCraftPlugin.get().getWorldManagers().add(this);

        // initialise world settings
        world.setAutoSave(false);
        world.setGameRuleValue("randomTickSpeed", "0");
        world.setGameRuleValue("doMobSpawning", "false");
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
}
