package net.xincraft.systems.match.countdown;

import lombok.Getter;
import lombok.Setter;
import net.xincraft.util.Countdown;

@Setter
@Getter
public abstract class PauseableCountdown extends Countdown {
    private boolean paused = false;

    public PauseableCountdown(int count, int speed) {
        super(count, speed);
    }

    @Override
    public void run() {
        if (paused) {
            return;
        }

        super.run();
    }
}
