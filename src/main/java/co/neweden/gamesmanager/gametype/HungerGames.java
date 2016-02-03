package co.neweden.gamesmanager.gametype;

import java.util.List;

import co.neweden.gamesmanager.game.config.MultiConfig;
import co.neweden.gamesmanager.game.config.Parser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.GameType;
import co.neweden.gamesmanager.GamesManager;
import co.neweden.gamesmanager.Util;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;
import co.neweden.gamesmanager.game.BlockManager;
import co.neweden.gamesmanager.game.BlockManager.FilterType;
import co.neweden.gamesmanager.game.Chests;
import co.neweden.gamesmanager.game.countdown.Countdown;

public class HungerGames implements GameType, Listener {
	
	Game game;
	String status;
	
	public HungerGames(Game game) {
		this.game = game;
	}

	public void start() {
		game.reservedSlots().enable();
		game.spectate().playersSpectateOnDeath(true);
		game.setPVP(false);
		game.worlds().saveSnapshots();
		new BlockManager(game)
			.setConfigPath("allowedBlocks")
			.filterType(FilterType.WHITELIST)
			.startListening();
		preLobby();
	}
	
	public void onCommand(CommandSender sender, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(Util.formatString("&aAvailable sub-commands:\n- setspawnpoint"));
			return;
		}
		if (!(sender instanceof Player)) return;
		Player player = (Player) sender;
		if (args[0].equals("setspawnpoint")) {
			if (game.getPlugin().getConfig().getStringList(game.getMapConfigPath() + ".gamespawns").contains(player.getLocation().getWorld().getName() + " " + (int) player.getLocation().getX() + " " + (int) player.getLocation().getY() + " " + (int) player.getLocation().getZ())) {
				sender.sendMessage(Util.formatString("&6This location is already a spawn location."));
				return;
			}
			List<String> locations = game.getConfig().getStringList(game.getMapConfigPath() + ".gamespawns");
			locations.add(player.getLocation().getWorld().getName() + " " + (int) player.getLocation().getX() + " " + (int) player.getLocation().getY() + " " + (int) player.getLocation().getZ());
			game.getConfig().set(game.getMapConfigPath() + ".gamespawns", locations, MultiConfig.Config.MAP);
			game.getConfig().saveConfig();
			sender.sendMessage(Util.formatString("&6Game spawn location added"));
		}
	}
	
	@EventHandler
	public void onJoin(GMPlayerJoinGameEvent event) {
		if (game.getPlayers().contains(event.getPlayer()) == false) return;
		game.resetDataForPlayer(event.getPlayer());
		event.getPlayer().sendMessage(Util.formatString("&bWelcome to Hunger Games, current players " + game.getPlaying().size()));
		switch (status) {
			case "prelobby":
				Bukkit.broadcastMessage(String.format(Util.formatString("&a%s has joined Hunger Games, join now to play!"), event.getPlayer().getName()));
				if (game.getLobbySpawnLocation() != null)
					event.getPlayer().teleport(game.getLobbySpawnLocation());
				if (game.getPlayers().size() >= 3) {
					lobby();
				} else {
					event.getPlayer().sendMessage(Util.formatString("&eThe game is currently in pre-lobby, 3 players minimum needed to start."));
				}
				break;
			case "lobby":
				Bukkit.broadcastMessage(String.format(Util.formatString("&a%s has joined Hunger Games, join now to play!"), event.getPlayer().getName()));
				if (game.getLobbySpawnLocation() != null)
					event.getPlayer().teleport(game.getLobbySpawnLocation());
				break;
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLeave(GMPlayerLeaveGameEvent event) {
		if (game.getPlayers().contains(event.getPlayer()) == false) return;
		int playing = game.getPlaying().size() - 1;
		
		if (status.equals("lobby")) {
			if (playing <= 3) preLobby();
		}
		if (status.equals("inprogress") || status.equals("deathmatch")) {
			if (playing <= 1) { endGame(); return; }
			if (status.equals("inprogress")) {
				if (playing <= 2) { preDeathmatch(); return; }
			}
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (game.getPlayers().contains(event.getEntity()) == false) return;
		int playing = game.getPlaying().size() - 1;
		if (status.equals("inprogress") || status.equals("deathmatch")) {
			game.broadcast(String.format("&b%s, there are only %s tributes left!", event.getDeathMessage(), playing));
			event.setDeathMessage(null);
			if (playing <= 1) { endGame(event.getEntity().getKiller()); return; }
			if (status.equals("inprogress")) {
				if (playing <= 2) { preDeathmatch(); return; }
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onRespawn(PlayerRespawnEvent event) {
		if (game.getPlayers().contains(event.getPlayer()) == false) return;
		if (status.equals("prelobby") || status.equals("lobby"))
			event.setRespawnLocation(game.getLobbySpawnLocation());
		else
			event.setRespawnLocation(game.getSpecSpawnLocation());
	}
	
	private void preLobby() {
		game.spectate().disableSpectateMode();
		game.countdown().stopAll();
		status = "prelobby";
		game.countdown().newCountdown(30, -1)
			.broadcastMessageToGameAt(1, "&bWaiting on players, minimum of 3 required to start.")
			.start();
	}
	
	private void lobby() {
		status = "lobby";
		game.countdown().stopAll();
		game.broadcast("&eMinimum players reached, game will start in 1 minute"); // TODO: Add more output
		game.countdown().newCountdown(60)
			.setBossBarForGameAt(60, "Game will start in %counter%!")
			.callMethodAt(1, this, "preIP")
			.start();
	}
	
	public void preIP() {
		status = "inprogress";
		game.spectate().enableSpectateMode();
		game.freezePlayers().enable();
		game.reservedSlots().onlyKickSpectators(true);
		Location[] spawns = (Location[]) game.getGameSpawnLocations().toArray(new Location[game.getSpawnLocations().size()]);
		Player[] players = (Player[]) game.getPlaying().toArray(new Player[game.getPlaying().size()]);
		// TODO: test with multiple players, test with more players than spawns
		for (int i=0; i < spawns.length; i++) {
			if (i < players.length && i < spawns.length) {
				players[i].teleport(spawns[i]);
			}
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				game.resetDataForPlayers();
			}
		}.runTaskLater(game.getPlugin(), 1L);
		Countdown cd = game.countdown().newCountdown(15)
			.callMethodAt(14, game, "resetDataForPlayers")
			.broadcastMessageToGameAt(15, "&bGame will start in 15 seconds, get ready!")
			.broadcastMessageToGameAt(10, "&bGame will start in 10 seconds!")
			.callMethodAt(0, this, "inprogress")
			.start();
		Countdown cd2 = cd.newCountdownAt(5, 1, 5, false);
			cd2.broadcastMessageToGameAt(1, "&bGame will start in %counter% seconds!")
			.start();
	}
	
	public void inprogress() {
		status = "inprogress";
		game.freezePlayers().disable();
		game.stats().startListening();
		game.broadcast("&eThe game has started!");
		new Chests(game).startListening();
		game.countdown().newCountdown(30, -1)
			.broadcastMessageToGameAt(1, "&bCurrent players: %playing%")
			.start();
		game.countdown().newCountdown(30)
			.broadcastMessageToGameAt(30, "&b30 second PvP grace period has started!")
			.callMethodAt(0, this, "enablePVP")
			.broadcastMessageToGameAt(0, "&bPvP Grace Period now over!")
			.start();
		game.countdown().newCountdown(600)
			.broadcastMessageToGameAt(300, "&cDeathmatch in 5 minutes")
			.broadcastMessageToGameAt(60, "&cDeathmatch in 1 minute, get ready!")
			.callMethodAt(0, this, "preDeathmatch")
			.start();
	}
	
	public void enablePVP() { game.setPVP(true); }
	
	public void preDeathmatch() {
		game.freezePlayers().enable();
		status = "deathmatch";
		game.broadcast("&cDeathmatch stats in 30 seconds, get ready!");
		
		Location[] spawns = (Location[]) game.getDMSpawnLocations().toArray(new Location[game.getDMSpawnLocations().size()]);
		Player[] players = (Player[]) game.getPlaying().toArray(new Player[game.getPlaying().size()]);
		// TODO: test with multiple players, test with more players than spawnsy
		for (int i=0; i < spawns.length; i++) {
			if (i < players.length) {
				players[i].teleport(spawns[i]);
			}
		}
		for (Player player : game.spectate().getSpectators()) {
			player.teleport(game.getSpecSpawnLocation());
		}
		
		Location dmCentre = null;
		if (game.getConfig().isString(game.getMapConfigPath() + ".dmcentre")) {
			if (Parser.verifyLocation(game.getConfig().getString(game.getMapConfigPath() + ".dmcentre"))) {
				dmCentre = Parser.parseLocation(game.getConfig().getString(game.getMapConfigPath() + ".dmcentre"));
			}
		}
		int dmRadius = 25;
		if (game.getPlugin().getConfig().isInt(game.getMapConfigPath() + ".dmborderradius")) {
			dmRadius = game.getPlugin().getConfig().getInt(game.getMapConfigPath() + ".dmborderradius");
		}
		for (Location loc : game.getDMSpawnLocations()) {
			if (loc != null) {
				game.worlds().setWorldBorder(loc.getWorld(), dmCentre, dmRadius);
			}
		}
		
		game.countdown().newCountdown(30)
			.callMethodAt(0, this, "deathmatch")
			.start();
	}
	
	public void deathmatch() {
		game.freezePlayers().disable();
		status = "deathmatch";
		game.broadcast("&bDeathmatch has begun!");
		game.countdown().newCountdown(120)
			.broadcastMessageToGameAt(60, "&cDeathmatch will end in 60 seconds.")
			.broadcastMessageToGameAt(30, "&cDeathmatch will end in 30 seconds.")
			.broadcastMessageToGameAt(15, "&cDeathmatch will end in 15 seconds.")
			.callMethodAt(0, this, "endGame")
			.start();
	}
	
	public void endGame() { endGame(null); }
	public void endGame(Player forceWinner) {
		game.countdown().stopAll();
		game.freezePlayers().disable();
		game.stats().stopListening();
		status = "endgame";
		game.stats().renderTopList(game.stats().getCurrentPlayTimes(), 3, forceWinner);
		new BukkitRunnable() {
			@Override
			public void run() {
				game.spectate().disableSpectateMode();
				game.kickAllPlayers("The game has ended and is now resetting");
				game.worlds().restoreWorlds();
				GamesManager.restartGame(game);
			}
		}.runTaskLater(game.getPlugin(), 15 * 20L);
	}
	
}
