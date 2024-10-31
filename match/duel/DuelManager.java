package net.xincraft.systems.match.duel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class DuelManager {
    private final Map<UUID, DuelInvite> invites = new HashMap<>();

    public void addInvite(UUID player, DuelInvite invite) {
        // if our inviter has already sent an invitation to someone, notify them the invite is invalid before we remove it
        if (invites.get(player) != null) {
            DuelInvite oldInvite = invites.get(player);
            Player cancelledPlayer = Bukkit.getPlayer(oldInvite.getDuelReceiver());

            if (cancelledPlayer != null) {
                cancelledPlayer.sendMessage(ChatColor.RED + "Your previous duel request has been cancelled.");
            }
        }

        invites.put(player, invite);

        invite.setTimeoutRunnable(new BukkitRunnable() {
            @Override
            public void run() {
                Player expiredPlayer = Bukkit.getPlayer(invite.getDuelReceiver());

                if (expiredPlayer != null) {
                    expiredPlayer.sendMessage(ChatColor.RED + "The duel request you have been sent has expired.");
                }

                Player expiredPlayerSender = Bukkit.getPlayer(player);
                if (expiredPlayerSender != null) {
                    expiredPlayerSender.sendMessage(ChatColor.RED + "The duel request you have sent has expired.");
                }

                invites.remove(player);
            }
        });
    }

    public boolean hasInviteOf(UUID player) {
        return invites.containsKey(player);
    }

    public DuelInvite getInvite(UUID player) {
        DuelInvite invite = invites.get(player);
        if (invite == null) {
            throw new IllegalArgumentException("Player does not have an invite. Use hasInviteOf() to check if they do.");
        }
        return invites.get(player);
    }

    public void removeInvite(UUID player) {
        if (invites.get(player) == null) {
            return;
        }

        System.out.println("Cancelling runnable");
        invites.get(player).getTimeoutRunnable().cancel();
        invites.remove(player);
    }
}
