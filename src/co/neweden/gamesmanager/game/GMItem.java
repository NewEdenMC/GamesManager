package co.neweden.gamesmanager.game;

import org.bukkit.inventory.ItemStack;

public class GMItem {
	
	ItemStack itemstack;
	int slot = 0;
	
	public void setItemStack(ItemStack itemstack) { this.itemstack = itemstack; }
	public ItemStack getItemStack() { return this.itemstack; }
	
	public void setSlot(int slot) { this.slot = slot; }
	public Integer getSlot() { return this.slot; }
	
}
