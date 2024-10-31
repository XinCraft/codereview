package net.xincraft.systems.match.countdown;

import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.match.CooldownManager;
import net.xincraft.systems.match.match.BridgeMatch;
import net.xincraft.util.Countdown;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArrowCountdown extends Countdown {
    private final BridgeMatch match;
    private final Player player;

    public ArrowCountdown(BridgeMatch match, Player player) {
        super((CooldownManager.ARROW_COOLDOWN / 1000) * 20, 1);
        this.match = match;
        this.player = player;
        start();
    }

    public void run(int count) {
        if (count % 20 == 0) {
            player.setLevel(count / 20);
        }

        if (player.getLevel() == 0) {
            this.cancel();
        }
        player.setExp(0.0125F * (float) count);
    }

    public void end() {
        player.setLevel(0);
        player.setExp(0);
        player.getInventory().setItem(XinCraftPlugin.get().getDatabaseManager().getPlayer(player).getKit().getArrow(), new ItemStack(Material.ARROW));
        player.playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 1.0F, 1.0F);
        match.getArrowManager().getCountdowns().remove(player.getUniqueId());
    }
}
