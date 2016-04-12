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
	private Set<GMMap> maps = new HashSet<>();
	private Set<Material> blocks = new HashSet<Material>();
	private String configPath = "";
	private FilterType filterType = FilterType.BLACKLIST;
	
	public enum FilterType { WHITELIST, BLACKLIST }
	
	public BlockManager(Game game) {
		this.game = game;
		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public BlockManager listenInWorld(GMMap map) { maps.add(map); return this; }
	public BlockManager listenInWorlds(Set<GMMap> maps) { this.maps.addAll(maps); return this; }
	public BlockManager setConfigPath(String path) { this.configPath = path; return this; }
	public String getConfigPath() { return this.configPath; }
	public BlockManager addBlock(Material block) { this.blocks.add(block); return this; }
	public BlockManager addBlocks(Set<Material> blocks) { this.blocks.addAll(blocks); return this; }
	public BlockManager listenForBlockPlace(boolean listen) { this.listenPlace = listen; return this; }
	public BlockManager listenForBlockBreak(boolean listen) { this.listenBreak = listen; return this; }
	public BlockManager filterType(FilterType filterType) { this.filterType = filterType; return this; }
	
	public BlockManager startListening() {
		if (maps.isEmpty())
			maps.addAll(game.worlds().getMaps());
		addConfigBlocks();
		if (!listenPlace && !listenBreak) {
			listenPlace = true;
			listenBreak = true;
		}
		listen = true;
		return this;
	}
	
	private void addConfigBlocks() {
		if (!game.getPlugin().getConfig().isList(configPath)) return;

		for (Object item : game.getConfig().getList(configPath)) {
			if (Parser.verifyItemStack(item.toString())) {
				blocks.add(Parser.parseItemStack(item.toString()).getType());
			}
		}
	}
	
	public BlockManager stopListening() { listen = false; return this; }
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled() || !listen || !listenPlace ||
				!game.getPlayers().contains(event.getPlayer())) return;

		if (filterType == FilterType.WHITELIST) {
			if (!blocks.contains(event.getBlock().getType())) event.setCancelled(true);
		} else {
			if (blocks.contains(event.getBlock().getType())) event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled() || !listen || !listenPlace ||
				!game.getPlayers().contains(event.getPlayer())) return;

		if (filterType == FilterType.WHITELIST) {
			if (!blocks.contains(event.getBlock().getType())) event.setCancelled(true);
		} else {
			if (blocks.contains(event.getBlock().getType())) event.setCancelled(true);
		}
	}
	
}
