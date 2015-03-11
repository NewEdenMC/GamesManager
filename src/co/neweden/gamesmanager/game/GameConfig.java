package co.neweden.gamesmanager.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.Util;

public class GameConfig {
	
	private FileConfiguration pConfig;
	private String gPath;
	private String mPath;
	public enum Scope { MAP, GAME }
	
	public GameConfig(Game game) {
		pConfig = game.getPlugin().getConfig();
		gPath = "games." + game.getName() + ".";
		mPath = gPath + "maps." + game.getCurrentMapName() + ".";
	}
	
	public String getRootPath() { return gPath.substring(0, gPath.length() - 1); }
	public String getMapPath() { return gPath.substring(0, gPath.length() - 1); }
	
	public Boolean getBoolean(String path, Boolean defaultValue) { return getBoolean(path, defaultValue, Scope.MAP); }
	public Boolean getBoolean(String path, Boolean defaultValue, Scope scope) {
		if (pConfig.isBoolean(mPath + path) && scope.equals(Scope.MAP))
			return pConfig.getBoolean(mPath + path);
		else if (pConfig.isBoolean(gPath + path))
			return pConfig.getBoolean(gPath + path);
		else return defaultValue;
	}
	
	public String getString(String path, String defaultValue) { return getString(path, defaultValue, Scope.MAP); }
	public String getString(String path, String defaultValue, Scope scope) {
		if (pConfig.isString(mPath + path) && scope.equals(Scope.MAP))
			return pConfig.getString(mPath + path);
		else if (pConfig.isString(gPath + path))
			return pConfig.getString(gPath + path);
		else return defaultValue;
	}
	
	public Integer getInteger(String path, Integer defaultValue) { return getInteger(path, defaultValue, Scope.MAP); }
	public Integer getInteger(String path, Integer defaultValue, Scope scope) {
		if (pConfig.isInt(mPath + path) && scope.equals(Scope.MAP))
			return pConfig.getInt(mPath + path);
		else if (pConfig.isInt(gPath + path))
			return pConfig.getInt(gPath + path);
		else return defaultValue;
	}
	
	public Double getDouble(String path, Double defaultValue) { return getDouble(path, defaultValue, Scope.MAP); }
	public Double getDouble(String path, Double defaultValue, Scope scope) {
		if (pConfig.isDouble(mPath + path) && scope.equals(Scope.MAP))
			return pConfig.getDouble(mPath + path);
		else if (pConfig.isDouble(gPath + path))
			return pConfig.getDouble(gPath + path);
		else return defaultValue;
	}
	
	public List<?> getList(String path, List<?> defaultValue) { return getList(path, defaultValue, Scope.MAP); }
	public List<?> getList(String path, List<?> defaultValue, Scope scope) {
		if (pConfig.isList(mPath + path) && scope.equals(Scope.MAP))
			return pConfig.getList(mPath + path);
		else if (pConfig.isList(gPath + path))
			return pConfig.getList(gPath + path);
		else return defaultValue;
	}
	
	public Location getLocation(String path, Location defaultValue) { return getLocation(path, defaultValue, false); }
	public Location getLocation(String path, Location defaultValue, Scope scope) { return getLocation(path, defaultValue, false, scope); }
	public Location getLocation(String path, Location defaultValue, Boolean cleanLocation) { return getLocation(path, defaultValue, cleanLocation, Scope.MAP); }
	public Location getLocation(String path, Location defaultValue, Boolean cleanLocation, Scope scope) {
		if (pConfig.isString(mPath + path) && scope.equals(Scope.MAP)) {
			if (Util.verifyLocation(pConfig.getString(mPath + path)) && scope.equals(Scope.MAP))
				return Util.parseLocation(pConfig.getString(mPath + path), cleanLocation);
		} else if (pConfig.isString(gPath + path)) {
			if (Util.verifyLocation(pConfig.getString(gPath + path)))
				return Util.parseLocation(pConfig.getString(gPath + path), cleanLocation);
		}
		return defaultValue;
	}
	
	public List<Location> getLocationList(String path, List<Location> defaultValue) { return getLocationList(path, defaultValue, false); }
	public List<Location> getLocationList(String path, List<Location> defaultValue, Scope scope) { return getLocationList(path, defaultValue, false, scope); }
	public List<Location> getLocationList(String path, List<Location> defaultValue, Boolean cleanLocation) { return getLocationList(path, defaultValue, cleanLocation, Scope.MAP); }
	public List<Location> getLocationList(String path, List<Location> defaultValue, Boolean cleanLocation, Scope scope) {
		if (pConfig.isList(mPath + path)) {
			return locListLoop(pConfig.getList(mPath + path), cleanLocation);
		} else if (pConfig.isList(gPath + path)) {
			return locListLoop(pConfig.getList(gPath + path), cleanLocation);
		} else return defaultValue;
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
