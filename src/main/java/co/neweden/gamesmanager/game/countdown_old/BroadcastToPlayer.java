package co.neweden.gamesmanager.game.countdown_old;

import org.bukkit.entity.Player;

public class BroadcastToPlayer {
	
	Player player;
	String message;
	
	BroadcastToPlayer(Player player, String message) {
		this.player = player;
		this.message = message;
	}
	
	public Player getPlayer() { return this.player; }
	public String getMessage() { return this.message; }
	
}
