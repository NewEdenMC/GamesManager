package co.neweden.gamesmanager.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;
import co.neweden.gamesmanager.event.GMPlayerSpectatingEvent;

public class Statistics implements Listener {
	
	private Game game;
	private Boolean listen = false;
	
	public Statistics(Game game) {
		this.game = game;
		Bukkit.getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public void startListening() {
		for (Player player : game.getPlaying()) {
			if (!joinedAt.containsKey(player))
				joinedAt.put(player, System.currentTimeMillis());
		}
		listen = true;
	}
	
	public void stopListening() {
		listen = false;
	}
	
	private HashMap<Player, Long> joinedAt = new HashMap<Player, Long>();
	public HashMap<Player, Long> getAllJoinTimes() { return joinedAt; }
	
	private HashMap<Player, Long> leftAt = new HashMap<Player, Long>();
	public HashMap<Player, Long> getAllLeaveTimes() { return leftAt; }
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onJoinGame(GMPlayerJoinGameEvent event) {
		if (listen && game.getPlaying().contains(event.getPlayer())) {
			joinedAt.put(event.getPlayer(), System.currentTimeMillis());
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onLeaveGame(GMPlayerLeaveGameEvent event) {
		if (listen && game.getPlaying().contains(event.getPlayer())) {
			leftAt.put(event.getPlayer(), System.currentTimeMillis());
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onPlayerSpectating(GMPlayerSpectatingEvent event) {
		if (listen && game.getPlaying().contains(event.getPlayer())) {
			leftAt.put(event.getPlayer(), System.currentTimeMillis());
		}
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
		if (!listen || !game.getPlaying().contains(event.getEntity())) return;
		
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
	
	public TreeMap<Long, ArrayList<Player>> groupAndSort(Map<Player, Long> map) { return groupAndSort(map, true); }
	public TreeMap<Long, ArrayList<Player>> groupAndSort(Map<Player, Long> map, boolean reverseOrder) {
		TreeMap<Long, ArrayList<Player>> groups;
		if (reverseOrder)
			groups = new TreeMap<>(Collections.reverseOrder());
		else
			groups = new TreeMap<>();
		
		for (Entry<Player, Long> e : map.entrySet()) {
			if (groups.containsKey(e.getValue()))
				// If the players statistic value is in the Map, then just add them to the inner List
				groups.get(e.getValue()).add(e.getKey());
			else
				// If the players statics value isn't in the Map, add the value and a new List with that player
				groups.put(e.getValue(), new ArrayList<>(Collections.singletonList(e.getKey())));
		}
		
		return groups;
	}
	
	private String readablePlace(Integer num) {
		if (num.equals(11) || num.equals(12) || num.equals(13))
			return num + "th";
		String place;
		switch (num % 10) {
			case 1: place = "&e" + num + "st"; break;
			case 2: place = "&6" + num + "nd"; break;
			case 3: place = "&c" + num + "rd"; break;
			default: place = num + "th"; break;
		}
		return place;
	}
	
	public void renderTopList(HashMap<Player, Long> data, int numToList) { renderTopList(data, numToList, null); }
	public void renderTopList(HashMap<Player, Long> data, int numToList, Player forceFirst) { renderTopList(data, numToList, false, forceFirst); }
	public void renderTopList(HashMap<Player, Long> data, int numToList, boolean reverseOrder) { renderTopList(data, numToList, false, null); }
	public void renderTopList(HashMap<Player, Long> data, int numToList, boolean reverseOrder, Player forceFirst) {
		if (forceFirst != null) data.remove(forceFirst);
		TreeMap<Long, ArrayList<Player>> groups = groupAndSort(data, reverseOrder);
		if (forceFirst != null)
			groups.put(groups.firstKey() + 1, new ArrayList<>(Collections.singletonList(forceFirst)));
		
		game.broadcast("&2&a\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580");
		game.broadcast("");
		game.broadcast("&bThe game has ended, the top three players are:");
		
		int place = 1;
		for (Entry<Long, ArrayList<Player>> group : groups.entrySet()) {
			String s = ""; int i = 0;
			for (Player player : group.getValue()) {
				 s += player.getName();
				if (group.getValue().size() - 2 == i)
					s += " and ";
				else
					if (group.getValue().size() - 1 != i) s += ", ";
				i++;
			}
			game.broadcast(String.format("%s place %s", readablePlace(place), s));
			place++;
			// Stop the loop if we shouldn't list any more places after this one
			if (place > numToList) break;
		}
		
		game.broadcast("");
		game.broadcast("&2&a\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580\u2580");
	}
	
}
