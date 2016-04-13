package co.neweden.gamesmanager.game.config;

import co.neweden.gamesmanager.GamesManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class Parser {

    private Parser() { }

    // Location

    public static boolean verifyLocation(String location) {
        try {
            if (parseLocation(location) instanceof Location)
                return true;
            else
                return false;
        } catch (NumberFormatException e) { return false;
        } catch (NullPointerException e) { return false; }
    }

    public static Location parseLocation(String location) { return parseLocation(location, false); }
    public static Location parseLocation(String location, Boolean cleanLocation) { return parseLocation(location, cleanLocation, null); }
    public static Location parseLocation(String location, Boolean cleanLocation, World defaultWorld) {
        List<String> parts = new ArrayList<>(Arrays.asList(location.split(" ")));
        World world = defaultWorld;
        // Check to see if the first the first element is a world name or coordinate
        // if world name get the world object and remove it for further processing
        // if coordinate just continue as normal
        try {
            Double.parseDouble(parts.get(0));
        } catch (NumberFormatException e) {
            try {
                world = Bukkit.getServer().getWorld(parts.get(0));
            } catch (NullPointerException ex) {
                throw new NullPointerException("Invalid world, either no work was given or world is not loaded, is the world name correct? " + parts.get(0));
            }
            parts.remove(0);
        }
        List<Double> locValues = Arrays.asList(0D, 0D, 0D); // x y z coordinates
        List<Float> aValues = Arrays.asList(0F, 0F); // yaw and pitch values
        Object current = null;
        try {
            for (int i = 0; i < 3; i++) {
                current = parts.get(0);
                locValues.set(i, Double.parseDouble(parts.get(0)));
                parts.remove(parts.get(0));
            }
            for (int i = 0; i < 2; i++) {
                current = parts.get(0);
                aValues.set(i, Float.parseFloat(parts.get(0)));
                parts.remove(parts.get(0));
            }
        } catch (IndexOutOfBoundsException e) {
        } catch (NumberFormatException e) { throw new NumberFormatException("Unable to parse location coordinate, is this value correct? " + current.toString()); }
        if (cleanLocation == true)
            return cleanLocation(new Location(world, locValues.get(0), locValues.get(1), locValues.get(2), aValues.get(0), aValues.get(1)));
        else
            return new Location(world, locValues.get(0), locValues.get(1), locValues.get(2), aValues.get(0), aValues.get(1));
    }

    public static Location cleanLocation(Location location) {
        Double x = location.getX(); Double y = location.getY(); Double z = location.getZ();
        x = Math.floor(x); y = Math.floor(y); z = Math.floor(z);

        if (x >= 0)
            location.setX(x + 0.5);
        else
            location.setX(x - 0.5);
        location.setY(y);
        if (z >= 0)
            location.setZ(z + 0.5);
        else
            location.setZ(z - 0.5);

        return location;
    }

    // ItemStack

    public static boolean verifyItemStack(String itemData) {
        return parseItemStack(itemData) != null;
    }

    public static ItemStack parseItemStack(String itemData) {
        String[] parts = itemData.split(" ");
        Material material;
        Short damage = 0;
        Integer amount = 1;

        if (parts[0].contains(":")) {
            String[] iParts = parts[0].split(":");
            parts[0] = iParts[0];
            try { damage = Short.parseShort(iParts[1]);
            } catch (NumberFormatException e) {
                GamesManager.getPlugin().getLogger().log(Level.WARNING, "I tried to parse an ItemStack (probably from a config file), however I was not able to parse the damage value, seems it's not a valid number", e);
            }
        }

        if (parts.length >= 2) {
            try { amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                GamesManager.getPlugin().getLogger().log(Level.WARNING, "I tried to parse an ItemStack (probably from a config file), however I was not able to parse the amount value, seems it's not a valid number", e);
            }
        }

        material = Material.getMaterial(parts[0].toUpperCase());
        if (material == null) return null;

        return new ItemStack(material, amount, damage);
    }



}
