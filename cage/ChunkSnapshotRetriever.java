package net.xincraft.systems.match.cage;

import org.bukkit.World;
import org.bukkit.ChunkSnapshot;

public class ChunkSnapshotRetriever implements ServerThreadSupplier<ChunkSnapshot> {
    private final World world;
    private final int chunkX;
    private final int chunkZ;
    private final int expected;
    private ChunkSnapshot result;

    public ChunkSnapshotRetriever(World world, int chunkX, int chunkZ, int expected) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.expected = expected;
    }

    @Override
    public void compute() {
        result = world.getChunkAt(chunkX, chunkZ).getChunkSnapshot();
    }

    @Override
    public ChunkSnapshot getResult() {
        return this.result;
    }
}
