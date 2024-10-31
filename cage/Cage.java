package net.xincraft.systems.match.cage;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.xincraft.XinCraftPlugin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Getter
@NoArgsConstructor
public class Cage {
    @Expose
    @SerializedName("cageBlocks")
    public CageBlock[] cageBlocks;

    @Expose(serialize = false, deserialize = false)
    private CageBlock[] cache;

    @Expose
    @SerializedName("name")
    private String name;

    @Expose
    @SerializedName("width")
    private int width;

    @Expose
    @SerializedName("height")
    private int height;

    @Expose
    @SerializedName("depth")
    private int depth;

    @Expose
    @SerializedName("yposition")
    private int yPosition;

    public Cage(String name, int yPosition, int width, int height, int depth) {
        cageBlocks = new CageBlock[width * height * depth];
        this.name = name;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public int size() {
        return cageBlocks.length;
    }

    public void cache(Location centreOfCage) {
        cache = new CageBlock[width * height * depth];

        Location position = centreOfCage.clone().subtract((double) width / 2, 0, (double) depth / 2);

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    Block cageBlock = position.getWorld().getBlockAt(
                            position.getBlockX() + x,
                            position.getBlockY() + y,
                            position.getBlockZ() + z);

                    int index = getIndex(y, z, x);

                    cache[index] = new CageBlock(cageBlock.getType(), cageBlock.getState().getData());
                }
            }
        }
    }

    public void generate(Location centreOfCage) {
        Location position = centreOfCage.clone().subtract((double) width / 2, 0, (double) depth / 2);

        boolean isFlipped = centreOfCage.getYaw() == -180;

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    int index = getIndex(y, z, x);
                    CageBlock cageBlock = cageBlocks[index];
                    if (cageBlock != null) {
                        int zPosition = isFlipped ? z : (depth - 1 - z);
                        setBlock(position, y, zPosition, x, cageBlock);
                    }
                }
            }
        }
    }

    public void resetToCache(Location centreOfCage) {
        Location position = centreOfCage.clone().subtract((double) width / 2, 0, (double) depth / 2);

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    int index = getIndex(y, z, x);
                    CageBlock cageBlock = cache[index];
                    if (cageBlock != null) {
                        setBlock(position, y, z, x, cageBlock);
                    }
                }
            }
        }
    }

    private int getIndex(int y, int z, int x) {
        // slice a cube into floors
        // each floor is width * depth in length into the array, y=0 is the first floor, y=1 is the second floor, etc.
        int floor = (y * depth * width);
        // each row is width in length, z=0 is the first row, z=1 is the second row, etc.
        int row = (z * width);
        // x is the column in the row
        return floor + row + x;
    }

    private void setBlock(Location position, int y, int z, int x, CageBlock cageBlock) {
        Bukkit.getScheduler().runTask(XinCraftPlugin.get(), () -> {
            Block block = position.getWorld().getBlockAt(
                    position.getBlockX() + x,
                    position.getBlockY() + y,
                    position.getBlockZ() + z
            );

            block.setType(cageBlock.getType(), false);

            BlockState state = block.getState();
            state.setData(cageBlock.getMaterialData());

            if (!state.update(true, false)) {
                throw new RuntimeException("failed to update block at " + block.getLocation());
            }
        });
    }

    public void saveToJson() {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(XinCraftPlugin.get().getDataFolder() + File.separator + "cages" + File.separator + name + ".json")) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Cage readJson(String name) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(XinCraftPlugin.get().getDataFolder() + File.separator + "cages" + File.separator + name.replace("_", "-") + ".json")) {
            return gson.fromJson(reader, Cage.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}