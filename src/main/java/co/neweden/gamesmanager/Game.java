package co.neweden.gamesmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import co.neweden.gamesmanager.game.config.MultiConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import co.neweden.gamesmanager.game.FreezePlayers;
import co.neweden.gamesmanager.game.ReservedSlots;
import co.neweden.gamesmanager.game.spectate.Spectate;
import co.neweden.gamesmanager.game.Statistics;
import co.neweden.gamesmanager.game.WorldsManager;
import co.neweden.gamesmanager.game.countdown.CMain;
import co.neweden.gamesmanager.gametype.HungerGames;
import co.neweden.gamesmanager.gametype.HungerGames_Dev;

public class Game {
	
	private String gameName;
	private Set<Player> players = new HashSet<Player>();
	private String mapName = "default";
	
	public Game(String gameName) {
		this.gameName = gameName;
		gameConfig = new MultiConfig(this);
		freezePlayers = new FreezePlayers(this);
		reservedSlots = new ReservedSlots(this);
		spectate = new Spectate(this);
		worldsManager = new WorldsManager(this);
		statistics = new Statistics(this);
		Event event = new Event(getPlugin());
		Set<Player> players = new HashSet<Player>();
		for (World world : worlds().getWorlds()) {
			players.addAll(world.getPlayers());
		}
		for (Player player : players) {
			event.joinPlayerToGame(player, this);
		}
	}
	
	public void cleanUp() {
		countdown().removeAll();
		spectate.disableSpectateMode();
		resetPVP();
	}

	public void preparePlayer(Player player) { players.add(player); getPlugin().getLogger().info(String.format("Preparing player %s for %s", player.getName(), getName())); }
	public void releasePlayer(Player player) { players.remove(player); getPlugin().getLogger().info(String.format("Releasing player %s from %s", player.getName(), getName())); }
	
	public void refreshPlayerList() {
		for (Player player : players) {
			boolean remove = true;
			for (World world : worlds().getWorlds()) {
				if (world.getPlayers().contains(player))
					remove = false;
			}
			if (remove == true)
				players.remove(player);
		}
	}
	
	private MultiConfig gameConfig;
	public MultiConfig getConfig() { return this.gameConfig; }
	
	private FreezePlayers freezePlayers;
	public FreezePlayers freezePlayers() { return freezePlayers; }
	
	private ReservedSlots reservedSlots;
	public ReservedSlots reservedSlots() { return this.reservedSlots; }
	
	private Spectate spectate;
	public Spectate spectate() { return this.spectate; }
	
	private WorldsManager worldsManager;
	public WorldsManager worlds() { return this.worldsManager; }
	
	private Statistics statistics;
	public Statistics stats() { return this.statistics; }
	
	public GMMain getPlugin() { return GamesManager.plugin; }
	public String getName() { return gameName; }
	public String getCurrentMapName() { return mapName; }
	public String getType() { return getConfig().getString("type", null); }
	public Boolean isEnabled() { return getConfig().getBoolean("enabled", false); }
	public String getMapConfigPath() { return "maps." + mapName; }
	public Integer getMinPlayerCount() { return getConfig().getInt("minplayers", 0); }
	public Integer getMaxPlayerCount() { return getConfig().getInt("maxplayers", 0); }
	
	public Set<Location> getSpawnLocations() {
		Set<Location> spawnLocations = new HashSet<Location>();
		if (getLobbySpawnLocation() != null)
			spawnLocations.add(getLobbySpawnLocation());
		if (getSpecSpawnLocation() != null)
			spawnLocations.add(getSpecSpawnLocation());
		spawnLocations.addAll(getGameSpawnLocations());
		spawnLocations.addAll(getDMSpawnLocations());
		return spawnLocations;
	}
	
	public Location getLobbySpawnLocation() { return getConfig().getLocation("lobbyspawn", null); }
	public Location getSpecSpawnLocation() { return getConfig().getLocation(getMapConfigPath() + ".specspawn", null); }
	public Set<Location> getGameSpawnLocations() { return getConfigLocations(getMapConfigPath() + ".gamespawns"); }
	public Set<Location> getDMSpawnLocations() { return getConfigLocations(getMapConfigPath() + ".dmspawns"); }
	
	public Set<Location> getConfigLocations(String configKey) {
		return new HashSet<>(getConfig().getLocationList(configKey, new ArrayList<Location>(), true));
	}
	
	private CMain cMain = null;
	public CMain countdown() {
		if (cMain != null)
			return cMain;
		else {
			cMain = new CMain(this);
			return cMain;
		}
	}
	
	private HashMap<String, Boolean> pvp = new HashMap<String, Boolean>();
	
	public void setPVP(boolean enable) {
		for (World world : worlds().getWorlds()) {
			if (pvp.containsKey(world) == false)
				pvp.put(world.getName(), world.getPVP());
			world.setPVP(enable);
		}
	}
	
	public void resetPVP() {
		for (World world : worlds().getWorlds()) {
			if (pvp.containsKey(world) == false)
				world.setPVP(pvp.get(world.getName()));
		}
		pvp.clear();
	}
	
	public void resetDataForPlayers() {
		for (Player player : getPlayers()) {
			resetDataForPlayer(player);
		}
	}
	
	public void resetDataForPlayer(Player player) {
		if (getPlayers().contains(player) == false) return;
		player.getInventory().clear();
		player.getInventory().setHelmet(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setBoots(null);
		player.resetPlayerTime();
		player.resetPlayerWeather();
		player.setLevel(0);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.setSaturation(10);
		player.setExhaustion(0F);
		player.setFoodLevel(20);
		Damageable dm = player;
		dm.setHealth(dm.getMaxHealth());
		dm.setFireTicks(0);
		for (PotionEffect effect : player.getActivePotionEffects()) { 
			player.removePotionEffect(effect.getType()); 
		}
	}
	
	public Set<Player> getPlayers() {
		return new HashSet<Player>(players);
	}
	
	public Set<Player> getPlaying() {
		Set<Player> players = getPlayers();
		for (Player player : spectate.getSpectators()) {
			players.remove(player);
		}
		return players;
	}
	
	public void broadcast(String message) {
		for (Player player : getPlayers()) {
			player.sendMessage(Util.formatString(message));
		}
	}
	
	public void kickAllPlayers(String message) {
		Set<Player> toKick = new HashSet<Player>();
		toKick.addAll(getPlayers());
		for (Player player : toKick) {
			kickPlayer(player, message);
		}
	}
	
	public void kickPlayer(Player player, String message) {
		if (getPlayers().contains(player))
			GamesManager.kickPlayer(player, message, getName());
	}
	
}
