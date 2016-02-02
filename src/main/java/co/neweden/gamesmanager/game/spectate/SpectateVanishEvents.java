package co.neweden.gamesmanager.game.spectate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

public class SpectateVanishEvents implements Listener {
	
	/*
	 * Credit to VanishNoPacket GitHub for events and some code :)
	 */
	
	Spectate instance;
	
	public SpectateVanishEvents(Spectate instance) {
		this.instance = instance;
		Bukkit.getServer().getPluginManager().registerEvents(this, instance.game.getPlugin());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		generalEvent(event, event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPickup(PlayerPickupItemEvent event) {
		generalEvent(event, event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onFoodChange(FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof Player)
			generalEvent(event, (Player) event.getEntity());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBucketFill(PlayerBucketFillEvent event) {
		generalEvent(event, event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent event) {
		generalEvent(event, event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShear(PlayerShearEntityEvent event) {
		generalEvent(event, event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDamage(EntityDamageEvent event) {
		Player cancelPlayer = null;
		
		// Cancel if damaged player is hidden
		if (event.getEntity() instanceof Player)
			cancelPlayer = (Player) event.getEntity();
		
		// Cancel if damager is hidden
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent) event;
			if (ev.getDamager() instanceof Player) {
				cancelPlayer = (Player) ev.getDamager();
			} else if (ev.getDamager() instanceof Projectile) { 
				Projectile projectile = (Projectile) ev.getDamager(); 
				if ((projectile.getShooter() != null) && (projectile.getShooter() instanceof Player)) { 
					cancelPlayer = (Player) projectile.getShooter();
				}
			}
		}
		generalEvent(event, cancelPlayer);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityTarget(EntityTargetEvent event) {
		if (event.getTarget() instanceof Player)
			generalEvent(event, (Player) event.getTarget());
	}
	
	private void generalEvent(Cancellable event, Player player) {
		if (event.isCancelled() == false &&
			instance.spectators.contains(player))
			{
				event.setCancelled(true);
			}
	}
	
}
