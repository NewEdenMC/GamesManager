package co.neweden.gamesmanager.game;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import co.neweden.gamesmanager.Game;

public class FreezePlayers implements Listener {
	
	private Game game;
	private Boolean freeze = false;
	
	public FreezePlayers(Game game) {
		this.game = game;
		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public void enable() { freeze = true; }
	public void disable() { freeze = false; }
	
	public Boolean arePlayersFrozen() {
		if (freeze == true)
			return true;
		else return false;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (freeze == false) return;
		if (game.getPlaying().contains(event.getPlayer()) == false) return;
		
		Set<Material> foodItems = new HashSet<Material>();
		foodItems.add(Material.APPLE);
		foodItems.add(Material.BAKED_POTATO);
		foodItems.add(Material.BREAD);
		foodItems.add(Material.CAKE);
		foodItems.add(Material.CAKE_BLOCK);
		foodItems.add(Material.CARROT);
		foodItems.add(Material.COOKED_BEEF);
		foodItems.add(Material.COOKED_CHICKEN);
		foodItems.add(Material.COOKED_FISH);
		foodItems.add(Material.COOKED_MUTTON);
		foodItems.add(Material.COOKED_RABBIT);
		foodItems.add(Material.COOKIE);
		foodItems.add(Material.GOLDEN_APPLE);
		foodItems.add(Material.GOLDEN_CARROT);
		foodItems.add(Material.MELON);
		foodItems.add(Material.MUSHROOM_SOUP);
		foodItems.add(Material.MUTTON);
		foodItems.add(Material.POISONOUS_POTATO);
		foodItems.add(Material.PORK);
		foodItems.add(Material.POTATO);
		foodItems.add(Material.PUMPKIN_PIE);
		foodItems.add(Material.RABBIT_STEW);
		foodItems.add(Material.RAW_BEEF);
		foodItems.add(Material.RAW_CHICKEN);
		foodItems.add(Material.RAW_FISH);
		foodItems.add(Material.ROTTEN_FLESH);
		foodItems.add(Material.SPIDER_EYE);
		
		foodItems.add(Material.EXP_BOTTLE); // Allw XP Bottles
		foodItems.add(Material.POTION); // Allow potions
		
		if (foodItems.contains(event.getPlayer().getItemInHand().getType())) return;
		
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (freeze == false) return;
		if (game.getPlaying().contains(event.getPlayer()) == false) return;
		
		Double fromX = Math.floor(event.getFrom().getX());	Double toX = Math.floor(event.getTo().getX());
		Double fromY = Math.floor(event.getFrom().getY());	Double toY = Math.floor(event.getTo().getY());
		Double fromZ = Math.floor(event.getFrom().getZ());	Double toZ = Math.floor(event.getTo().getZ());
		
		Boolean tp = false;
		if (event.getFrom().getWorld().equals(event.getTo().getWorld()) == false) tp = true;
		if (!fromX.equals(toX)) tp = true;
		if (!fromY.equals(toY)) tp = true;
		if (!fromZ.equals(toZ)) tp = true;
		
		if (tp == true) {
			Location loc = event.getFrom();
			loc.setWorld(event.getFrom().getWorld());
			loc.setX(fromX + 0.5);
			loc.setY(fromY);
			loc.setZ(fromZ + 0.5);
			loc.setYaw(event.getTo().getYaw());
			loc.setPitch(event.getTo().getPitch());
			event.getPlayer().teleport(loc);
			event.setCancelled(true);
		}
	}
	
}
