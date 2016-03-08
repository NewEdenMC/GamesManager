package co.neweden.gamesmanager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class GMMain extends JavaPlugin {
	
	public final Logger logger = Logger.getLogger("Minecraft");
	
	@Override
	public void onEnable() {
		GamesManager.plugin = this;
		GamesManager.event = new Event(this);
		this.saveDefaultConfig();
		getCommand("gamesmanager").setExecutor(new CommandManager(this));
		getCommand("join").setExecutor(new CommandManager(this));
		initLoadGames();
		getServer().getPluginManager().registerEvents(GamesManager.event, this);
	}
	
	@Override
	public void onDisable() {
		for (Game game : GamesManager.getEnabledGames()) {
			GamesManager.stopGame(game);
		}
		try {
			FileUtils.deleteDirectory(new File("GamesManager_TempWorld"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
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
		getLogger().info(String.format("Attempting to load %s", name));
		String type = getConfig().getString("games." + name + ".type", null);
		Boolean enabled = getConfig().getBoolean("games." + name + ".enabled", false);

		if (type == null || enabled == false) {
			getLogger().info(String.format("Game %s is either disabled or no Game Type is specified, skipping", name));
			return null;
		}

		GameType typeClass;
		try {
			typeClass = instantiateGameType("co.neweden.gamesmanager.gametype." + type, GameType.class);
		} catch (IllegalStateException ex) {
			getLogger().severe(String.format("Unable to find or load the Game Type for %s, make sure  the correct Game Type is provided in the %s config.", name, getName()));
			ex.printStackTrace();
			return null;
		}

		Game game = (Game) typeClass;
		game.gameType = typeClass;
		game.gameTypeName = type;
		game.gameName = name;
		game.construct();
		GamesManager.games.put(name, game);
		typeClass.start();
		getLogger().info(String.format("Game %s now loaded", name));
		return (Game) typeClass;
	}

	private GameType instantiateGameType(String className, Class<GameType> type){
		try{
			return type.cast(Class.forName(className).newInstance());
		} catch(final InstantiationException e){
			throw new IllegalStateException(e);
		} catch(final IllegalAccessException e){
			throw new IllegalStateException(e);
		} catch(final ClassNotFoundException e){
			throw new IllegalStateException(e);
		}
	}

}
