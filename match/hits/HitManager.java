package net.xincraft.systems.match.hits;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HitManager {
    // player who's been hit is the UUID, and lasthit contains the damager and the time of hit
    private final Map<UUID, LastHit> map = new HashMap<>();

    /**
     * The amount of time in seconds after being hit where a player void will count as a kill.
     */
    // bearsta said change from 3 to 5
    private static final int SECONDS_TO_VOID_KILL = 5;

    // set recent hit
    public void setHit(UUID victim, UUID damager) {
        map.put(victim, new LastHit(damager, Instant.now().plus(Duration.ofSeconds(SECONDS_TO_VOID_KILL))));
    }

    // Check if cooldown has expired
    public boolean isHitRecent(UUID victim) {
        if (!map.containsKey(victim)) {
            return false;
        }

        Instant time = map.get(victim).getTime();
        return time != null && Instant.now().isBefore(time);
    }

    public LastHit getHit(UUID victim) {
        return map.get(victim);
    }

    public void removeHit(UUID victim) {
        map.remove(victim);
    }
}