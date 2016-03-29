package co.neweden.gamesmanager;

import java.util.*;

import co.neweden.gamesmanager.game.*;
import co.neweden.gamesmanager.game.config.MultiConfig;
import co.neweden.gamesmanager.game.countdown.CMain;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;

import co.neweden.gamesmanager.game.spectate.Spectate;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Game {
	
	protected String gameName;
	protected GameType gameType;
	protected String gameTypeName;
	private Set<Player> players = new HashSet<>();
	@Deprecated
	private String mapName = "default";
	
	protected void construct() {
		worldsManager = new WorldsManager(this);
		gameConfig = new MultiConfig(this);
		freezePlayers = new FreezePlayers(this);
		reservedSlots = new ReservedSlots(this);
		spectate = new Spectate(this);
		statistics = new Statistics(this);
		lobby = new Lobby(this);
		Event event = new Event(getPlugin());
	}

	public GameType getGameTypeInstance() { return gameType; }

	public void cleanUp() {
		kickAllPlayers("The game has ended and is now resetting");
		countdown().removeAll();
		spectate.disableSpectateMode();
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

	private Lobby lobby;
	public Lobby lobby() { return lobby; }
	
	private Statistics statistics;
	public Statistics stats() { return this.statistics; }

	public GMMain getPlugin() { return GamesManager.plugin; }
	public String getName() { return gameName; }
	@Deprecated
	public String getCurrentMapName() { return mapName; }
	public String getTypeName() { return gameTypeName; }
	public Boolean isEnabled() { return getConfig().getBoolean("enabled", false);}
	@Deprecated
	public String getMapConfigPath() { return "maps." + mapName; }
	public Integer getMinPlayerCount() { return getConfig().getInt("minplayers", 0); }
	public Integer getMaxPlayerCount() { return getConfig().getInt("maxplayers", 0); }

	private CMain cMain = new CMain(this);
	public CMain countdown() { return cMain; }

	@Deprecated
	private HashMap<GMMap, Boolean> pvp = new HashMap<>();

	@Deprecated
	public void setPVP(boolean enable) {
		for (GMMap map : worlds().getMaps()) {
			if (!pvp.containsKey(map))
				pvp.put(map, map.getWorld().getPVP());
			map.getWorld().setPVP(enable);
		}
	}

	@Deprecated
	public void resetPVP() {
		for (GMMap map : worlds().getMaps()) {
			if (!pvp.containsKey(map))
				map.getWorld().setPVP(pvp.get(map));
		}
		pvp.clear();
	}

	public void resetDataForPlayers() { resetDataForPlayers(GameMode.SURVIVAL); }
	public void resetDataForPlayers(GameMode gm) {
		for (Player player : getPlayers()) {
			resetDataForPlayer(player, gm);
		}
	}

	public void resetDataForPlayer(Player player) { resetDataForPlayer(player, GameMode.SURVIVAL); }
	public void resetDataForPlayer(Player player, GameMode gm) {
		if (!getPlayers().contains(player)) return;
		player.setGameMode(GameMode.SURVIVAL);
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

	public void broadcastTitle(String title, String subTitle) { broadcastTitle(title, subTitle, 20L, 100L, 20L); }
	public void broadcastTitle(String title, String subTitle, Long fadeIn, Long stay, Long fadeOut) {
		for (Player player : getPlayers()) {
			Util.playerSendTitle(player, title, subTitle, fadeIn, stay, fadeOut);
		}
	}

	private Fireworks fireworks = new Fireworks(this);
	public void randomFireworks(Location loc, Integer duration) { fireworks.randomFireworks(loc, null, duration); }
	public void randomFireworks(Entity entity, Integer duration) { fireworks.randomFireworks(null, entity, duration); }

	public void kickAllPlayers(String message) {
		Set<Player> toKick = new HashSet<>();
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
