package co.neweden.gamesmanager;

import org.bukkit.command.CommandSender;

public interface GameType {
	
	public void start();
	public void onCommand(CommandSender sender, String[] args);
	
}
