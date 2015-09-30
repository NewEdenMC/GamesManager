package co.neweden.gamesmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class GamesManager {
	
	protected static GMMain plugin;
	protected static HashMap<String, Game> games = new HashMap<String, Game>();
	
	private GamesManager() { }
	
	public static Set<Game> getAllGames() { return getGames(false); }
	public static Set<Game> getEnabledGames() { return getGames(true); }
	
	private static Set<Game> getGames(Boolean returnOnlyEnabledGames) {
    	return new HashSet<Game>(games.values());
	}
	
	public static void restartGame(Game game) {
		if (games.containsKey(game.getName()) == false) return;
		
		String gameName = game.getName();
		game.cleanUp();
		games.remove(game.getName());
		for (Game newGame : getEnabledGames()) {
			if (newGame.getName().equals(gameName) == false ||
				game.isEnabled() == false ||
				game.getTypeClass() == null) continue;
			
			Bukkit.getServer().getLogger().info(String.format("[%s] Restarting game %s of type %s", plugin.getDescription().getName(), game.getName(), game.getType()));
			GameType gameClass = newGame.getTypeClass();
			Bukkit.getServer().getPluginManager().registerEvents((Listener) gameClass, plugin);
			gameClass.start();
		}
	}
	
	public static boolean isPlayerInAnyGame(Player player) {
		if (getGameByPlayer(player) != null)
			return true;
		else
			return false;
	}
	
	public static Game getGameByName(String name) {
		for (Game game : getEnabledGames()) {
			if (game.getName().equals(name))
				return game;
		}
		return null;
	}
	
	public static Game getGameByPlayer(Player player) {
		for (Game game : getEnabledGames()) {
			if (game.getPlayers().contains(player))
				return game;
		}
		return null;
	}
	
	public static Game getGameByWorld(World world) {
		for (Game game : getEnabledGames()) {
			if (game.worlds().getWorlds().contains(world))
				return game;
		}
		return null;
	}
	
	public static void kickPlayer(Player player, String message) { kickPlayer(player, message, "GamesManager"); }
	public static void kickPlayer(Player player, String message, String sender) {
		if (plugin.getConfig().isString("kickPlayersTo.server")) {
			return;
		}
		if (plugin.getConfig().isString("kickPlayersTo.world")) {
			String cWorld = plugin.getConfig().getString("kickPlayersTo.world");
			if (Bukkit.getWorld(cWorld) != null) {
				player.teleport(Bukkit.getWorld(cWorld).getSpawnLocation());
				player.sendMessage(Util.formatString(String.format("&e[&6%s&e] &6%s", sender, message)));
			} else {
				Bukkit.getLogger().log(Level.WARNING, String.format("[%s] Could not kick %s to world %s, world does not exist, kicking from server instead.", plugin.getName(), player.getName(), cWorld));
				player.kickPlayer(message);
			}
		}
		// Due to a random bug in the Bukkit events system, sometimes players aren't removed from
		// the player lists when they are kicked from a game, this "should" resolve this issue.
		GamesManager.refreshPlayerLists();
	}
	
	public static void refreshPlayerLists() {
		for (Game game : GamesManager.games.values()) {
			game.refreshPlayerList();
		}
	}
	
}
