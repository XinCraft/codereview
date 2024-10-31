package net.xincraft.systems.match.match;

import lombok.Getter;
import net.xincraft.XinCraftPlugin;
import net.xincraft.systems.party.Party;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TeamHandler {
    private final Map<UUID, BridgeTeam> teams = new HashMap<>();
    @Getter
    private final TeamConfiguration config;

    private final Map<BridgeTeam, List<Player>> teamPlayerMap = new HashMap<>();

    public TeamHandler(TeamConfiguration teamConfig) {
        this.config = teamConfig;
    }

    public BridgeTeam getTeam(Player player) {
        return teams.get(player.getUniqueId());
    }

    public BridgeTeam getTeam(UUID player) {
        return teams.get(player);
    }

    public List<Player> getPlayers(BridgeTeam team) {
        return teamPlayerMap.getOrDefault(team, new ArrayList<>());
    }

    public void setupTeams(List<Player> players, boolean isDuel) {
        BridgeTeam[] bridgeTeams = BridgeTeam.values();

        // if in a private party set teams based on parties, for example everyone from one party will go on red
        // and everyone on the other will go on blue
        if (isDuel) {
            // loop through the players and get their party. if they are the owner, add the party to a list
            List<Party> parties = new ArrayList<>();
            for (Player player : players) {
                Party party = XinCraftPlugin.get().getPartyManager().getParty(player);
                if (party.isOwner(player)) {
                    parties.add(party);
                }
            }

            // get the players in each party
            List<List<Player>> partyPlayers = parties.stream().map(Party::getPlayers).collect(Collectors.toList());

            // set the teams
            for (int i = 0; i < partyPlayers.size(); i++) {
                for (Player player : partyPlayers.get(i)) {
                    this.teams.put(player.getUniqueId(), bridgeTeams[i]);
                    teamPlayerMap.computeIfAbsent(bridgeTeams[i], k -> new ArrayList<>()).add(player);
                }
            }
        } else {
            // setup teams for all players
            int teamIndex = 0;

            for (Player player : players) {
                // team handling
                BridgeTeam bridgeTeam = bridgeTeams[teamIndex];
                this.teams.put(player.getUniqueId(), bridgeTeam);
                teamPlayerMap.computeIfAbsent(bridgeTeam, k -> new ArrayList<>()).add(player);
                // Cycle to the next team
                teamIndex = (teamIndex + 1) % bridgeTeams.length;
            }
        }
    }
}
