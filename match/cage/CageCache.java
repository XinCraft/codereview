package net.xincraft.systems.match.cage;

import org.bukkit.Location;

public class CageCache extends Structure {
    private final Location centrePos;

    public CageCache(Location centrePos) {
        super();
        this.centrePos = centrePos;

        // limitations on cage size
        this.width = 15;
        this.height = 20; // account for weird y positions
        this.depth = 15;
        blocks = new ThinBlock[width * height * depth];

        // load the cache
        load(centrePos);
    }

    public void generate() {
        generate(centrePos);
    }
}
