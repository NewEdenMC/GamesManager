package co.neweden.gamesmanager.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import co.neweden.gamesmanager.game.config.Parser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import co.neweden.gamesmanager.Game;

public class Chests implements Listener {
	
	private Game game;
	private Boolean listen = false;
	private Set<GMMap> maps = new HashSet<>();
	private Set<Block> chests = new HashSet<>();
	private String configPath = "";
	private int minToFill = 4;
	private int maxToFill = 7;
	private boolean dynamicFill = false;
	
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
	public Chests setDynamicRangeFill(boolean dynamicFill) { this.dynamicFill = dynamicFill; return this; }
	public boolean getDynamicRangeFill() { return this.dynamicFill; }
	
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
					InventoryHolder left = dChest.getLeftSide().getHolder();
					InventoryHolder right = dChest.getRightSide().getHolder();
					chests.add(((Chest) left).getBlock());
					chests.add(((Chest) right).getBlock());
				} else {
					chests.add(block);
				}
				fillContainer(block);
			}
		}
	}
	
	public void fillContainer(Block block) {
		if(block.getState() instanceof InventoryHolder) {
			Inventory inv = Bukkit.createInventory(null, 9);
			BlockState state = block.getState();
			if (state instanceof Chest) {
				Chest chest = (Chest) state;
				inv = chest.getInventory();
			}
			inv.clear();
			for (ItemStackWrapper item : getRandomChestItems(inv.getSize())) {
				inv.setItem(item.getSlot(), item.getItemStack());
			}
		}
	}
	
	public Set<ItemStackWrapper> getRandomChestItems(int slots) {
		Set<ItemStackWrapper> items = new HashSet<>();
		if (game.getConfig().isList(configPath)) {
			List<ItemStackWrapper> itemArray = new ArrayList<>();
			itemArray.addAll(getConfigItemList());
			Random rand = new Random();
			int amountToAdd = rand.nextInt((this.maxToFill - this.minToFill) + 1) + this.minToFill; // add support for dynamic range
			
			if (itemArray.size() < amountToAdd) amountToAdd = itemArray.size();
			
			// Generate a list of random inventory slot numbers
			List<Integer> slotsToAdd = new ArrayList<>();
			while (slotsToAdd.size() <= amountToAdd) {
				int slot = rand.nextInt(slots - 1);
				if (slotsToAdd.contains(slot) == false) slotsToAdd.add(slot);
			}
			
			// TODO: Add support for chance values
			
			// Generate a list of random items to add
			List<ItemStackWrapper> itemsToAdd = new ArrayList<ItemStackWrapper>();
			while (itemsToAdd.size() <= amountToAdd) {
				int index = rand.nextInt(itemArray.size() - 1);
				itemsToAdd.add(itemArray.get(index));
				itemArray.remove(index);
			}
			
			for (int i = 0; i <= amountToAdd - 1; i++) {
				ItemStackWrapper item = itemsToAdd.get(i);
				item.setSlot(slotsToAdd.get(i));
				items.add(item);
			}
		}
		return items;
	}
	
	public List<ItemStackWrapper> getConfigItemList() {
		List<ItemStackWrapper> items = new ArrayList<>();
		if (!game.getConfig().isList(configPath)) return items;
		for (Object item : game.getConfig().getList(configPath)) {
			if (Parser.verifyItemStack(item.toString())) {
				items.add(Parser.parseItemStack(item.toString()));
			}
		}
		return items;
	}
	
}
