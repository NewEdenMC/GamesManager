package co.neweden.gamesmanager.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;

public class ReservedSlots implements Listener {
	
	private Game game;
	@SuppressWarnings("unused")
	private boolean rsEnabled = false;
	@SuppressWarnings("unused")
	private boolean onlyKickSpec = false;
	private List<Player> players = new ArrayList<Player>();
	private boolean onlyKickSpectators;
	
	public ReservedSlots(Game game) {
		this.game = game;
		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public void enable() { rsEnabled = true; }
	public void disable() { rsEnabled = false; }
	public void onlyKickSpectators(Boolean kickSpectators) { this.onlyKickSpec = true; }
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onJoin(GMPlayerJoinGameEvent event) {
		if (event.getGame().equals(game) == false)
			return;
		
		players.add(event.getPlayer());
		
		if (rsEnabled = false || event.getPlayer().hasPermission("gamesmanager.reservedslot"))
			return;
		
		if (game.getMaxPlayerCount() > 0 && game.getPlayers().size() > game.getMaxPlayerCount()) {
			Bukkit.broadcastMessage(nextPlayerToKick().toString());
			if (nextPlayerToKick() == null) {
				game.kickPlayer(event.getPlayer(), "There is no room left in this game :(");
			} else {
				game.kickPlayer(nextPlayerToKick(), "You have been kicked from the game because the game is full and someone with a reserved slot has joined just after you.");
			}
		}
	}
	
	@EventHandler
	public void onLeave(GMPlayerLeaveGameEvent event) {
		if (players.contains(event.getPlayer()))
			players.remove(event.getPlayer());
	}
	
	public Player nextPlayerToKick() {
		if (players.isEmpty()) return null;
		if (players.size() <= 1) return null;
		// Loop backwards through players that have joined, and skip the newest player (likely the player who just joined)
		Bukkit.broadcastMessage((players.size() - 2) + ": " + players.toString());
		for (int i = players.size() - 2; i < 0; i--) {
			if (players.get(i).hasPermission("gamesmanager.reservedslot"))
				continue;
			if (onlyKickSpectators == true) {
				if (game.spectate().getSpectators().contains(players.get(i)) == false)
					// skip this player if they are aren't a spectator
					continue;
			}
			// Return current player if they were not skipped above
			return players.get(i);
		}
		return null;
	}
	
}
