package co.neweden.gamesmanager;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class GMMain extends JavaPlugin implements Listener {
	
	public final Logger logger = Logger.getLogger("Minecraft");
	
	@Override
	public void onEnable() {
		GamesManager.plugin = this;
		this.saveDefaultConfig();
		getCommand("gamesmanager").setExecutor(new CommandManager(this));
		initLoadGames();
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(new Event(this), this);

	}
	
	@Override
	public void onDisable() {
		logger.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
	}
	
	private void initLoadGames() {
		logger.info(String.format("[%s] Setting up games", getDescription().getName()));
		getDataFolder().mkdir();
		
		for (String name : getGameConfigs().keySet()) {
			loadGame(name);
		}
		
		if (GamesManager.games.isEmpty()) {
			logger.info(String.format("[%s] No games to load", getDescription().getName()));
		}
	}
	
	protected Map<String, File> getGameConfigs() {
		File[] files;
		Map<String, File> configs = new HashMap<String, File>();
		try {
			files = getDataFolder().listFiles();
		} catch (NullPointerException e) { return configs; }
		
		for (int i = 0; i < files.length; i++) {
			String fName = files[i].getName();
			// Skip to next file if this is the config file, is not a file and isn't a YML file
			if (fName.equals("config.yml") || !files[i].isFile() || !fName.substring(fName.length() - 4, fName.length()).equals(".yml")) {
				continue;
			}
			configs.put(fName.substring(0, fName.length() - 4), files[i]);
		}
		return configs;
	}
	
	protected Game loadGame(String name) {
		Game game = new Game(this, name);
		if (game.getConfig().getBoolean("enabled", false) == false)
			return null;
		
		GameType gameClass = game.getTypeClass();
		if (gameClass == null) return null;
		Bukkit.getServer().getPluginManager().registerEvents((Listener) gameClass, this);
		GamesManager.games.put(name, game);
		gameClass.start();
		logger.info(String.format("[%s] Game %s now loaded", getDescription().getName(), name));
		return game;
	}
	
}
