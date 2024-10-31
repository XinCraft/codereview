package net.xincraft.systems.match;

import lombok.Getter;
import net.xincraft.XinCraftPlugin;
import net.xincraft.manager.LobbyWorld;
import net.xincraft.systems.match.cage.CageStructure;
import net.xincraft.systems.match.duel.DuelManager;
import net.xincraft.systems.match.map.BridgeMap;
import net.xincraft.systems.match.map.BridgeMapType;
import net.xincraft.systems.match.match.BridgeMatch;
import net.xincraft.systems.match.match.MatchStatus;
import net.xincraft.systems.match.match.TeamConfiguration;
import net.xincraft.systems.party.Party;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BridgeMatchManager {
    public static final String MATCHES_DIRECTORY = "matches";
    public static final String MATCHES_PRESET_DIRECTORY = "matches-preset";

    public static boolean saveStats;

    private final HashMap<Integer, BridgeMatch> matches = new HashMap<>();

    // we want to cache these so we don't have to access the directory every queue
    @Getter
    private List<BridgeMap> loadedMaps;
    @Getter
    private final List<BridgeMap> enabledMaps = new ArrayList<>();

    private final Random random = new Random();

    @Getter
    private final HashMap<UUID, CageStructure> cageCache = new HashMap<>();

    @Getter
    private final DuelManager duelManager = new DuelManager();

    private int id = 0;

    private final File mapConfigDir;

    public BridgeMatchManager() {
        // delete existing matches folder as safety in-case it didn't unload properly
        try {
            FileUtils.deleteDirectory(new File(MATCHES_DIRECTORY));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // recreate matches directory
        if (!new File(MATCHES_DIRECTORY).mkdirs()) {
            // if the directory wasn't created
            throw new RuntimeException("Failed to create matches directory");
        }

        // Clean matches-preset directory
        File matchPresetsDir = new File(MATCHES_PRESET_DIRECTORY);
        if (matchPresetsDir.exists() && matchPresetsDir.isDirectory()) {
            for (File worldDir : Objects.requireNonNull(matchPresetsDir.listFiles())) {
                if (worldDir.isDirectory()) {
                    for (File file : Objects.requireNonNull(worldDir.listFiles())) {
                        if (!file.getName().equals("region")) {
                            try {
                                if (file.isDirectory()) {
                                    FileUtils.deleteDirectory(file);
                                } else {
                                    if (!file.delete()) {
                                        throw new IOException("Failed to delete file: " + file.getName());
                                    }
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to clean matches-preset directory", e);
                            }
                        }
                    }
                }
            }
        } else {
            if (!matchPresetsDir.mkdirs()) {
                throw new RuntimeException("Failed to create matches-preset directory");
            }
        }

        // ensure the cages directory exists
        File cageDirectory = new File(XinCraftPlugin.get().getDataFolder() + File.separator + "cages");
        if (!cageDirectory.exists()) {
            if (!cageDirectory.mkdirs()) {
                throw new RuntimeException("Failed to create cages directory");
            }
        }

        mapConfigDir = new File(XinCraftPlugin.get().getDataFolder() + File.separator + "maps");
        loadMaps();
    }

    public static @NotNull String getMatchDir(int id) {
        return BridgeMatchManager.MATCHES_DIRECTORY + File.separator + "match-" + id;
    }

    public void loadMaps() {
        loadedMaps = new ArrayList<>();

        // load all configs and pray a map world preset exists todo: handle this better
        for (File file : Objects.requireNonNull(mapConfigDir.listFiles())) {

            BridgeMap bridgeMap = BridgeMap.readJson(new File(mapConfigDir + File.separator + file.getName()));
            loadedMaps.add(bridgeMap);

            if (bridgeMap.isEnabled()) {
                enabledMaps.add(bridgeMap);
            }
        }
    }

    // do I need this?
    public void close() {
        // delete existing matches folder
        try {
            FileUtils.deleteDirectory(new File(MATCHES_DIRECTORY));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // if the world starts with preset, unload it
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith(MATCHES_PRESET_DIRECTORY)) {
                Bukkit.unloadWorld(world, true);
            }
        }
    }

    public void queue(Player player, TeamConfiguration teamConfiguration) {
        queue(player, null, teamConfiguration, false);
    }

    public void queue(Player player, BridgeMap map, TeamConfiguration teamConfiguration, boolean competitive) {
        Party party = XinCraftPlugin.get().getPartyManager().getParty(player);
        // if the player is in a party
        if (party != null) {
            if (!party.isOwner(player)) {
                player.sendMessage(ChatColor.RED + "You must be the party leader to queue!");
                return;
            }
            // if all members aren't in the lobby, tell the member to join the lobby and notify the owner we cant queue
            boolean anyNotLobby = false;
            for (Player member : party.getPlayers()) {
                if (!member.getWorld().getName().equals(LobbyWorld.SPAWN.getWorld().getName())) {
                    member.sendMessage(ChatColor.RED + "Your party leader has tried to queue a match but you are not in the lobby!");
                    anyNotLobby = true;
                }
            }

            if (anyNotLobby) {
                player.sendMessage(ChatColor.RED + "All party members must be in the lobby to queue!");
                return;
            }
        }


        BridgeMatch queueMatch = null;

        // find a match that isn't full
        for (BridgeMatch match : matches.values()) {
            if (match.getStatus() != MatchStatus.QUEUEING) {
                continue;
            }

            if (match.isFull()) {
                continue;
            }

            // if the ongoing match does not have the same team configuration, skip
            if (!match.getTeamHandler().getConfig().compatibleWith(teamConfiguration)) {
                continue;
            }

            // if the match has room for all party members
            if (party != null && match.getPlayers().size() + party.getPlayers().size() > match.getTeamHandler().getConfig().getMaxPlayers()) {
                continue;
            }

            if (map != null && !match.getMap().getName().equals(map.getName())) {
                System.out.println("Map mismatch: " + match.getMap().getName() + " != " + map.getName());
                continue;
            }

            if (competitive && !match.getMap().getType().equals(BridgeMapType.COMPETITIVE)) {
                continue;
            }

            queueMatch = match;
            break;
        }

        // if no match was found, create a new one
        if (queueMatch == null) {
            if (map == null) {
                queueMatch = matchMake(teamConfiguration, competitive);
            } else {
                queueMatch = matchMake(map, teamConfiguration);
            }
        }

        // if in a party, add all the party members
        if (party != null) {
            // if the party size is the same as max players, set save stats to false
            queueMatch.setSaveStats(party.getPlayers().size() != queueMatch.getTeamHandler().getConfig().getMaxPlayers());
            party.getPlayers().forEach(queueMatch::addPlayer);
            return;
        }

        queueMatch.addPlayer(player);
    }

    public BridgeMatch matchMake(TeamConfiguration teamConfiguration, boolean competitive) {
        // randomly select a map from loadedMaps

        BridgeMap map = competitive ? getRandomMap(BridgeMapType.COMPETITIVE) : getRandomMap();
        return matchMake(map, teamConfiguration);
    }

    public BridgeMap getRandomMap() {
        return enabledMaps.get(random.nextInt(enabledMaps.size()));
    }

    public BridgeMap getRandomMap(BridgeMapType type) {
        List<BridgeMap> maps = new ArrayList<>();
        for (BridgeMap map : enabledMaps) {
            if (map.getType() == type) {
                maps.add(map);
            }
        }
        return maps.get(random.nextInt(maps.size()));
    }

    public BridgeMatch matchMake(BridgeMap map, TeamConfiguration teamConfiguration) {
        System.out.println("Making match with map: " + map.getName() + " and team configuration: " + teamConfiguration);
        BridgeMatch match = new BridgeMatch(++id, map, teamConfiguration);
        matches.put(id, match);
        return match;
    }

    public BridgeMatch getMatch(int id) {
        return matches.get(id);
    }

    public void removeMatch(BridgeMatch match) {
        File directory = new File(match.getWorld().getWorldFolder().getPath());
        Bukkit.unloadWorld(match.getWorld(), false);

        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        XinCraftPlugin.get().getWorldManagers().remove(match);
        matches.remove(match.getId());

        XinCraftPlugin.get().getSidebarUpdater().updateLobby();
    }

    public void debug(Player player) {
        player.sendMessage("Current matches: " + matches.size());
        matches.values().forEach(match ->
                player.sendMessage("   - Match: " + match.getId() + " Players (" + match.getPlayers().size() + ") " + match.getWorld().getPlayers()));
    }

    public int getPlayerCount() {
        int count = 0;
        for (BridgeMatch match : matches.values()) {
            count += match.getPlayers().size();
        }
        return count;
    }

    public int getPlaying(TeamConfiguration config) {
        int count = 0;
        for (BridgeMatch match : matches.values()) {
            if (match.getStatus() == MatchStatus.PLAYING && match.getTeamHandler().getConfig().compatibleWith(config)) {
                count += match.getPlayers().size();
            }
        }
        return count;
    }

    public int getQueueing(TeamConfiguration config) {
        int count = 0;
        for (BridgeMatch match : matches.values()) {
            if (match.getStatus() == MatchStatus.QUEUEING && match.getTeamHandler().getConfig().compatibleWith(config)) {
                count += match.getPlayers().size();
            }
        }
        return count;
    }
}
