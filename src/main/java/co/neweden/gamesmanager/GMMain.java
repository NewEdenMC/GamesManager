package co.neweden.gamesmanager;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class GMMain extends JavaPlugin {
	
	public final Logger logger = Logger.getLogger("Minecraft");
	
	@Override
	public void onEnable() {
		GamesManager.plugin = this;
		this.saveDefaultConfig();
		getCommand("gamesmanager").setExecutor(new CommandManager(this));
		initLoadGames();
		getServer().getPluginManager().registerEvents(new Event(this), this);
	}
	
	@Override
	public void onDisable() {
		logger.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
	}
	
	private void initLoadGames() {
		logger.info(String.format("[%s] Setting up games", getDescription().getName()));
		getDataFolder().mkdir();
		
		for (String name : listGamesFromConfig()) {
			loadGame(name);
		}
		
		if (GamesManager.games.isEmpty()) {
			logger.info(String.format("[%s] No games to load", getDescription().getName()));
		}
	}

	private Set<String> listGamesFromConfig() {
		Set<String> keys = getConfig().getKeys(true);
		if (keys.isEmpty()) {
			return keys;
		}

		Set<String> games = new LinkedHashSet<>();
		Boolean add;
		String skey;
		for (String key : keys) {
			if (key.length() > 5) {
				add = true;
				if (!key.substring(0, 6).equals("games.")) add = false; // add only if key starts with "games."
				skey = key.substring(6);
				if (skey.contains(".")) add = false; // remove "games." and only add if key doesn't container a "."
				if (add == true)
					games.add(skey);
			}
		}
		return games;
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
		if (getConfig().getBoolean("games." + name + ".enabled", false) == false)
			return null;

		Game game = new Game(this, name);

		GameType gameClass = game.getTypeClass();
		if (gameClass == null) return null;
		Bukkit.getServer().getPluginManager().registerEvents((Listener) gameClass, this);
		GamesManager.games.put(name, game);
		gameClass.start();
		getLogger().info(String.format("Game %s now loaded", name));
		return game;
	}
	
}
