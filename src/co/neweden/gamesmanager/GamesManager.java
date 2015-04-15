package co.neweden.gamesmanager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class GamesManager {
	
	private static HashMap<String, Game> games = new HashMap<String, Game>();
	
	private GamesManager() { }
	
	private static GMMain getPlugin() {
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("GamesManager");
		if (plugin instanceof GMMain) {
			return (GMMain) plugin;
		} else {
			Bukkit.getServer().getLogger().log(Level.SEVERE, "plugin:GamesManager is not an instance of GMMain, check to make sure you do not have any other plugins with the name GamesManager and you are not running multipe versions of GamesManager.");
			throw new ClassCastException();
		}
	}
	
	public static Set<Game> getAllGames() { return getGames(false); }
	public static Set<Game> getEnabledGames() { return getGames(true); }
	
	private static Set<Game> getGames(Boolean returnOnlyEnabledGames) {
    	Set<String> keys = getPlugin().getConfig().getKeys(true);
    	if (keys.isEmpty()) {
    		return null;
    	}
    	
    	Set<Game> games = new HashSet<Game>();
    	Boolean add;
    	String skey;
    	for (String key : keys) {
    		if (key.length() > 5) {
    			add = true;
    			if (!key.substring(0, 6).equals("games.")) add = false; // add only if key starts with "worlds."
    			skey = key.substring(6);
    			if (skey.contains(".")) add = false; // remove "worlds." and only add if key doesn't container a "."
    			if (returnOnlyEnabledGames == true) { // run code if disabled games should be filtered out
    				if (getPlugin().getConfig().isBoolean("games." + skey + ".enabled")) { // run if value is specified in the config
    					if (getPlugin().getConfig().getBoolean("games." + skey + ".enabled") == false)
    						add = false; // Set add to false if game has been disabled not enabled
    				} else add = false;
    			}
    			if (add == true) {
    				if (GamesManager.games.containsKey(skey) == false) {
    					GamesManager.games.put(skey, new Game(getPlugin(), skey));
    				}
					games.add(GamesManager.games.get(skey));
    				//games.add(new Game(getPlugin(), skey));
    			}
    		}
    	}
    	if (games.size() == 0) return null;
    	
    	return games;
	}
	
	public static void restartGame(Game game) {
		if (games.containsKey(game.getName()) == false) return;
		
		String gameName = game.getName();
		game.cleanUp();
		games.remove(game.getName());
		for (Game newGame : getEnabledGames()) {
			if (newGame.getName().equals(gameName) == false) continue;
			if (game.isEnabled() == false) continue;
			if (game.getTypeClass() == null) continue;
			Bukkit.getServer().getLogger().info(String.format("[%s] Restarting game %s of type %s", getPlugin().getDescription().getName(), game.getName(), game.getType()));
			GameType gameClass = newGame.getTypeClass();
			Bukkit.getServer().getPluginManager().registerEvents((Listener) gameClass, getPlugin());
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
		if (getPlugin().getConfig().isString("kickPlayersTo.server")) {
			return;
		}
		if (getPlugin().getConfig().isString("kickPlayersTo.world")) {
			String cWorld = getPlugin().getConfig().getString("kickPlayersTo.world");
			if (Bukkit.getWorld(cWorld) != null) {
				player.teleport(Bukkit.getWorld(cWorld).getSpawnLocation());
				player.sendMessage(Util.formatString(String.format("&e[&6%s&e] &6%s", sender, message)));
			} else {
				Bukkit.getLogger().log(Level.WARNING, String.format("[%s] Could not kick %s to world %s, world does not exist, kicking from server instead.", getPlugin().getName(), player.getName(), cWorld));
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
