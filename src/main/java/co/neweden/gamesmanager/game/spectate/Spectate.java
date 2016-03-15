package co.neweden.gamesmanager.game.spectate;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.GamesManager;
import co.neweden.gamesmanager.Util;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;
import co.neweden.gamesmanager.event.GMPlayerPreJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerSpectatingEvent;
import co.neweden.gamesmanager.game.spectate.SpectateVanishEvents;

public class Spectate implements Listener {
	
	protected Game game;
	private Boolean enableSpec = false;
	private Boolean specOnDeath = false;
	protected Set<Player> spectators;
	
	public Spectate(Game game) {
		this.game = game;
		spectators = new HashSet<Player>();
		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
		new SpectateVanishEvents(this);
	}
	
	public boolean playersCanSpectate() {
		return true;
	}
	
	public void enableSpectateMode() { enableSpec = true; }
	public void disableSpectateMode() { enableSpec = false; }
	
	public boolean isSpectateModeEnabled() { return enableSpec; }
	
	public void playersSpectateOnDeath(Boolean spectateOnDeath) {
		specOnDeath = spectateOnDeath;
	}
	
	public Set<Player> getSpectators() { return spectators; }
	
	@EventHandler
	public void onPreJoinGame(GMPlayerPreJoinGameEvent event) {
		if (!event.isCancelled() &&
			event.getGame().equals(game) &&
			enableSpec &&
			!playersCanSpectate())
		{
			event.setKickMessage("You cannot join the game as the game is in progress mode and it is not possible to spectate this game.");
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoinGame(GMPlayerJoinGameEvent event) {
		if (!event.isCancelled() &&
			event.getGame().equals(game) &&
			enableSpec &&
			playersCanSpectate())
		{
			refreshHiddenPlayers();
			activateSpectate(event.getPlayer());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLeaveGame(GMPlayerLeaveGameEvent event) {
		if (!event.isCancelled() &&
			event.getGame().equals(game) &&
			spectators.contains(event.getPlayer()))
			{
				deactivateSpectate(event.getPlayer());
			}
	}
	
	private Set<Player> respawnKick = new HashSet<Player>();
	private Set<Player> respawnSpec = new HashSet<Player>();
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(final PlayerDeathEvent event) {
		if (!game.getPlayers().contains(event.getEntity()) ||
			!enableSpec ||
			!specOnDeath) return;
		
		if (playersCanSpectate()) {
			respawnSpec.add(event.getEntity());
		} else {
			respawnKick.add(event.getEntity());
		}
		new BukkitRunnable() {
			@Override public void run() {
				event.getEntity().spigot().respawn();
			}
		}.runTaskLater(game.getPlugin(), 1L);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		if (respawnKick.contains(event.getPlayer())) {
			respawnKick.remove(event.getPlayer());
			new BukkitRunnable() {
				@Override public void run() {
					GamesManager.kickPlayer(event.getPlayer(), "You have been kicked from the game as it is not possible to spectate this game.", game.getName());
				}
			}.runTaskLater(game.getPlugin(), 1L);
		}
		if (respawnSpec.contains(event.getPlayer())) {
			respawnSpec.remove(event.getPlayer());
			event.setRespawnLocation(game.getConfig().getLocation("specspawn"));
			activateSpectate(event.getPlayer());
		}
	}
	
	public void activateSpectate(Player player) {
		GMPlayerSpectatingEvent spectatingEvent = new GMPlayerSpectatingEvent(player, game);
		Bukkit.getServer().getPluginManager().callEvent(spectatingEvent);
		
		if (spectatingEvent.isCancelled()) return;
		
		spectators.add(player);
		player.setAllowFlight(true);
		player.setFlying(true);
		hidePlayerFromGame(player);
		player.sendMessage(Util.formatString("&aYou are now spectatig the game."));
	}
	
	public void deactivateSpectate(Player player) {
		spectators.remove(player);
		player.setFlying(false);
		player.setAllowFlight(false);
		showPlayerToGame(player);
	}
	
	public void refreshHiddenPlayers() {
		for (Player player : spectators) {
			hidePlayerFromGame(player);
		}
	}
	
	public void hidePlayerFromGame(Player player) {
		for (Player target : game.getPlayers()) {
			target.hidePlayer(player);
		}
	}
	
	public void showPlayerToGame(Player player) {
		for (Player target : game.getPlayers()) {
			target.showPlayer(player);
		}
	}
	
}
