package net.xincraft.systems.match.hits;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class LastHit {
    private final UUID damager;
    private final Instant time;

    public LastHit(UUID damager, Instant time) {
        this.damager = damager;
        this.time = time;
    }
}
