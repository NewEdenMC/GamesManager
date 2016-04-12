package co.neweden.gamesmanager.game;

import java.util.*;
import java.util.Map.Entry;

import co.neweden.gamesmanager.game.config.Parser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;

import co.neweden.gamesmanager.Game;
import org.bukkit.inventory.ItemStack;

public class Chests implements Listener {
	
	private Game game;
	private Boolean listen = false;
	private Set<GMMap> maps = new HashSet<>();
	private Set<Block> chests = new HashSet<>();
	private String configPath = "";
	private int minToFill = 4;
	private int maxToFill = 7;

	public Chests(Game game) {
		this.game = game;
		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public Chests listenInWorld(GMMap map) { maps.add(map); return this; }
	public Chests listenInWorlds(Set<GMMap> maps) { this.maps.addAll(maps); return this; }
	public Chests setConfigPath(String path) { this.configPath = path; return this; }
	public String getConfigPath() { return configPath; }
	public Chests setRangeOfInvToFill(int min, int max) { this.minToFill = min; this.maxToFill = max; return this; }
	public int getMinToFill() { return this.minToFill; }
	public int getMaxToFill() { return this.maxToFill; }

	public Chests startListening() {
		if (maps.isEmpty())
			maps.addAll(game.worlds().getMaps());
		if (configPath.equals(""))
			configPath = "items";
		listen = true;
		return this;
	}
	
	public Chests stopListening() { listen = false; return this; }
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled() || !listen ||
				!maps.contains(game.worlds().getMap(event.getPlayer().getWorld()))) return;

		Block block = event.getClickedBlock();
		if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
			if (!chests.contains(block)) {
				Chest chest = (Chest) block.getState();
				if (chest.getInventory() instanceof DoubleChestInventory) {
					DoubleChestInventory dChest = (DoubleChestInventory) chest.getInventory();
					chests.add(( (Chest) dChest.getLeftSide().getHolder() ).getBlock());
					chests.add(( (Chest) dChest.getRightSide().getHolder() ).getBlock());
				} else {
					chests.add(block);
				}
				fillContainer(block);
			}
		}
	}
	
	public void fillContainer(Block block) {
		if(block.getState() instanceof Chest) {
			Chest chest = (Chest) block.getState();
			Inventory inv = chest.getInventory();
			inv.clear();
			for (Entry<Integer, ItemStack> item : getRandomChestItems(inv.getSize()).entrySet()) {
				inv.setItem(item.getKey(), item.getValue());
			}
		}
	}
	
	public TreeMap<Integer, ItemStack> getRandomChestItems(Integer slots) {
		TreeMap<Integer, ItemStack> items = new TreeMap<>();
		List<ItemStack> itemArray = game.getConfig().getItemStackList(configPath, null);
		if (itemArray == null) return items;

		Random rand = new Random();
		int amountToAdd = rand.nextInt((this.maxToFill - this.minToFill) + 1) + this.minToFill;

		if (itemArray.size() < amountToAdd) amountToAdd = itemArray.size();

		// Generate a list of random inventory slot numbers
		List<Integer> slotsToAdd = new ArrayList<>();
		while (slotsToAdd.size() <= amountToAdd) {
			int slot = rand.nextInt(slots - 1);
			if (!slotsToAdd.contains(slot)) slotsToAdd.add(slot);
		}

		// Generate a list of random items to add
		List<ItemStack> itemsToAdd = new ArrayList<>();
		while (itemsToAdd.size() <= amountToAdd) {
			int index = rand.nextInt(itemArray.size() - 1);
			itemsToAdd.add(itemArray.get(index));
			itemArray.remove(index);
		}

		for (int i = 0; i <= amountToAdd - 1; i++) {
			ItemStack item = itemsToAdd.get(i);
			items.put(slotsToAdd.get(i), item);
		}
		return items;
	}

}
