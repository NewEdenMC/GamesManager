package co.neweden.gamesmanager;

import co.neweden.gamesmanager.game.GMMap;
import org.bukkit.command.CommandSender;

public interface GameType {
	
	void start();
	void onCommand(CommandSender sender, String[] args);
	void postLobby(GMMap map);
	
}
