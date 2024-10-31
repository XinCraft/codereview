package net.xincraft.systems.match.map;

import net.xincraft.WorldManager;
import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.BridgeMatchManager;
import net.xincraft.util.Utilities;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class MapEditorWorld implements WorldManager {
    private final String mapName;
    private UUID worldUUID;

    public MapEditorWorld(String mapName) {
        this.mapName = mapName;
    }

    public void load(Player player) {
        String worldName = BridgeMatchManager.MATCHES_PRESET_DIRECTORY + File.separator + mapName;

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage(ChatColor.YELLOW + "Loading bridge editor world for map " + ChatColor.GREEN + mapName);
            WorldCreator creator = new WorldCreator(BridgeMatchManager.MATCHES_PRESET_DIRECTORY + File.separator + mapName);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generatorSettings("2;0;1;");
            creator.generateStructures(false);

            world = creator.createWorld();
            world.setGameRuleValue("randomTickSpeed", "0");
            world.setGameRuleValue("doMobSpawning", "false");
        }

        worldUUID = world.getUID();

        XinCraftPlugin.get().getWorldManagers().add(this);

        World finalWorld = world;
        Bukkit.getScheduler().runTaskLater(XinCraftPlugin.get(), () -> {
            player.sendMessage(ChatColor.YELLOW + "Teleporting you to the map editor world for map " + ChatColor.GREEN + mapName);
            player.teleport(new Location(finalWorld, 0.5, 94, 0.5));
        }, 1);
    }

    public void save(Player player) {
        String worldName = BridgeMatchManager.MATCHES_PRESET_DIRECTORY + File.separator + mapName;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(Utilities.parse(ChatColor.RED + "The map editor world for: " + mapName + " is not loaded!"));
            return;
        }

        XinCraftPlugin.get().getLobbyWorld().setup(player);

        File worldFolder = new File(world.getWorldFolder().getPath());
        Bukkit.unloadWorld(world, true);

        XinCraftPlugin.get().getWorldManagers().remove(this);

        // delete everything but region folder
        for (File file : Objects.requireNonNull(worldFolder.listFiles())) {
            if (!file.getName().equals("region")) {
                if (file.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(file);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    if (!file.delete()) {
                        throw new RuntimeException("Failed to delete file: " + file.getName());
                    }
                }
            }
        }

        player.sendMessage(ChatColor.GREEN + "Successfully unloaded the map editor world for map " + mapName);
    }

    @Override
    public UUID getWorldUUID() {
        return worldUUID;
    }

    @Override
    public Collection<UUID> visiblePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getWorld().getName().startsWith(BridgeMatchManager.MATCHES_PRESET_DIRECTORY))
                .map(Entity::getUniqueId).collect(Collectors.toList());
    }
}
