package co.neweden.gamesmanager;

import org.bukkit.command.CommandSender;

public interface GameType {
	
	void start();
	void onCommand(CommandSender sender, String[] args);
	
}
