package co.neweden.gamesmanager;

import java.util.*;

import co.neweden.gamesmanager.game.*;
import co.neweden.gamesmanager.game.config.MultiConfig;
import co.neweden.gamesmanager.game.countdown.CMain;
import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import co.neweden.gamesmanager.game.spectate.Spectate;

public class Game {
	
	protected String gameName;
	protected GameType gameType;
	protected String gameTypeName;
	private Set<Player> players = new HashSet<>();
	private String mapName = "default";
	
	protected void construct() {
		worldsManager = new WorldsManager(this);
		gameConfig = new MultiConfig(this);
		freezePlayers = new FreezePlayers(this);
		reservedSlots = new ReservedSlots(this);
		spectate = new Spectate(this);
		statistics = new Statistics(this);
		Event event = new Event(getPlugin());
	}

	public GameType getGameTypeInstance() { return gameType; }

	public void cleanUp() {
		kickAllPlayers("The game has ended and is now resetting");
		countdown().removeAll();
		oldcountdown().removeAll();
		spectate.disableSpectateMode();
		resetPVP();
		worlds().unloadMaps();
	}

	public void preparePlayer(Player player) { players.add(player); getPlugin().getLogger().info(String.format("Preparing player %s for %s", player.getName(), getName())); }
	public void releasePlayer(Player player) { players.remove(player); getPlugin().getLogger().info(String.format("Releasing player %s from %s", player.getName(), getName())); }
	
	public void refreshPlayerList() {
		for (Player player : players) {
			boolean remove = true;
			for (GMMap map : worlds().getMaps()) {
				if (map.getWorld().getPlayers().contains(player))
					remove = false;
			}
			if (remove)
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
	public String getTypeName() { return gameTypeName; }
	public Boolean isEnabled() { return getConfig().getBoolean("enabled", false);}
	public String getMapConfigPath() { return "maps." + mapName; }
	public Integer getMinPlayerCount() { return getConfig().getInt("minplayers", 0); }
	public Integer getMaxPlayerCount() { return getConfig().getInt("maxplayers", 0); }

	@Deprecated
	public Set<Location> getSpawnLocations() {
		Set<Location> spawnLocations = new HashSet<Location>();
		try {
			spawnLocations.add(getLobbySpawnLocation());
		} catch (NullPointerException ex) { }
		try {
			spawnLocations.add(getSpecSpawnLocation());
		} catch (NullPointerException ex) { }
		spawnLocations.addAll(getGameSpawnLocations());
		spawnLocations.addAll(getDMSpawnLocations());
		return spawnLocations;
	}

	@Deprecated
	public Location getLobbySpawnLocation() { return getConfig().getLocation("lobbyspawn", null, true); }
	@Deprecated
	public Location getSpecSpawnLocation() { return getConfig().getLocation(getMapConfigPath() + ".specspawn", null, true); }
	@Deprecated
	public Set<Location> getGameSpawnLocations() { return getConfigLocations(getMapConfigPath() + ".gamespawns"); }
	@Deprecated
	public Set<Location> getDMSpawnLocations() { return getConfigLocations(getMapConfigPath() + ".dmspawns"); }
	@Deprecated
	public Set<Location> getConfigLocations(String configKey) {
		return new HashSet<>(getConfig().getLocationList(configKey, new ArrayList<Location>(), true));
	}
	
	private co.neweden.gamesmanager.game.countdown_old.CMain cMainOld = null;
	@Deprecated
	public co.neweden.gamesmanager.game.countdown_old.CMain oldcountdown() {
		if (cMainOld != null)
			return cMainOld;
		else {
			cMainOld = new co.neweden.gamesmanager.game.countdown_old.CMain(this);
			return cMainOld;
		}
	}

	private CMain cMain = new CMain(this);
	public CMain countdown() { return cMain; }
	
	private HashMap<GMMap, Boolean> pvp = new HashMap<>();
	
	public void setPVP(boolean enable) {
		for (GMMap map : worlds().getMaps()) {
			if (!pvp.containsKey(map))
				pvp.put(map, map.getWorld().getPVP());
			map.getWorld().setPVP(enable);
		}
	}
	
	public void resetPVP() {
		for (GMMap map : worlds().getMaps()) {
			if (!pvp.containsKey(map))
				map.getWorld().setPVP(pvp.get(map));
		}
		pvp.clear();
	}
	
	public void resetDataForPlayers() {
		for (Player player : getPlayers()) {
			resetDataForPlayer(player);
		}
	}
	
	public void resetDataForPlayer(Player player) {
		if (!getPlayers().contains(player)) return;
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
		return new HashSet<>(players);
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
