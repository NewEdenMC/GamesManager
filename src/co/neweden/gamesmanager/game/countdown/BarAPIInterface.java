package co.neweden.gamesmanager.game.countdown;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.Util;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;

public class BarAPIInterface implements Listener {
	
	private Game game;
	private String message = "";
	private int duration = 0;
	private Integer countdown = 0;
	private boolean startForGame = false;
	
	public BarAPIInterface(Game game, String message, int duration) {
		this.game = game;
		this.message = message;
		this.duration = duration;
		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	// Duplicate method in Countdown
	public String formatMessage(String message) {
		message = Util.formatString(message);
		message = message.replaceAll("%counter%", countdown.toString());
		message = message.replaceAll("%totalPlaying%", "" + game.getPlaying().size());
		Set<String> playingPlayerNames = new HashSet<String>();
		for (Player player : game.getPlaying()) {
			playingPlayerNames.add(player.getName());
		}
		message = message.replaceAll("%playing%", StringUtils.join(playingPlayerNames, ", "));
		return message;
	}
	
	public void startForGame() {
		startForGame = true;
		/*for (Player player : game.getPlayers()) {
			me.confuser.barapi.BarAPI.setMessage(player, message, duration);
		}*/
		countdown = duration;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (startForGame == false) return;
				for (Player player : game.getPlayers()) {
					float percent = ((float) countdown / duration) * 100;
					try {
						me.confuser.barapi.BarAPI.setMessage(player, formatMessage(message), percent);
					} catch (NullPointerException e) {
						Bukkit.getLogger().warning(String.format("[%s] Cannot create Boss Bar as plugin BarAPI is not installed.", game.getPlugin().getName()));
					}
				}
				if (countdown == 0) {
					for (Player player : game.getPlayers()) {
						try {
							me.confuser.barapi.BarAPI.removeBar(player);
						} catch (NullPointerException e) { }
					}
					startForGame = false;
				} else
					countdown--;
			}
		}.runTaskTimer(game.getPlugin(), 0L, 20L);
	}
	
	@EventHandler
	public void onJoin(GMPlayerJoinGameEvent event) {
		if (startForGame == false) return;
		if (game.getPlayers().contains(event.getPlayer()) == false) return;
		//me.confuser.barapi.BarAPI.setMessage(event.getPlayer(), message, countdown);
	}
	
	@EventHandler
	public void onLeave(GMPlayerLeaveGameEvent event) {
		if (startForGame == false) return;
		if (game.getPlayers().contains(event.getPlayer()) == false) return;
		try {
			me.confuser.barapi.BarAPI.removeBar(event.getPlayer());
		} catch (NullPointerException e) { }
	}
	
}
