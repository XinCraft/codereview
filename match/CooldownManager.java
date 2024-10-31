package net.xincraft.systems.match;

import net.xincraft.systems.match.countdown.ArrowCountdown;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<UUID, Instant> map = new HashMap<>();

    @Getter private final Map<UUID, ArrowCountdown> countdowns = new HashMap<>();

    // 3.5 seconds except we can't use floats
    public static final int ARROW_COOLDOWN = 3500;

    // Set cooldown
    public void setCooldown(UUID key, Duration duration) {
        map.put(key, Instant.now().plus(duration));
    }

    // Check if cooldown has expired
    public boolean hasCooldown(UUID key) {
        Instant cooldown = map.get(key);
        return cooldown != null && Instant.now().isBefore(cooldown);
    }

    public void endCountdown(UUID key) {
        ArrowCountdown countdown = countdowns.get(key);
        if (countdown != null) {
            countdown.cancel();
            countdown.end();
            countdowns.remove(key);
        }
    }
}
