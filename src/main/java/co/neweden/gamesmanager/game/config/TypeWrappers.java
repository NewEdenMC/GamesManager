package co.neweden.gamesmanager.game.config;

import co.neweden.gamesmanager.Game;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class TypeWrappers {

    protected Game game;

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

    public Boolean isLocation(String path) {
        if (isString(path))
            return Parser.verifyLocation(get(path, null).toString());
        else return false;
    }

    public Location getLocation(String path) { return getLocation(path, null); }
    public Location getLocation(String path, Location def) { return getLocation(path, def, false); }
    public Location getLocation(String path, Location def, Boolean cleanLocation) {
        World world = null;
        if (def == null) {
            if (game.worlds().getCurrentMap().getWorld() != null) world = game.worlds().getCurrentMap().getWorld();
        } else
            world = def.getWorld();
        if (isLocation(path))
            return Parser.parseLocation(getString(path), cleanLocation, world);
        else
            return def;
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
    public List<Location> getLocationList(String path, List<Location> def) { return getLocationList(path, def, false); }
    public List<Location> getLocationList(String path, List<Location> def, Boolean cleanLocation) {
        if (isList(path)) {
            return locListLoop(getList(path), def, cleanLocation);
        }
        return def;
    }

    private List<Location> locListLoop(List<?> list, List<Location> def, Boolean cleanLocation) {
        List<Location> locList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            World world = null;
            if (def == null) {
                if (game.worlds().getCurrentMap().getWorld() != null) world = game.worlds().getCurrentMap().getWorld();
            } else {
                if (i < def.size()) world = def.get(i).getWorld();
            }
            locList.add(Parser.parseLocation(list.get(i).toString(), cleanLocation, world));
        }
        return locList;
    }

}
