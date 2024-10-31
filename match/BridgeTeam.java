package net.xincraft.systems.match.match;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Color;

@Getter
public enum BridgeTeam {
    BLUE(ChatColor.BLUE, 0, (byte) 11, Color.BLUE),
    RED(ChatColor.RED, -180, (byte) 14, Color.RED);

    private final ChatColor chatColour;
    private final int facingYaw;
    private final byte clayBlock;
    private final Color armourColour;

    private final String letter;

    BridgeTeam(ChatColor chatColour, int facingYaw, byte clayBlock, Color armourColour) {
        this.chatColour = chatColour;
        this.facingYaw = facingYaw;
        this.clayBlock = clayBlock;
        this.armourColour = armourColour;
        // set letter to the first letter of the enum name
        letter = name().substring(0, 1);
    }
}
