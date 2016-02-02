package co.neweden.gamesmanager.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import co.neweden.gamesmanager.Game;

public class GMPlayerLeaveGameEvent extends Event implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private Game game;
	private boolean cancelled;
	private boolean noCancel = false;
	
	public GMPlayerLeaveGameEvent(Player player, Game game) {
		this.player = player;
		this.game = game;
	}
	
	public GMPlayerLeaveGameEvent(Player player, Game game, boolean noCancel) {
		this.player = player;
		this.game = game;
		this.noCancel = noCancel;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public Game getGame() {
		return game;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	public void setCancelled(boolean cancel) {
		if (noCancel == true)
			cancelled = cancel;
	}
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
}
