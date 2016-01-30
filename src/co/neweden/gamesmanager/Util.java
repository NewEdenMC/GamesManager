package co.neweden.gamesmanager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import co.neweden.gamesmanager.game.ItemStackWrapper;

public final class Util {
	
	private Util() { }
	
	public static boolean verifyLocation(String location) {
		if (location.contains(" ")) {
			String[] parts = location.split(" ");
			if (parts.length >= 4 && parts.length <= 6) {
				if (Bukkit.getServer().getWorld(parts[0]) == null) return false;
				try {
					Double.parseDouble(parts[1]);
					Double.parseDouble(parts[2]);
					Double.parseDouble(parts[3]);
				} catch (NumberFormatException e) { return false; }
				try {
					try { Float.parseFloat(parts[4]); } catch (NumberFormatException e) { return false; }
				} catch (IndexOutOfBoundsException e) { }
				try {
					try { Float.parseFloat(parts[5]); } catch (NumberFormatException e) { return false; }
				} catch (IndexOutOfBoundsException e) { }
				return true;
			}
		}
		return false;
	}
	
	public static Location parseLocation(String location) { return parseLocation(location, false); }
	public static Location parseLocation(String location, Boolean cleanLocation) {
		String[] parts = location.split(" ");
		World world = Bukkit.getServer().getWorld(parts[0]);
		Double x = Double.parseDouble(parts[1]);
		Double y = Double.parseDouble(parts[2]);
		Double z = Double.parseDouble(parts[3]);
		Float yaw = 0F;
		try {
			yaw = Float.parseFloat(parts[4]);
		} catch (IndexOutOfBoundsException e) { }
		Float pitch = 0F;
		try {
			pitch = Float.parseFloat(parts[5]);
		} catch (IndexOutOfBoundsException e) { }
		if (cleanLocation == true)
			return cleanLocation(new Location(world, x, y, z, yaw, pitch));
		else
			return new Location(world, x, y, z, yaw, pitch);
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
	
	public static boolean verifyConfigItem(String itemData) {
		//if (itemData.contains(" ")) {
			String[] parts = itemData.split(" ");
			if (parts.length == 1) {
				if (parts[0] != "") {
					if (Material.getMaterial(parts[0].toUpperCase()) == null) {
						return false;
					}
				}
				return true;
			}
		//}
		return false;
	}
	
	public static ItemStackWrapper parseConfigItem(String itemData) {
		String[] parts = itemData.split(" ");
		ItemStackWrapper item = new ItemStackWrapper();
		item.setItemStack(new ItemStack(Material.getMaterial(parts[0].toUpperCase())));
		return item;
	}
	
	public static String formatString(String text) {
		text = text.replaceAll("&0", "\u00A70"); // Black
		text = text.replaceAll("&1", "\u00A71"); // Dark Blue
		text = text.replaceAll("&2", "\u00A72"); // Dark Green
		text = text.replaceAll("&3", "\u00A73"); // Dark Aqua
		text = text.replaceAll("&4", "\u00A74"); // Dark Red
		text = text.replaceAll("&5", "\u00A75"); // Dark Purple
		text = text.replaceAll("&6", "\u00A76"); // Gold
		text = text.replaceAll("&7", "\u00A77"); // Gray
		text = text.replaceAll("&8", "\u00A78"); // Dark Gray
		text = text.replaceAll("&9", "\u00A79"); // Blue
		text = text.replaceAll("&a", "\u00A7a"); // Green
		text = text.replaceAll("&b", "\u00A7b"); // Aqua
		text = text.replaceAll("&c", "\u00A7c"); // Red
		text = text.replaceAll("&d", "\u00A7d"); // Light Purple
		text = text.replaceAll("&e", "\u00A7e"); // Yellow
		text = text.replaceAll("&f", "\u00A7f"); // White
		
		text = text.replaceAll("&k", "\u00A7k"); // Obfuscated
		text = text.replaceAll("&l", "\u00A7l"); // Bold
		text = text.replaceAll("&m", "\u00A7m"); // Strikethrough
		text = text.replaceAll("&o", "\u00A7o"); // Italic
		text = text.replaceAll("&r", "\u00A7r"); // Reset
		
		return text;
	}
	
}
