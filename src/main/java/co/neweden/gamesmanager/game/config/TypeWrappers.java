package co.neweden.gamesmanager.game.config;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class TypeWrappers {

    public Object get(String path, Object def) {
        // Placeholder method to be overridden in sub-classes
        return new Object();
    }

    // Boolean

    public Boolean isBoolean(String path) { return get(path, null) instanceof Boolean; }

    public Boolean getBoolean(String path) { return getBoolean(path, null); }
    public Boolean getBoolean(String path, Boolean def) {
        return (Boolean) get(path, def);
    }

    // String

    public Boolean isString(String path) { return get(path, null) instanceof String; }

    public String getString(String path) { return getString(path, null); }
    public String getString(String path, String def) {
        return (String) get(path, def);
    }

    // Integer

    public Boolean isInt(String path) { return get(path, null) instanceof String; }

    public Integer getInt(String path) { return getInt(path, null); }
    public Integer getInt(String path, Integer def) { return (Integer) get(path, def); }

    // Location

    public Location getLocation(String path) { return getLocation(path, null); }
    public Location getLocation(String path, Location defaultValue) { return getLocation(path, defaultValue, false); }
    public Location getLocation(String path, Location defaultValue, Boolean cleanLocation) {
        if (isString(path)) {
            if (Parser.verifyLocation(getString(path)))
                return Parser.parseLocation(getString(path), cleanLocation);
        }
        return defaultValue;
    }

    // List<?>

    public Boolean isList(String path) {
        return get(path, null) instanceof List;
    }

    public List getList(String path) { return getList(path, null); }
    public List getList(String path, String def) {
        return (List) get(path, def);
    }

    // List<String>

    public List<String> getStringList(String path) { return getStringList(path, null); }
    public List<String> getStringList(String path, String def) {
        return (List<String>) get(path, def);
    }

    // List<Location>

    public List<Location> getLocationList(String path) { return getLocationList(path, null); }
    public List<Location> getLocationList(String path, List<Location> defaultValue) { return getLocationList(path, defaultValue, false); }
    public List<Location> getLocationList(String path, List<Location> defaultValue, Boolean cleanLocation) {
        if (isList(path)) {
            return locListLoop(getList(path), cleanLocation);
        }
        return defaultValue;
    }

    private List<Location> locListLoop(List<?> list, Boolean cleanLocation) {
        List<Location> locList = new ArrayList<>();
        for (Object location : list) {
            String loc = location.toString();
            if (Parser.verifyLocation(loc) == true)
                locList.add(Parser.parseLocation(loc, cleanLocation));
        }
        return locList;
    }

}
