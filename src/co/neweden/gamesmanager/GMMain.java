package co.neweden.gamesmanager;

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
		this.saveDefaultConfig();
		setupGames();
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
			sender.sendMessage(String.format(Util.formatString("&cYou do not have permission to run commands for game: %s"), game.getName()));
			return true;
		}
		
		if (game.getTypeClass() == null) {
			sender.sendMessage(String.format(Util.formatString("&cThe game %s is of the type %s however the matching type class does not exist."), game.getName(), game.getType()));
			return true;
		}
		
		String[] gArgs = Arrays.copyOfRange(args, 1, args.length);
		game.getTypeClass().onCommand(sender, gArgs);
		
		return true;
	}
	
	private void setupGames() {
		if (GamesManager.getEnabledGames() == null) {
			logger.info(String.format("[%s] Unable to get any games from the config or all games have been disabled, no games will be run", getDescription().getName()));
			return;
		}
		for (Game game : GamesManager.getEnabledGames()) {
			if (game.getTypeClass() != null) {
				GameType gameClass = game.getTypeClass();
				Bukkit.getServer().getPluginManager().registerEvents((Listener) gameClass, this);
				gameClass.start();
				logger.info(String.format("[%s] Starting game %s of type %s", getDescription().getName(), game.getName(), game.getType()));
			}
		}
	}
	
}
