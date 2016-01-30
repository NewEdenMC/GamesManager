package co.neweden.gamesmanager.game;

import java.util.HashSet;
import java.util.Set;

import co.neweden.gamesmanager.game.config.Parser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.Util;

public class BlockManager implements Listener {
	
	private Game game;
	private boolean listenPlace = false;
	private boolean listenBreak = false;
	private boolean listen = false;
	private Set<World> worlds = new HashSet<World>();
	private Set<Material> blocks = new HashSet<Material>();
	private String configPath = "";
	private FilterType filterType = FilterType.BLACKLIST;
	
	public enum FilterType { WHITELIST, BLACKLIST }
	
	public BlockManager(Game game) {
		this.game = game;
		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public BlockManager listenInWorld(World world) { worlds.add(world); return this; }
	public BlockManager listenInWorlds(Set<World> worlds) { this.worlds.addAll(worlds); return this; }
	public BlockManager setConfigPath(String path) { this.configPath = path; return this; }
	public String getConfigPath() { return this.configPath; }
	public BlockManager addBlock(Material block) { this.blocks.add(block); return this; }
	public BlockManager addBlocks(Set<Material> blocks) { this.blocks.addAll(blocks); return this; }
	public BlockManager listenForBlockPlace(boolean listen) { this.listenPlace = listen; return this; }
	public BlockManager listenForBlockBreak(boolean listen) { this.listenBreak = listen; return this; }
	public BlockManager filterType(FilterType filterType) { this.filterType = filterType; return this; }
	
	public BlockManager startListening() {
		if (worlds.isEmpty())
			worlds.addAll(game.worlds().getWorlds());
		addConfigBlocks();
		if (listenPlace == false && listenBreak == false) {
			listenPlace = true;
			listenBreak = true;
		}
		listen = true;
		return this;
	}
	
	private void addConfigBlocks() {
		if (game.getPlugin().getConfig().isList(configPath) == false) return;
		for (Object item : game.getPlugin().getConfig().getList(configPath)) {
			if (Parser.verifyConfigItem(item.toString())) {
				blocks.add(Parser.parseConfigItem(item.toString()).getItemStack().getType());
			}
		}
	}
	
	public BlockManager stopListening() { listen = false; return this; }
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) return;
		if (game.getPlayers().contains(event.getPlayer()) == false) return;
		if (listen == false) return;
		if (listenPlace == false) return;
		if (filterType == FilterType.WHITELIST) {
			if (blocks.contains(event.getBlock().getType()) == false) event.setCancelled(true);
		} else {
			if (blocks.contains(event.getBlock().getType()) == true) event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		if (game.getPlayers().contains(event.getPlayer()) == false) return;
		if (listen == false) return;
		if (listenPlace == false) return;
		if (filterType == FilterType.WHITELIST) {
			if (blocks.contains(event.getBlock().getType()) == false) event.setCancelled(true);
		} else {
			if (blocks.contains(event.getBlock().getType()) == true) event.setCancelled(true);
		}
	}
	
}
