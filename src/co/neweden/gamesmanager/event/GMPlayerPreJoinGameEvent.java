package co.neweden.gamesmanager.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import co.neweden.gamesmanager.Game;

public class GMPlayerPreJoinGameEvent extends Event implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private Game game;
	private boolean cancelled;
	
	public GMPlayerPreJoinGameEvent(Player player, Game game) {
		this.player = player;
		this.game = game;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}
	
	String kickMessage = "You have been kicked from the game.";
	
	public String getKickMessage() { return kickMessage; }
	public void setKickMessage(String message) { kickMessage = message; }
	
	public Game getGame() {
		return game;
	}
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
}
