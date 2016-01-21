package co.neweden.gamesmanager.game;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.Util;

public class WorldsManager implements Listener {
	
	private Game game;
	
	public WorldsManager(Game game) {
		this.game = game;
		Bukkit.getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public Set<World> getWorlds() {
		Set<World> worlds = new HashSet<World>();
		Set<Location> locations = game.getSpawnLocations();
		if (locations.isEmpty()) return worlds;
		for (Location loc : locations) {
			if (loc == null) continue;
			worlds.add(loc.getWorld());
		}
		return worlds;
	}
	
	public void saveSnapshots() {
		for (World world : getWorlds()) {
			saveSnapshot(world);
		}
	}
	
	public void saveSnapshot(World world) {
		File wFolder = world.getWorldFolder();
		File sFolder = new File(game.getPlugin().getDataFolder().getPath() + "/worldSnapshots/" + world.getName());
		
		world.save();
		try {
			if (sFolder.exists()) {
				FileUtils.deleteDirectory(sFolder);
			}
			sFolder.mkdirs();
			FileUtils.copyDirectory(wFolder, sFolder);
			//Util.copyFolder(wFolder, sFolder);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.SEVERE, String.format("[%s] An error occured when creating a snapshot backup of the world %s, see stack trace below.", game.getPlugin().getName(), world.getName()));
			e.printStackTrace();
		}
	}
	
	public void restoreWorlds() {
		for (World world : getWorlds()) {
			restoreWorld(world);
		}
	}
	
	public void restoreWorld(World world) {
		File wFolder = world.getWorldFolder();
		File sFolder = new File(game.getPlugin().getDataFolder().getPath() + "/worldSnapshots/" + world.getName());
		
		Bukkit.unloadWorld(world, true);
		try {
			if (sFolder.isDirectory() == false) return;
			if (wFolder.exists()) {
				FileUtils.deleteDirectory(wFolder);
			}
			wFolder.mkdirs();
			FileUtils.copyDirectory(sFolder, wFolder);
			WorldCreator.name(world.getName()).createWorld();
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.SEVERE, String.format("[%s] An error occured when restoring the snapshot backup for the world %s, depending on the error the world may now be corrupt try restoring it manually, see stack trace below.", game.getPlugin().getName(), world.getName()));
			e.printStackTrace();
		}
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
		if (event.isCancelled() == true) return;
		if (game.getPlayers().contains(event.getPlayer()) == false) return;
		if (wbSquares.containsKey(event.getPlayer().getWorld().getName()) == false) return;
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
		
		if (outside == true) {
			if (tp == true)
				event.getPlayer().teleport(square.getCentre());
			else
				event.getPlayer().teleport(wbMovePlayer(event.getPlayer().getLocation()));
			event.getPlayer().sendMessage(Util.formatString("&cYou are not allowed to pass beond this point."));
		}
	}
	
}
