package co.neweden.gamesmanager.game;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;

public class Statistics implements Listener {
	
	private Game game;
	private Boolean listen = false;
	
	public Statistics(Game game) {
		this.game = game;
		Bukkit.getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public void startListening() { listen = true; }
	public void stopListening() { listen = false; }
	
	private HashMap<Player, Long> joinedAt = new HashMap<Player, Long>();
	public HashMap<Player, Long> getAllJoinTimes() { return joinedAt; }
	
	private HashMap<Player, Long> leftAt = new HashMap<Player, Long>();
	public HashMap<Player, Long> getAllLeaveTimes() { return leftAt; }
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onJoinGame(GMPlayerJoinGameEvent event) {
		if (listen == false) return;
	if (game.getPlaying().contains(event.getPlayer()) == false) return;
		joinedAt.put(event.getPlayer(), System.currentTimeMillis());
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onLeaveGame(GMPlayerLeaveGameEvent event) {
		if (listen == false) return;
		if (game.getPlaying().contains(event.getPlayer()) == false) return;
		leftAt.put(event.getPlayer(), System.currentTimeMillis());
	}
	
	public Long getJoinTime(Player player) {
		if (joinedAt.containsKey(player)) {
			return joinedAt.get(player);
		} else {
			return 0L;
		}
	}
	
	public Long getLeaveTime(Player player) {
		if (leftAt.containsKey(player)) {
			return leftAt.get(player);
		} else {
			return 0L;
		}
	}
	
	public HashMap<Player, Long> getCurrentPlayTimes() {
		HashMap<Player, Long> times = new HashMap<Player, Long>();
		for (Player player : joinedAt.keySet()) {
			times.put(player, getCurrentPlayTime(player));
		}
		return times;
	}
	
	public Long getCurrentPlayTime(Player player) {
		if (joinedAt.containsKey(player)) {
			Long joinTime = joinedAt.get(player);
			Long leftTime = System.currentTimeMillis();
			if (leftAt.containsKey(player)) {
				leftTime = leftAt.get(player);
			}
			return leftTime - joinTime;
		} else {
			return 0L;
		}
	}
	
	private HashMap<Player, Integer> kills = new HashMap<Player, Integer>();
	public HashMap<Player, Integer> getAllTotalKills() { return kills; }
	
	private HashMap<Player, Integer> deaths = new HashMap<Player, Integer>();
	public HashMap<Player, Integer> getAllTotalDeaths() { return deaths; }
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onDeath(PlayerDeathEvent event) {
		if (listen == false) return;
		if (game.getPlaying().contains(event.getEntity()) == false) return;
		
		if (kills.containsKey(event.getEntity().getKiller())) {
			kills.put(event.getEntity().getKiller(), kills.get(event.getEntity().getKiller()) + 1);
		} else {
			kills.put(event.getEntity().getKiller(), 1);
		}
		
		if (deaths.containsKey(event.getEntity())) {
			deaths.put(event.getEntity(), kills.get(event.getEntity().getKiller()) + 1);
		} else {
			deaths.put(event.getEntity(), 1);
		}
	}
	
	public Integer getTotalKills(Player player) {
		if (kills.containsKey(player)) {
			return kills.get(player);
		} else {
			return 0;
		}
	}
	
	public Integer getTotalDeaths(Player player) {
		if (deaths.containsKey(player)) {
			return deaths.get(player);
		} else {
			return 0;
		}
	}
	
}
