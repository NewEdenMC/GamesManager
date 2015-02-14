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
import co.neweden.gamesmanager.event.GMPlayerPreJoinGameEvent;

public class Spectate implements Listener {
	
	private Game game;
	private Boolean enableSpec = false;
	private Boolean specOnDeath = false;
	
	public Spectate(Game game) {
		this.game = game;
		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
		if (playersCanSpectate() == true) {
			// TODO: Add spectate integration for enableSpec and specOnDeath
		}
	}
	
	public boolean playersCanSpectate() {
		// TODO: Add spectate integration
		return false;
	}
	
	public void enableSpectateMode() {
		enableSpec = true;
		if (playersCanSpectate() == false) return;
		// TODO: Add spectate integration
	}
	
	public void disableSpectateMode() {
		enableSpec = false;
		if (playersCanSpectate() == false) return;
		// TODO: Add spectate integration
	}
	
	public boolean isSpectateModeEnabled() {
		return enableSpec;
	}
	
	public void playersSpectateOnDeath(Boolean spectateOnDeath) {
		specOnDeath = spectateOnDeath;
		if (playersCanSpectate() == false) return;
		// TODO: Add spectate integration
	}
	
	@EventHandler
	public void onPreJoinGame(GMPlayerPreJoinGameEvent event) {
		if (!event.getGame().equals(game)) return;
		if (enableSpec == false) return;
		if (playersCanSpectate() == true) {
			event.getPlayer().sendMessage(Util.formatString("&3You are now spectatig the game, click to teleport."));
			return;
		}
		event.setKickMessage("You cannot join the game as the game is in progress mode and it is not possible to spectate this game.");
		event.setCancelled(true);
	}
	
	private Set<Player> respawnKick = new HashSet<Player>();
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(final PlayerDeathEvent event) {
		if (game.getPlayers().contains(event.getEntity()) == false) return;
		if (enableSpec == false) return;
		if (specOnDeath == false) return;
		if (playersCanSpectate() == false) {
			respawnKick.add(event.getEntity());
			Bukkit.getScheduler().scheduleSyncDelayedTask(game.getPlugin(), new Runnable() {
				@Override
				public void run() {
					PacketPlayInClientCommand in = new PacketPlayInClientCommand(EnumClientCommand.PERFORM_RESPAWN); // Gets the packet class
					EntityPlayer cPlayer = ((CraftPlayer)event.getEntity()).getHandle(); // Gets the EntityPlayer class
					cPlayer.playerConnection.a(in); // Handles the rest of it
				}
			}, 1L);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		if (respawnKick.contains(event.getPlayer()) == false) return;
		respawnKick.remove(event.getPlayer());
		new BukkitRunnable() {
			@Override
			public void run() {
				GamesManager.kickPlayer(event.getPlayer(), "You have been kicked from the game as it is not possible to spectate this game.", game.getName());
			}
		}.runTaskLater(game.getPlugin(), 1L);
	}
	
	public Set<Player> getSpectators() {
		// TODO: Add spectate integration
		return new HashSet<Player>();
	}
	
}
