package co.neweden.gamesmanager.game;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.Util;

public class GameConfig extends YamlConfiguration {
	
	public GameConfig(Game game) {
		try {
			load(new File(game.getPlugin().getDataFolder(), game.getName() + ".yml"));
		} catch (FileNotFoundException ex) {
		} catch (IOException ex) {
			Bukkit.getLogger().log(Level.SEVERE, String.format("[%s] %s: cannot load configuration!", game.getPlugin().getDescription().getName(), game.getName()));
			ex.printStackTrace();
		} catch (InvalidConfigurationException ex) {
			Bukkit.getLogger().log(Level.SEVERE, String.format("[%s] %s: cannot load configuration!", game.getPlugin().getDescription().getName(), game.getName()));
			ex.printStackTrace();
		}
	}
	
	public Location getLocation(String path) { return getLocation(path, null); }
	public Location getLocation(String path, Location defaultValue) { return getLocation(path, defaultValue, false); }
	public Location getLocation(String path, Location defaultValue, Boolean cleanLocation) {
		if (isString(path)) {
			if (Util.verifyLocation(getString(path)))
				return Util.parseLocation(getString(path), cleanLocation);
		}
		return defaultValue;
	}
	
	public List<Location> getLocationList(String path) { return getLocationList(path, null); }
	public List<Location> getLocationList(String path, List<Location> defaultValue) { return getLocationList(path, defaultValue, false); }
	public List<Location> getLocationList(String path, List<Location> defaultValue, Boolean cleanLocation) {
		if (isList(path)) {
			return locListLoop(getList(path), cleanLocation);
		}
		return defaultValue;
	}
	
	private List<Location> locListLoop(List<?> list, Boolean cleanLocation) {
		List<Location> locList = new ArrayList<Location>();
		for (Object location : list) {
			String loc = location.toString();
			if (Util.verifyLocation(loc) == true) 
				locList.add(Util.parseLocation(loc, cleanLocation));
		}
		return locList;
	}
	
}
