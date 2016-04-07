package co.neweden.gamesmanager.game;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;

import co.neweden.gamesmanager.game.config.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.Util;
import org.bukkit.util.FileUtil;

public class WorldsManager implements Listener {
	
	private Game game;
	private Set<GMMap> maps;
	private GMMap currentMap;
	protected Boolean gamePvP = null;

	public WorldsManager(Game game) {
		maps = new HashSet<>();
		this.game = game;
		Bukkit.getPluginManager().registerEvents(this, game.getPlugin());
	}

	public Set<GMMap> getMaps() { return new HashSet<>(maps); }

	public GMMap getCurrentMap() { return currentMap; }

	public GMMap loadMap(World world) { return loadMap(world.getName()); }
	public GMMap loadMap(String worldName) {
		if (worldName == null) return null;

		Path pPath = Paths.get("GamesManager_TempWorld");
		String wName = pPath + File.separator + game.getName() + "_" + worldName;

		Integer count = 0;
		while (Files.exists(Paths.get(wName + "_" + count)))
			count++;
		wName = wName + "_" + count;
		Path wPath = Paths.get(wName);

		try {
			try {
				Files.createDirectory(pPath);
			} catch (FileAlreadyExistsException ex) {
				if (!Files.isDirectory(pPath)) {
					game.getPlugin().getLogger().severe(pPath.toString() + " exists and is not a directory, GamesManager needs this to be a directory to store temporary world instances");
					return null;
				}
			}
			FileUtils.copyDirectory(new File(worldName), wPath.toFile());
			Files.delete(Paths.get(wPath + File.separator + "uid.dat"));
		} catch (IOException ex) {
			game.getPlugin().getLogger().severe("IOException has occurred: " + ex.getMessage());
			return null;
		}

		World nWorld = WorldCreator.name(wName).createWorld();
		if (nWorld == null) {
			game.getPlugin().getLogger().severe("Could not create or load world " + wName);
			return null;
		}
		nWorld.setAutoSave(false);
		nWorld.setGameRuleValue("doMobSpawning", "false");
		if (gamePvP != null) nWorld.setPVP(gamePvP);
		GMMap map = new GMMap(game, nWorld, worldName);
		maps.add(map);
		return map;
	}

	public GMMap getMap(World world) {
		for (GMMap map : maps) {
			if (map.getWorld().equals(world)) return map;
		}
		return null;
	}

	public void unloadMaps() {
		Set<GMMap> tMaps = new HashSet<>();
		tMaps.addAll(maps);
		for (GMMap map : tMaps) {
			unloadMap(map);
		}
	}

	public boolean unloadMap(GMMap map) {
		game.getPlugin().getLogger().info(String.format("[%s] Unloading and deleting map %s", game.getName(), map.getWorld().getName()));
		maps.remove(map);
		boolean unload = game.getPlugin().getServer().unloadWorld(map.getWorld(), false);
		if (!unload) {
			maps.add(map);
			game.getPlugin().getLogger().warning(String.format("[%s] Unable to unload world %s", game.getName(), map.getWorld().getName()));
			return false;
		}
		try {
			FileUtils.deleteDirectory(new File(map.getWorld().getName()));

			File root = new File("GamesManager_TempWorld");
			if (root.list().length == 0)
				FileUtils.deleteDirectory(root);

		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return true;
	}

	public void setCurrentMap(GMMap map) {
		game.getConfig().switchMap(map.getBaseWorldName());
		currentMap = map;
	}

	private HashMap<String, WBSquare> wbSquares = new HashMap<String, WBSquare>();
	
	public void setWorldBorder(World world, Location centre, Integer radius) {
		wbSquares.put(world.getName(), new WBSquare(centre, radius));
	}
	
	public void resetWorldBorder(World world) {
		wbSquares.remove(world.getName());
	}
	
	private Location wbMovePlayer(Location pLoc) {
		Location nLoc = new Location(pLoc.getWorld(), pLoc.getX(), pLoc.getY(), pLoc.getZ());
		WBSquare space = wbSquares.get(pLoc.getWorld().getName());
		Location centre = space.getCentre();
		
		if (pLoc.getX() > centre.getX()) {
			if (pLoc.getX() >= space.getTopX())
				nLoc.setX(pLoc.getX() - 3);
		} else {
			if (pLoc.getX() <= space.getBottomX())
				nLoc.setX(pLoc.getX() + 3);
		}
		if (pLoc.getZ() > centre.getZ()) {
			if (pLoc.getZ() >= space.getTopZ())
				nLoc.setZ(pLoc.getZ() - 3);
		} else {
			if (pLoc.getZ() <= space.getBottomZ())
				nLoc.setZ(pLoc.getZ() + 3);
		}
		
		if (nLoc.getBlock().getType() != Material.AIR) {
			for (int i = (int) nLoc.getY(); nLoc.getBlock().getType() == Material.AIR; ++i) {
				nLoc.setY((double) i);
			}
		}
		
		nLoc.setYaw(pLoc.getYaw());
		nLoc.setPitch(pLoc.getPitch());
		
		return nLoc;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.isCancelled() ||
			!game.getPlayers().contains(event.getPlayer()) ||
			!wbSquares.containsKey(event.getPlayer().getWorld().getName())) return;

		WBSquare square = wbSquares.get(event.getPlayer().getWorld().getName());
		Location location = event.getPlayer().getLocation();
		
		Boolean outside = false;
		Boolean tp = false;
		
		if ((int) location.getX() > square.getTopX() ||
				(int) location.getZ() > square.getTopZ() ||
				(int) location.getX() < square.getBottomX() ||
				(int) location.getZ() < square.getBottomZ())
			outside = true;
		
		if ((int) location.getX() > square.getTopX() + 3 ||
				(int) location.getZ() > square.getTopZ() + 3 ||
				(int) location.getX() < square.getBottomX() - 3 ||
				(int) location.getZ() < square.getBottomZ() - 3)
			tp = true;
		
		if (outside) {
			if (tp)
				event.getPlayer().teleport(square.getCentre());
			else
				event.getPlayer().teleport(wbMovePlayer(event.getPlayer().getLocation()));
			event.getPlayer().sendMessage(Util.formatString("&cYou are not allowed to pass beond this point."));
		}
	}

	public void setPvP(boolean pvp) {
		gamePvP = pvp;
		for (GMMap map : getMaps()) {
			map.getWorld().setPVP(pvp);
		}
	}

	private boolean bedEnter = false;
	public void enableBeds(boolean enable) { bedEnter = enable; }

	@EventHandler(priority = EventPriority.HIGH)
	public void bedInteractEvent(PlayerBedEnterEvent event) {
		if (!event.isCancelled() && game.getPlayers().contains(event.getPlayer()) && !bedEnter)
			event.setCancelled(true);
	}
	
}
