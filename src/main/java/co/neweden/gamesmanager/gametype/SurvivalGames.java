package co.neweden.gamesmanager.gametype;

import java.util.List;

import co.neweden.gamesmanager.game.GMMap;
import co.neweden.gamesmanager.game.config.MultiConfig;
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
import co.neweden.gamesmanager.game.countdown_old.Countdown;

public class SurvivalGames extends Game implements GameType, Listener {
	
	String status;
	GMMap gameMap;

	public SurvivalGames() {
		Bukkit.getPluginManager().registerEvents(this, getPlugin());
	}

	public void start() {
		String lobbyWorld = getConfig().getString("lobbyworld");
		worlds().setCurrentMap(worlds().loadMap(lobbyWorld));

		reservedSlots().enable();
		spectate().playersSpectateOnDeath(true);
		setPVP(false);
		new BlockManager(this)
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
			if (getPlugin().getConfig().getStringList(getMapConfigPath() + ".gamespawns").contains(player.getLocation().getWorld().getName() + " " + (int) player.getLocation().getX() + " " + (int) player.getLocation().getY() + " " + (int) player.getLocation().getZ())) {
				sender.sendMessage(Util.formatString("&6This location is already a spawn location."));
				return;
			}
			List<String> locations = getConfig().getStringList(getMapConfigPath() + ".gamespawns");
			locations.add(player.getLocation().getWorld().getName() + " " + (int) player.getLocation().getX() + " " + (int) player.getLocation().getY() + " " + (int) player.getLocation().getZ());
			getConfig().set(getMapConfigPath() + ".gamespawns", locations, MultiConfig.Config.MAP);
			getConfig().saveConfig();
			sender.sendMessage(Util.formatString("&6Game spawn location added"));
		}
	}
	
	@EventHandler
	public void onJoin(GMPlayerJoinGameEvent event) {
		if (!getPlayers().contains(event.getPlayer())) return;
		resetDataForPlayer(event.getPlayer());
		event.getPlayer().sendMessage(Util.formatString("&bWelcome to Hunger Games, current players " + getPlaying().size()));
		switch (status) {
			case "prelobby":
				Bukkit.broadcastMessage(String.format(Util.formatString("&a%s has joined Hunger Games, join now to play!"), event.getPlayer().getName()));
				event.getPlayer().teleport(getConfig().getLocation("lobbyspawn"));
				if (getPlayers().size() >= getConfig().getInt("minPlayersNeeded", 3)) {
					lobby();
				} else {
					event.getPlayer().sendMessage(Util.formatString("&eThe game is currently in pre-lobby, 3 players minimum needed to start."));
				}
				break;
			case "lobby":
				Bukkit.broadcastMessage(String.format(Util.formatString("&a%s has joined Hunger Games, join now to play!"), event.getPlayer().getName()));
				event.getPlayer().teleport(getConfig().getLocation("lobbyspawn"));
				break;
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLeave(GMPlayerLeaveGameEvent event) {
		if (!getPlayers().contains(event.getPlayer())) return;
		int playing = getPlaying().size() - 1;
		
		if (status.equals("lobby")) {
			if (playing <= getConfig().getInt("minPlayersNeeded", 3)) preLobby();
		}
		if (status.equals("inprogress") || status.equals("deathmatch")) {
			if (playing <= 1) { endGame(); return; }
			if (status.equals("inprogress")) {
				if (playing <= 2) { preDeathmatch(); }
			}
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (!getPlayers().contains(event.getEntity())) return;
		int playing = getPlaying().size() - 1;
		if (status.equals("inprogress") || status.equals("deathmatch")) {
			broadcast(String.format("&b%s, there are only %s tributes left!", event.getDeathMessage(), playing));
			event.setDeathMessage(null);
			if (playing <= 1) { endGame(event.getEntity().getKiller()); return; }
			if (status.equals("inprogress")) {
				if (playing <= 2) { preDeathmatch(); }
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onRespawn(PlayerRespawnEvent event) {
		if (!getPlayers().contains(event.getPlayer())) return;
		if (status.equals("prelobby") || status.equals("lobby"))
			event.setRespawnLocation(getConfig().getLocation("lobbyspawn"));
		else
			event.setRespawnLocation(getConfig().getLocation("specspawn"));
	}
	
	private void preLobby() {
		spectate().disableSpectateMode();
		countdown().stopAll();
		status = "prelobby";
		countdown().newCountdown(30, -1)
			.broadcastMessageToGameAt(1, "&bWaiting on players, minimum of 3 required to start.")
			.start();
	}
	
	private void lobby() {
		status = "lobby";
		int time = getConfig().getInt("lobbyCountdownToStart", 60);
		countdown().stopAll();
		broadcast("&eMinimum players reached, game will start in 1 minute"); // TODO: Add more output
		countdown().newCountdown(time)
			.setBossBarForGameAt(time, "Game will start in %counter%!")
			.callMethodAt(1, this, "preIP")
			.start();
	}
	
	public void preIP() {
		status = "inprogress";
		spectate().enableSpectateMode();
		freezePlayers().enable();
		reservedSlots().onlyKickSpectators(true);

		gameMap = worlds().loadMap(getConfig().getString("gameMap"));
		worlds().setCurrentMap(gameMap);
		List<Location> spawnsList = getConfig().getLocationList("gamespawns", null, true);
		Location[] spawns = spawnsList.toArray(new Location[spawnsList.size()]);
		Player[] players = getPlaying().toArray(new Player[getPlaying().size()]);
		// TODO: test with multiple players, test with more players than spawns
		for (int i=0; i < spawns.length; i++) {
			if (i < players.length && i < spawns.length) {
				players[i].teleport(spawns[i]);
			}
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				resetDataForPlayers();
			}
		}.runTaskLater(getPlugin(), 1L);
		Countdown cd = countdown().newCountdown(15)
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
		freezePlayers().disable();
		stats().startListening();
		broadcast("&eThe game has started!");
		new Chests(this).startListening();
		countdown().newCountdown(30, -1)
			.broadcastMessageToGameAt(1, "&bCurrent players: %playing%")
			.start();
		int grace = getConfig().getInt("gracePeriodLength", 30);
		if (grace > 0) {
			countdown().newCountdown(grace)
				.broadcastMessageToGameAt(grace, String.format("&b%s second PvP grace period has started!", grace))
				.callMethodAt(0, this, "enablePVP")
				.broadcastMessageToGameAt(0, "&bPvP Grace Period now over!")
				.start();
		}
		countdown().newCountdown(getConfig().getInt("timeToDeathmatch", 600))
			.broadcastMessageToGameAt(300, "&cDeathmatch in 5 minutes")
			.broadcastMessageToGameAt(60, "&cDeathmatch in 1 minute, get ready!")
			.callMethodAt(0, this, "preDeathmatch")
			.start();
	}

	public void enablePVP() { setPVP(true); }

	public void preDeathmatch() {
		freezePlayers().enable();
		status = "deathmatch";
		int countdown = getConfig().getInt("countdownToDeathmatch", 30);
		broadcast(String.format("&cDeathmatch stats in %s seconds, get ready!", countdown));

		List<Location> spawnsList = getConfig().getLocationList("dmspawns", null, true);
		Location[] spawns = spawnsList.toArray(new Location[spawnsList.size()]);
		Player[] players = getPlaying().toArray(new Player[getPlaying().size()]);
		// TODO: test with multiple players, test with more players than spawnsy
		for (int i=0; i < spawns.length; i++) {
			if (i < players.length) {
				players[i].teleport(spawns[i]);
			}
		}
		for (Player player : spectate().getSpectators()) {
			player.teleport(getConfig().getLocation("specspawn"));
		}

		Location dmCentre = getConfig().getLocation("dmcentre");
		int dmRadius = getConfig().getInt("dmborderradius", 25);
		worlds().setWorldBorder(gameMap.getWorld(), dmCentre, dmRadius);

		countdown().newCountdown(countdown)
			.callMethodAt(0, this, "deathmatch")
			.start();
	}
	
	public void deathmatch() {
		freezePlayers().disable();
		status = "deathmatch";
		broadcast("&bDeathmatch has begun!");
		countdown().newCountdown(getConfig().getInt("deathmatchLength", 120))
			.broadcastMessageToGameAt(60, "&cDeathmatch will end in 60 seconds.")
			.broadcastMessageToGameAt(30, "&cDeathmatch will end in 30 seconds.")
			.broadcastMessageToGameAt(15, "&cDeathmatch will end in 15 seconds.")
			.callMethodAt(0, this, "endGame")
			.start();
	}
	
	public void endGame() { endGame(null); }
	public void endGame(Player forceWinner) {
		countdown().stopAll();
		freezePlayers().disable();
		stats().stopListening();
		status = "endgame";
		stats().renderTopList(stats().getCurrentPlayTimes(), 3, forceWinner);
		final Game game = this;
		new BukkitRunnable() {
			@Override
			public void run() {
				spectate().disableSpectateMode();
				GamesManager.restartGame(game);
			}
		}.runTaskLater(getPlugin(), 15 * 20L);
	}
	
}
