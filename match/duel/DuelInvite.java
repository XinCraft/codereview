package net.xincraft.systems.match.duel;

import lombok.Getter;
import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.map.BridgeMap;
import net.xincraft.systems.match.match.TeamConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

@Getter
public class DuelInvite {
    private final UUID duelReceiver;
    private final BridgeMap map;
    private final TeamConfiguration teamConfiguration;
    private BukkitRunnable timeoutRunnable;

    public DuelInvite(UUID duelReceiver, BridgeMap map, TeamConfiguration teamConfiguration) {
        this.map = map;
        this.teamConfiguration = teamConfiguration;
        this.duelReceiver = duelReceiver;
    }

    public void setTimeoutRunnable(BukkitRunnable runnable) {
        this.timeoutRunnable = runnable;
        timeoutRunnable.runTaskLater(XinCraftPlugin.get(), 60 * 20);
    }
}
