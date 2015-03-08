package co.neweden.gamesmanager.game;

import org.bukkit.configuration.file.FileConfiguration;

import co.neweden.gamesmanager.Game;

public class GameConfig {
	
	private FileConfiguration pConfig;
	private String gPath;
	
	public GameConfig(Game game) {
		pConfig = game.getPlugin().getConfig();
		gPath = "games." + game.getName() + ".";
	}
	
	public String getRootPath() {
		return gPath.substring(0, gPath.length() - 1);
	}
	
}
