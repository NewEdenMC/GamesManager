package co.neweden.gamesmanager.gametype;

import java.util.List;

import co.neweden.gamesmanager.game.GMMap;
import co.neweden.gamesmanager.game.Lobby;
import co.neweden.gamesmanager.game.config.MultiConfig;
import co.neweden.gamesmanager.game.countdown.Countdown;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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

public class SurvivalGames extends Game implements GameType, Listener {
	
	String status;
	GMMap gameMap;
	Location dmCentre;
	private Lobby lobby;

	public SurvivalGames() {
		Bukkit.getPluginManager().registerEvents(this, getPlugin());
	}

	public void start() {
		reservedSlots().enable();
		spectate().playersSpectateOnDeath(true);
		setPVP(false);
		new BlockManager(this)
			.setConfigPath("allowedBlocks")
			.filterType(FilterType.WHITELIST)
			.startListening();
		status = "lobby";
		lobby = new Lobby(this);
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
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onJoin(GMPlayerJoinGameEvent event) {
		if (event.isCancelled() || !getPlayers().contains(event.getPlayer())) return;
		if (status.equals("lobby")) {
			Bukkit.broadcastMessage(String.format(Util.formatString("&a%s has joined Survival Games, type &e/join %s&a now to play!"), event.getPlayer().getName(), getName()));
		}
		resetDataForPlayer(event.getPlayer());
		event.getPlayer().sendMessage(Util.formatString("&bWelcome to Survival Games, current players " + getPlaying().size()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLeave(GMPlayerLeaveGameEvent event) {
		if (event.isCancelled() || !getPlayers().contains(event.getPlayer())) return;
		int playing = getPlaying().size() - 1;
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

	public void postLobby(GMMap map) {
		gameMap = map;
		preIP();
	}

	public void preIP() {
		status = "inprogress";
		spectate().enableSpectateMode();
		freezePlayers().enable();
		reservedSlots().onlyKickSpectators(true);

		dmCentre = getConfig().getLocation("dmcentre");

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
		countdown().newCountdown()
				.at(15).broadcastMessage("&bGame will start in 15 seconds, get ready!")
				.at(10).broadcastMessage("&bGame will start in 10 seconds!")
				.at(0).callMethod(this, "inprogress")
				.start();
		countdown().newCountdown(1, 10, 5)
				.at(1).broadcastTitle("&b%counter%", "&aseconds until game begins!")
				.start();
	}
	
	public void inprogress() {
		status = "inprogress";
		freezePlayers().disable();
		stats().startListening();
		broadcastTitle("", "&eThe game has started!", 10L, 40L, 10L);
		new Chests(this).startListening();
		countdown().newCountdown(120, 0, -1)
				.at(0).broadcastMessage("&bCurrent players: %playing%")
				.start();
		int grace = getConfig().getInt("gracePeriodLength", 30);
		if (grace > 0) {
			countdown().newCountdown()
					.at(grace).broadcastMessage(String.format("&b%s second PvP grace period has started!", grace))
					.at(0).callMethod(this, "enablePVP")
					.at(0).broadcastMessage("&bPvP Grace Period now over!")
					.start();
		}
		countdown().newCountdown(getConfig().getInt("timeToDeathmatch", 600))
				.at(300).broadcastMessage("&cDeathmatch in 5 minutes")
				.at(60).broadcastMessage("&cDeathmatch in 1 minute, get ready!")
				.at(60).displayBossBar(Bukkit.createBossBar(Util.formatString("&e&lDeathmatch starting soon, get ready!"), BarColor.PURPLE, BarStyle.SOLID))
				.at(0).callMethod(this, "preDeathmatch")
				.start();
	}

	public void enablePVP() { setPVP(true); }

	public void preDeathmatch() {
		freezePlayers().enable();
		status = "deathmatch";
		int countdown = getConfig().getInt("countdownToDeathmatch", 15);
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

		int dmRadius = getConfig().getInt("dmborderradius", 25);
		worlds().setWorldBorder(gameMap.getWorld(), dmCentre, dmRadius);

		countdown().newCountdown(countdown)
			.at(0).callMethod(this, "deathmatch")
			.start();
	}
	
	public void deathmatch() {
		freezePlayers().disable();
		status = "deathmatch";
		broadcast("&bDeathmatch has begun!");
		countdown().newCountdown(getConfig().getInt("deathmatchLength", 120))
			.at(60).broadcastMessage("&cDeathmatch will end in 60 seconds.")
			.at(30).broadcastMessage("&cDeathmatch will end in 30 seconds.")
			.at(15).broadcastMessage("&cDeathmatch will end in 15 seconds.")
			.at(0).callMethod(this, "endGame")
			.start();
	}
	
	public void endGame() { endGame(null); }
	public void endGame(Player forceWinner) {
		countdown().stopAll();
		freezePlayers().disable();
		stats().stopListening();
		status = "endgame";
		stats().renderTopList(stats().getCurrentPlayTimes(), 3, forceWinner);
		if (forceWinner == null)
			randomFireworks(dmCentre, 15);
		else
			randomFireworks(forceWinner, 15);
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
