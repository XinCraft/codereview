package net.xincraft.systems.match.cage;

import lombok.Getter;
import net.xincraft.systems.match.match.BridgeTeam;

@Getter
public class ScorerPlayer {
    private final String name;
    private final BridgeTeam team;

    public ScorerPlayer(String name, BridgeTeam team) {
        this.name = name;
        this.team = team;
    }
}
