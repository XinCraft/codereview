package net.xincraft.systems.match.runnable;

import lombok.Getter;
import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.countdown.PauseableCountdown;
import net.xincraft.systems.match.match.Match;

public class MatchTimer {
    public static final int FINAL_GAME_LENGTH_TICKS = 900;

    @Getter
    private final PauseableCountdown countdown;

    public MatchTimer(Match match, int gameLengthTicks) {
        countdown = new PauseableCountdown(gameLengthTicks, 20) {
            @Override
            public void run(int count) {
                // update all player scoreboards with updated time left
                XinCraftPlugin.get().getSidebarUpdater().update(match.getWorld());

                if (count > FINAL_GAME_LENGTH_TICKS) {
                    throw new RuntimeException("Game length ticks exceeded final game length ticks");
                }
            }

            @Override
            public void end() {
                match.handleTimeFail();
            }
        };
    }

    public void start() {
        countdown.start();
    }
}
