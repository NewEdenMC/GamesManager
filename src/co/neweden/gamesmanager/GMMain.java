package co.neweden.gamesmanager;

import java.io.File;
import java.util.Arrays;
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
		loadGames();
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(new Event(this), this);
	}
	
	@Override
	public void onDisable() {
		logger.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
	}
	
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (args.length == 0) {
			if (sender.hasPermission("gamesmanager.version"))
				sender.sendMessage(String.format(Util.formatString("&b%s &aversion %s&b"), getName(), getDescription().getVersion()));
			else
				sender.sendMessage(String.format(Util.formatString("&b%s"), getName()));
			
			sender.sendMessage(String.format(Util.formatString("&bRun &e%s&b followed by the name of a game for commands specific to that game."), commandLabel));
			return true;
		}
		
		Game game = GamesManager.getGameByName(args[0]);
		if (game == null) {
			sender.sendMessage(String.format(Util.formatString("&cThe game %s either does not exist or is diabled."), args[0]));
			return true;
		}
		
		if (sender.hasPermission("gamesmanager.game." + game.getName()) == false) {
			if (sender.hasPermission("gamesmanager.game.*") == false) {
				sender.sendMessage(String.format(Util.formatString("&cYou do not have permission to run commands for game: %s"), game.getName()));
				return true;
			}
		}
		
		if (game.getTypeClass() == null) {
			sender.sendMessage(String.format(Util.formatString("&cThe game %s is of the type %s however the matching type class does not exist."), game.getName(), game.getType()));
			return true;
		}
		
		String[] gArgs = Arrays.copyOfRange(args, 1, args.length);
		game.getTypeClass().onCommand(sender, gArgs);
		
		return true;
	}
	
	private void loadGames() {
		logger.info(String.format("[%s] Setting up games", getDescription().getName()));
		getDataFolder().mkdir();
		File[] files;
		try {
			files = getDataFolder().listFiles();
		} catch (NullPointerException e) { return; }
		
		for (int i = 0; i < files.length; i++) {
			String fName = files[i].getName();
			// Skip to next file if this is the config file, is not a file and isn't a YML file
			if (files[i].equals("config.yml") || !files[i].isFile() || !fName.substring(fName.length() - 4, fName.length()).equals(".yml")) {
				continue;
			}
			String name = fName.substring(0, fName.length() - 4);
			if (loadGame(name) == true)
				logger.info(String.format("[%s] Game %s now loaded", getDescription().getName(), name));
		}
		
		if (GamesManager.games.isEmpty()) {
			logger.info(String.format("[%s] No games to load", getDescription().getName()));
		}
	}
	
	private boolean loadGame(String name) {
		Game game = new Game(this, name);
		if (game.getConfig().getBoolean("enabled", false) == false)
			return false;
		
		GameType gameClass = game.getTypeClass();
		if (gameClass == null) return false;
		Bukkit.getServer().getPluginManager().registerEvents((Listener) gameClass, this);
		GamesManager.games.put(name, game);
		gameClass.start();
		return true;
	}
	
}
