package net.xincraft.systems.match.cage;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public abstract class Structure {
    protected ThinBlock[] blocks;

    protected int width;
    protected int height;
    protected int depth;

    protected int getIndex(int y, int z, int x) {
        // slice a cube into floors
        // each floor is width * depth in length into the array, y=0 is the first floor, y=1 is the second floor, etc.
        int floor = (y * depth * width);
        // each row is width in length, z=0 is the first row, z=1 is the second row, etc.
        int row = (z * width);
        // x is the column in the row
        return floor + row + x;
    }

    protected void setBlock(Location position, int y, int z, int x, ThinBlock cageBlock) {
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
    }

    public void load(Location centrePos) {
        Location position = centrePos.clone().subtract((double) width / 2, 0, (double) depth / 2);

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    Block cageBlock = position.getWorld().getBlockAt(
                            position.getBlockX() + x,
                            position.getBlockY() + y,
                            position.getBlockZ() + z);

                    int index = getIndex(y, z, x);

                    blocks[index] = new ThinBlock(cageBlock.getType(), cageBlock.getState().getData());
                }
            }
        }
    }

    public void generate(Location centreOfCage) {
        Location position = centreOfCage.clone().subtract((double) width / 2, 0, (double) depth / 2);

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    int index = getIndex(y, z, x);
                    ThinBlock cageBlock = blocks[index];
                    if (cageBlock != null) {
                        setBlock(position, y, z, x, cageBlock);
                    }
                }
            }
        }
    }
}
