package co.neweden.gamesmanager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import co.neweden.gamesmanager.game.GMItem;

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
	
	public static GMItem parseConfigItem(String itemData) {
		String[] parts = itemData.split(" ");
		GMItem item = new GMItem();
		item.setItemStack(new ItemStack(Material.getMaterial(parts[0].toUpperCase())));
		return item;
	}
	
	public static String formatString(String text) {
		text = text.replaceAll("&0", "§0"); // Black
		text = text.replaceAll("&1", "§1"); // Dark Blue
		text = text.replaceAll("&2", "§2"); // Dark Green
		text = text.replaceAll("&3", "§3"); // Dark Aqua
		text = text.replaceAll("&4", "§4"); // Dark Red
		text = text.replaceAll("&5", "§5"); // Dark Purple
		text = text.replaceAll("&6", "§6"); // Gold
		text = text.replaceAll("&7", "§7"); // Gray
		text = text.replaceAll("&8", "§8"); // Dark Gray
		text = text.replaceAll("&9", "§9"); // Blue
		text = text.replaceAll("&a", "§a"); // Green
		text = text.replaceAll("&b", "§b"); // Aqua
		text = text.replaceAll("&c", "§c"); // Red
		text = text.replaceAll("&d", "§d"); // Light Purple
		text = text.replaceAll("&e", "§e"); // Yellow
		text = text.replaceAll("&f", "§f"); // White
		
		text = text.replaceAll("&k", "§k"); // Obfuscated
		text = text.replaceAll("&l", "§l"); // Bold
		text = text.replaceAll("&m", "§m"); // Strikethrough
		text = text.replaceAll("&o", "§o"); // Italic
		text = text.replaceAll("&r", "§r"); // Reset
		
		return text;
	}
	
}
