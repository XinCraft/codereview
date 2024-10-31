package net.xincraft.systems.match.match;

public enum MatchStatus {
    QUEUEING,
    PLAYING,
    ENDING,
    ;

    public boolean notOngoing() {
        return this != PLAYING;
    }
}
