package net.xincraft.systems.match.map;

import lombok.Getter;

@Getter
public class BlockLimitRule {
    private final int deviation;
    private final int blockLimit;

    public BlockLimitRule(int deviation, int blockLimit) {
        this.deviation = deviation;
        this.blockLimit = blockLimit;
    }
}
