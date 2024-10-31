package net.xincraft.systems.match.map;

import lombok.Getter;
import net.xincraft.systems.match.match.BridgeTeam;
import org.bukkit.Location;
import org.bukkit.World;

@Getter
public class Position {
    private final int x;
    private final int y;
    private final int z;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location toLocation(World world) {
        // centring and location
        return new Location(world, x + 0.5, y, z + 0.5, -180, 0);
    }

    public Location toTeamSpawn(World world, BridgeTeam bridgeTeam) {
        return new Location(world, x + 0.5, y, z + 0.5, bridgeTeam.getFacingYaw(), 0);
    }
}
