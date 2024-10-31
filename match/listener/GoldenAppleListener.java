package net.xincraft.systems.match.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GoldenAppleListener implements Listener {
    @EventHandler
    public void onEat(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.GOLDEN_APPLE) {
            return;
        }

        // handle our own logic when a player consumes a golden apple
        event.setCancelled(true);

        // we could check if they are in a bridge match or game here, but it is unnecessary as
        // there's no other places where the player can eat a golden apple but will need
        // to be changed if implementing a survival mode or something similar.

        Player player = event.getPlayer();
        // reduce item stack todo: doesnt work
        ItemStack goldenApple = event.getItem().clone();
        goldenApple.setAmount(goldenApple.getAmount() - 1);
        player.getInventory().setItem(player.getInventory().getHeldItemSlot(), goldenApple);

        // copy Hypixel logic and only add absorption if the player doesn't already have it
        player.setHealth(20);
        if (!player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,
                    1200, 0, true, true));
        }
    }
}
