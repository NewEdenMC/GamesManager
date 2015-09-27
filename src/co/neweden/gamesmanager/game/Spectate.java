package co.neweden.gamesmanager.game;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.server.v1_8_R1.EntityPlayer;
import net.minecraft.server.v1_8_R1.EnumClientCommand;
import net.minecraft.server.v1_8_R1.PacketPlayInClientCommand;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
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

public class Spectate implements Listener {
	
	private Game game;
	private Boolean enableSpec = false;
	private Boolean specOnDeath = false;
	private Set<Player> spectators;
	
	public Spectate(Game game) {
		this.game = game;
		spectators = new HashSet<Player>();
		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
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
		if (event.isCancelled() == false &&
			event.getGame().equals(game) &&
			enableSpec == true &&
			playersCanSpectate() == false)
		{
			event.setKickMessage("You cannot join the game as the game is in progress mode and it is not possible to spectate this game.");
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoinGame(GMPlayerJoinGameEvent event) {
		if (event.isCancelled() == false &&
			event.getGame().equals(game) &&
			enableSpec == true &&
			playersCanSpectate() == true)
		{
			activateSpectate(event.getPlayer());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLeaveGame(GMPlayerLeaveGameEvent event) {
		if (event.isCancelled() == false &&
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
		if (game.getPlayers().contains(event.getEntity()) == false ||
			enableSpec == false ||
			specOnDeath == false) return;
		
		if (playersCanSpectate() == true) {
			respawnSpec.add(event.getEntity());
		} else {
			respawnKick.add(event.getEntity());
		}
		new BukkitRunnable() {
			@Override public void run() {
				PacketPlayInClientCommand in = new PacketPlayInClientCommand(EnumClientCommand.PERFORM_RESPAWN); // Gets the packet class
				EntityPlayer cPlayer = ((CraftPlayer)event.getEntity()).getHandle(); // Gets the EntityPlayer class
				cPlayer.playerConnection.a(in); // Handles the rest of it
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
			event.setRespawnLocation(game.getSpecSpawnLocation());
			activateSpectate(event.getPlayer());
		}
	}
	
	public void activateSpectate(Player player) {
		GMPlayerSpectatingEvent spectatingEvent = new GMPlayerSpectatingEvent(player, game);
		Bukkit.getServer().getPluginManager().callEvent(spectatingEvent);
		
		if (spectatingEvent.isCancelled() == true) return;
		
		spectators.add(player);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.hidePlayer(player);
		player.sendMessage(Util.formatString("&aYou are now spectatig the game."));
	}
	
	public void deactivateSpectate(Player player) {
		spectators.remove(player);
		player.setFlying(false);
		player.setAllowFlight(false);
	}
	
	public void refreshHiddenPlayers() {
		
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
