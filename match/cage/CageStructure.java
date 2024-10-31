package net.xincraft.systems.match.cage;

import com.google.gson.Gson;
import lombok.Getter;
import net.xincraft.XinCraftPlugin;
import org.bukkit.Location;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Getter
public class CageStructure extends Structure {
    private final String name;
    private final int yPosition;

    public CageStructure(String name, int yPosition, int width, int height, int depth) {
        blocks = new ThinBlock[width * height * depth];
        this.name = name;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    @Override
    public void generate(Location centreOfCage) {
        Location position = centreOfCage.clone().subtract((double) width / 2, 0, (double) depth / 2);

        boolean isFlipped = centreOfCage.getYaw() == -180;

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    int index = getIndex(y, z, x);
                    ThinBlock cageBlock = blocks[index];
                    if (cageBlock != null) {
                        int zPosition = isFlipped ? z : (depth - 1 - z);
                        setBlock(position, y, zPosition, x, cageBlock);
                    }
                }
            }
        }
    }

    public void saveToJson() {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(XinCraftPlugin.get().getDataFolder() + File.separator + "cages" + File.separator + name + ".json")) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CageStructure readJson(String name) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(XinCraftPlugin.get().getDataFolder() + File.separator + "cages" + File.separator + name.replace("_", "-") + ".json")) {
            return gson.fromJson(reader, CageStructure.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}