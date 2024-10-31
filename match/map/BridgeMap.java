package net.xincraft.systems.match.map;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.match.BridgeTeam;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// hypixel:
// bridge is 9 blocks tall and can build up 7 blocks
// bridge length is always 20 so it goes 20BLUE 1WHITE 20RED
@Getter
public class BridgeMap {
    private final String name;
    @Setter
    private Material icon;
    private final Position joinSpawnPoint;
    private final Map<BridgeTeam, Position> teamSpawnpoints;
    @Setter private BridgeMapType type;

    @Setter private boolean enabled = false;

    /**
     * The amount of blocks past the bridge in the direction of the goal where the player can place blocks.
     * For example: On Urban the player can place 6 blocks past the bridge towards the goal and no more,
     * so this value would be set to 6.
     */
    private final int blockLimit;

    private final List<BlockLimitRule> advancedBlockLimitRules;

    public BridgeMap(String name, Material icon, int blockLimit, Position joinSpawnPoint, Map<BridgeTeam, Position> teamSpawnpoints) {
        this.name = name;
        this.icon = icon;
        this.blockLimit = blockLimit;
        this.joinSpawnPoint = joinSpawnPoint;
        this.teamSpawnpoints = teamSpawnpoints;
        this.advancedBlockLimitRules = new ArrayList<>();
    }

    public void saveToJson() {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(XinCraftPlugin.get().getDataFolder() + File.separator + "maps" + File.separator + name + ".json")) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BridgeMap readJson(File file) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, BridgeMap.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String formattedMapName() {
        // we could cache this for performance reasons?
        // replaced "-" with " " and capitalised the first letter of each word
        return WordUtils.capitalize(name.replace("-", " "));
    }
}
