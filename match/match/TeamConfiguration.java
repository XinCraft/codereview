package net.xincraft.systems.match.match;

import net.xincraft.database.data.stats.StatsType;
import lombok.Getter;

@Getter
public class TeamConfiguration {
    public static final TeamConfiguration SOLOS = new TeamConfiguration(1, 2);
    public static final TeamConfiguration DOUBLES = new TeamConfiguration(2, 2);
    public static final TeamConfiguration THREES = new TeamConfiguration(3, 2);
    public static final TeamConfiguration FOURS = new TeamConfiguration(4, 2);

    private final int teamSize;
    private final int teamCount;

    public TeamConfiguration(int teamSize, int teamCount) {
        this.teamSize = teamSize;
        this.teamCount = teamCount;
    }

    public int getMaxPlayers() {
        return teamSize * teamCount;
    }

    public boolean compatibleWith(Object obj) {
        TeamConfiguration that = (TeamConfiguration) obj;
        return teamSize == that.teamSize && teamCount == that.teamCount;
    }

    public StatsType getStatsType() {
        return StatsType.values()[teamSize];
    }
}
