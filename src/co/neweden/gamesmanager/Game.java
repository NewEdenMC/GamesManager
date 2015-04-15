package co.neweden.gamesmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;

import co.neweden.gamesmanager.game.FreezePlayers;
import co.neweden.gamesmanager.game.GameConfig;
import co.neweden.gamesmanager.game.GameConfig.Scope;
import co.neweden.gamesmanager.game.ReservedSlots;
import co.neweden.gamesmanager.game.Spectate;
import co.neweden.gamesmanager.game.WorldsManager;
import co.neweden.gamesmanager.game.countdown.CMain;
import co.neweden.gamesmanager.gametype.HungerGames;
import co.neweden.gamesmanager.gametype.HungerGames_Dev;

public class Game implements Listener {
	
	private GMMain plugin;
	private String gamename;
	private GameType gameClass = null;
	private Set<Player> players = new HashSet<Player>();
	private String mapName = "default";
	
	public Game(GMMain instance, String gamename) {
		this.plugin = instance;
		this.gamename = gamename;
		gameConfig = new GameConfig(this);
		freezePlayers = new FreezePlayers(this);
		reservedSlots = new ReservedSlots(this);
		spectate = new Spectate(this);
		worldsManager = new WorldsManager(this);
		Bukkit.getServer().getPluginManager().registerEvents(this, getPlugin());
		Event event = new Event(instance);
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
	
	public void preparePlayer(Player player) { players.add(player); Bukkit.getLogger().info(String.format("[%s] Preparing player %s for %s", plugin.getName(), player.getName(), getName())); }
	public void releasePlayer(Player player) { players.remove(player); Bukkit.getLogger().info(String.format("[%s] Releasing player %s from %s", plugin.getName(), player.getName(), getName())); }
	
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
	
	private GameConfig gameConfig;
	public GameConfig getConfig() { return this.gameConfig; }
	
	private FreezePlayers freezePlayers;
	public FreezePlayers freezePlayers() { return freezePlayers; }
	
	private ReservedSlots reservedSlots;
	public ReservedSlots reservedSlots() { return this.reservedSlots; }
	
	private Spectate spectate;
	public Spectate spectate() { return this.spectate; }
	
	private WorldsManager worldsManager;
	public WorldsManager worlds() { return this.worldsManager; }
	
	public GMMain getPlugin() { return plugin; }
	@Deprecated
	public String getConfigPath() { return "games." + getName(); }
	@Deprecated
	public String getMapConfigPath() { return getConfigPath() + ".maps." + mapName; }
	public String getName() { return gamename; }
	public String getCurrentMapName() { return mapName; }
	public String getType() { return getConfig().getString("type", null, Scope.GAME); }
	public Boolean isEnabled() { return getConfig().getBoolean("enabled", false, Scope.GAME); }
	
	public GameType getTypeClass() {
		if (gameClass != null) return gameClass;
		if (getType() == null) {
			getPlugin().logger.warning(String.format("[%s] Unable to determine the game type to load for %s, please check your config.", getPlugin().getDescription().getName(), getName()));
			return null;
		}
		switch (getType()) {
			case "hungergames":
				gameClass = new HungerGames(this);
				break;
			case "hungergames_dev":
				gameClass = new HungerGames_Dev(this);
				break;
			default: getPlugin().logger.warning(String.format("[%s] Game type %s for %s is either not valid or is not installed, please check your config", getPlugin().getDescription().getName(), getType(), getName()));
				return null;
		}
		return gameClass;
	}
	
	public Integer getMinPlayerCount() { return getConfig().getInteger("minplayers", 0); }
	public Integer getMaxPlayerCount() { return getConfig().getInteger("maxplayers", 0); }
	
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
	public Location getSpecSpawnLocation() { return getConfig().getLocation("specspawn", null); }
	public Set<Location> getGameSpawnLocations() { return getConfigLocations("gamespawns"); }
	public Set<Location> getDMSpawnLocations() { return getConfigLocations("dmspawns"); }
	
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
		/*Set<Player> players = new HashSet<Player>();
		Set<World> worlds = getWorlds();
		if (worlds.isEmpty()) return players;
		for (World world : worlds) {
			for (Player player : world.getPlayers()) {
				players.add(player);
			}
		}
		return players;*/
		return this.players;
	}
	
	public Set<Player> getPlaying() {
		// TODO: Add spectate integration
		return getPlayers();
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
