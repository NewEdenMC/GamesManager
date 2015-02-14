package co.neweden.gamesmanager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;

import co.neweden.gamesmanager.game.FreezePlayers;
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
	
	public void preparePlayer(Player player) { players.add(player); }
	public void releasePlayer(Player player) { players.remove(player); }
	
	public void updatePlayerList() {
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
	
	private FreezePlayers freezePlayers;
	public FreezePlayers freezePlayers() { return freezePlayers; }
	
	private ReservedSlots reservedSlots;
	public ReservedSlots reservedSlots() { return this.reservedSlots; }
	
	private Spectate spectate;
	public Spectate spectate() { return this.spectate; }
	
	private WorldsManager worldsManager;
	public WorldsManager worlds() { return this.worldsManager; }
	
	public GMMain getPlugin() { return plugin; }
	public String getConfigPath() { return "games." + getName(); }
	public String getMapConfigPath() { return getConfigPath() + ".maps." + mapName; }
	public String getName() { return gamename; }
	
	public String getType() {
		if (getPlugin().getConfig().isString("games." + getName() + ".type")) {
			return getPlugin().getConfig().getString("games." + getName() + ".type");
		} else return null;
	}
	
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
	
	public boolean isEnabled() {
		if (plugin.getConfig().isBoolean(getConfigPath() + ".enabled")) {
			return plugin.getConfig().getBoolean(getConfigPath() + ".enabled");
		} else return false;
	}
	
	public int getMinPlayerCount() {
		if (plugin.getConfig().isInt(getConfigPath() + ".minplayers")) {
			return plugin.getConfig().getInt(getConfigPath() + ".minplayers");
		} else {
			return 0;
		}
	}
	
	public int getMaxPlayerCount() {
		if (plugin.getConfig().isInt(getConfigPath() + ".maxplayers")) {
			return plugin.getConfig().getInt(getConfigPath() + ".maxplayers");
		} else {
			return 0;
		}
	}
	
	public Set<Location> getSpawnLocations() {
		Set<Location> spawnLocations = new HashSet<Location>();
		spawnLocations.add(getLobbySpawnLocation());
		spawnLocations.add(getSpecSpawnLocation());
		spawnLocations.addAll(getGameSpawnLocations());
		spawnLocations.addAll(getDMSpawnLocations());
		return spawnLocations;
	}
	
	public Location getLobbySpawnLocation() { return getConfigLocation("lobbyspawn"); }
	public Location getSpecSpawnLocation() { return getConfigLocation("specspawn"); }
	public Set<Location> getGameSpawnLocations() { return getConfigLocations("gamespawns"); }
	public Set<Location> getDMSpawnLocations() { return getConfigLocations("dmspawns"); }
	
	public Location getConfigLocation(String configKey) {
		for (Location location : getConfigLocations(configKey)) {
			return location;
		}
		return null;
	}
	
	public Set<Location> getConfigLocations(String configKey) {
		Set<Location> spawnLocations = new HashSet<Location>();
		String gPath = getConfigPath() + "." + configKey;
		String mPath = getMapConfigPath() + "." + configKey;
		String path = mPath;
		if (getPlugin().getConfig().isString(mPath) == false && 
			getPlugin().getConfig().isList(mPath) == false)
				path = gPath;
		
		if (getPlugin().getConfig().isString(path)) {
			if (Util.verifyLocation(getPlugin().getConfig().getString(path)) == true) 
				spawnLocations.add(Util.parseLocation(getPlugin().getConfig().getString(path), true));
		}
		if (getPlugin().getConfig().isList(path)) {
			List<?> loclist = getPlugin().getConfig().getList(path);
			for (Object location : loclist) {
				String loc = location.toString();
				if (Util.verifyLocation(loc) == true) 
					spawnLocations.add(Util.parseLocation(loc, true));
			}
		}
		return spawnLocations;
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
