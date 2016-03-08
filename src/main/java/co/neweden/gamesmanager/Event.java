package co.neweden.gamesmanager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;
import co.neweden.gamesmanager.event.GMPlayerPreJoinGameEvent;

public final class Event implements Listener {
	
	GMMain plugin;
	protected enum EventResponseCode { RETURN, CALLED, CANCEL, CANCELED }
	
	public Event(GMMain instance) {
		this.plugin = instance;
	}
	
	public EventResponseCode joinPlayerToGame(Player player, Game game) {
		GMPlayerPreJoinGameEvent gmprejevent = new GMPlayerPreJoinGameEvent(player, game);
		Bukkit.getServer().getPluginManager().callEvent(gmprejevent);
		
		if (gmprejevent.isCancelled() == true) {
			if (game.worlds().getWorlds().contains(player.getWorld()))
				GamesManager.kickPlayer(player, "You have been kicked from this world as a game has just been started and you are not able to join the game.", game.getName());
			return EventResponseCode.CANCELED;
		}
		
		game.preparePlayer(player);
		
		GMPlayerJoinGameEvent gmjevent = new GMPlayerJoinGameEvent(player, game);
		Bukkit.getServer().getPluginManager().callEvent(gmjevent);
		
		if (gmjevent.isCancelled() == true) {
			game.releasePlayer(player);
			if (game.worlds().getWorlds().contains(player.getWorld()))
				GamesManager.kickPlayer(player, "You have been kicked from this world as a game has just been started and you are not able to join the game.", game.getName());
			return EventResponseCode.CANCELED;
		}
		return EventResponseCode.CALLED;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPreJoin(PlayerJoinEvent event) {
		// TODO: Fix this even so it passes the correct game
		Game game = GamesManager.getGameByWorld(event.getPlayer().getWorld());
		if (game == null) return;
		
		GMPlayerPreJoinGameEvent gmevent = new GMPlayerPreJoinGameEvent(event.getPlayer(), game);
		Bukkit.getServer().getPluginManager().callEvent(gmevent);
		
		if (gmevent.isCancelled() == true) {
			GamesManager.kickPlayer(event.getPlayer(), gmevent.getKickMessage(), game.getName());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		Game game = GamesManager.getGameByWorld(event.getPlayer().getWorld());
		if (game == null) return;
		if (event.getPlayer().isOnline() == false) return; // Sanity check as event can't be cancelled
		game.preparePlayer(event.getPlayer());
		
		GMPlayerJoinGameEvent gmevent = new GMPlayerJoinGameEvent(event.getPlayer(), game);
		Bukkit.getServer().getPluginManager().callEvent(gmevent);
		
		if (gmevent.isCancelled() == true) {
			game.releasePlayer(event.getPlayer());
			GamesManager.kickPlayer(event.getPlayer(), gmevent.getKickMessage(), game.getName());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLeave(PlayerQuitEvent event) {
		if (GamesManager.isPlayerInAnyGame(event.getPlayer())) {
			Game game = GamesManager.getGameByPlayer(event.getPlayer());
			callLeaveEvent(event.getPlayer(), game);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onKick(PlayerKickEvent event) {
		if (event.isCancelled() == true) return;
		if (GamesManager.isPlayerInAnyGame(event.getPlayer())) {
			Game game = GamesManager.getGameByPlayer(event.getPlayer());
			callLeaveEvent(event.getPlayer(), game);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onTeleport(final PlayerTeleportEvent event) {
		if (event.isCancelled() == true) return;
		EventResponseCode switchEvent = prePlayerTeleport(event.getPlayer(), event.getFrom().getWorld(), event.getTo().getWorld());
		if (switchEvent == EventResponseCode.CANCEL)
			event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPortal(final PlayerPortalEvent event) {
		if (event.isCancelled() == true) return;
		EventResponseCode switchEvent = prePlayerTeleport(event.getPlayer(), event.getFrom().getWorld(), event.getTo().getWorld());
		if (switchEvent == EventResponseCode.CANCEL)
			event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		new BukkitRunnable() {
			@Override
			public void run() {
				postPlayerTeleport(event.getPlayer(), event.getPlayer().getWorld());
			}
		}.runTaskLater(plugin, 1L);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onChangedWorld(final PlayerChangedWorldEvent event) {
		postPlayerTeleport(event.getPlayer(), event.getPlayer().getWorld());
	}
	
	private EventResponseCode callPreJoinEvent(Player player, Game game) {
		GMPlayerPreJoinGameEvent gmevent = new GMPlayerPreJoinGameEvent(player, game);
		Bukkit.getServer().getPluginManager().callEvent(gmevent);
		if (gmevent.isCancelled() == true) {
			player.sendMessage(Util.formatString(String.format("&e[&6%s&e] &6%s", game.getName(), gmevent.getKickMessage())));
			return EventResponseCode.CANCEL;
		}
		return EventResponseCode.CALLED;
	}
	
	private EventResponseCode callJoinEvent(Player player, Game game) {
		game.preparePlayer(player);
		GMPlayerJoinGameEvent gmevent = new GMPlayerJoinGameEvent(player, game);
		Bukkit.getServer().getPluginManager().callEvent(gmevent);
		if (gmevent.isCancelled() == true) {
			game.releasePlayer(player);
			player.sendMessage(Util.formatString(String.format("&e[&6%s&e] &6%s", game.getName(), gmevent.getKickMessage())));
			return EventResponseCode.CANCEL;
		}
		return EventResponseCode.CALLED;
	}
	
	private EventResponseCode callLeaveEvent(Player player, Game game) {
		GMPlayerLeaveGameEvent gmevent = new GMPlayerLeaveGameEvent(player, game);
		Bukkit.getServer().getPluginManager().callEvent(gmevent);
		if (gmevent.isCancelled() == false) {
			game.releasePlayer(player);
			return EventResponseCode.CALLED;
		}
		return EventResponseCode.CANCEL;
	}
	
	private EventResponseCode prePlayerTeleport(Player player, World fromWorld, World toWorld) {
		if (fromWorld != null && toWorld != null) {
			if (fromWorld.equals(toWorld))
				return EventResponseCode.RETURN;
		}
		
		Game fromGame = GamesManager.getGameByWorld(fromWorld);
		Game toGame = GamesManager.getGameByWorld(toWorld);
		/*for (Game game : GamesManager.getEnabledGames()) { // loop round each enabled game
			if (game.worlds().getWorlds().contains(fromWorld)) fromGame = game;
			if (game.worlds().getWorlds().contains(toWorld)) toGame = game;
		}*/
		if (fromGame == null && toGame == null) return EventResponseCode.RETURN;
		
		if (toGame == null) { // run if player left game and isn't joining another game
			return callLeaveEvent(player, fromGame);
		}
		
		if (fromGame == null) { // run if player joined game and didn't come from a game
			if (toGame.getPlayers().contains(player))
				return EventResponseCode.RETURN;
			return callPreJoinEvent(player, toGame);
		}
		
		if (fromGame.equals(toGame)) return EventResponseCode.RETURN;
		// Run if player is switching from one game to another
		if (callLeaveEvent(player, fromGame).equals(EventResponseCode.CANCEL))
			return EventResponseCode.CANCEL;
		
		if (toGame.getPlayers().contains(player))
			return EventResponseCode.CALLED;
		
		return callPreJoinEvent(player, toGame);
	}
	
	private EventResponseCode postPlayerTeleport(Player player, World world) {
		// If appropriate checks are performed after calling prePlayerTeleport,
		// this method shouldn't be called, it should be assumed that the player
		// is no longer in any game, but we'll check anyway.
		Game game = GamesManager.getGameByWorld(world);
		if (game == null) return EventResponseCode.RETURN;
		
		if (game.getPlayers().contains(player))
			return EventResponseCode.RETURN;
		else
			return callJoinEvent(player, game);
	}
	
}
