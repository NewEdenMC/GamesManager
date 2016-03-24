package co.neweden.gamesmanager.game;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.GamesManager;
import co.neweden.gamesmanager.Util;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;

public class Lobby implements Listener {

    private Game game;
    public LobbyState state = LobbyState.STOP;
    public Location lobbySpawn;
    public Integer minPlayersNeeded;
    public Integer lobbyCountdownToStart;
    public List<String> availableMaps;
    private GMMap gameMap;
    public BarColor barColor;
    public BarStyle barStyle;

    public enum LobbyState { PRE, INPROGRESS, STOP }

    public Lobby(Game game) {
        this.game = game;
        Bukkit.getPluginManager().registerEvents(this, game.getPlugin());

        String lobbyWorld = game.getConfig().getString("lobby.world");
        game.worlds().setCurrentMap(game.worlds().loadMap(lobbyWorld));

        // GameType config
        minPlayersNeeded = game.getConfig().getInt("lobby.minPlayersNeeded", 3);
        lobbyCountdownToStart = game.getConfig().getInt("lobby.countdownToStart", 60);
        availableMaps = game.getConfig().getStringList("lobby.availableMaps");

        // Map config
        lobbySpawn = game.getConfig().getLocation("lobbyspawn");

        barColor = BarColor.PURPLE;
        barStyle = BarStyle.SOLID;

        pre();
    }


    @EventHandler
    public void onJoin(GMPlayerJoinGameEvent event) {
        if (event.isCancelled() || !game.getPlayers().contains(event.getPlayer()) ||
                state.equals(LobbyState.STOP)) return;
        event.getPlayer().teleport(lobbySpawn);
        game.resetDataForPlayer(event.getPlayer());
        if (state.equals(LobbyState.PRE)) {
            if (game.getPlayers().size() >= minPlayersNeeded)
                inProgress();
            else
                event.getPlayer().sendMessage(Util.formatString(String.format("&eThe game is currently in pre-lobby, %s players minimum needed to start.", minPlayersNeeded)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(GMPlayerLeaveGameEvent event) {
        if (event.isCancelled() || !game.getPlayers().contains(event.getPlayer())) return;
        int playing = game.getPlaying().size() - 1;
        if (state.equals(LobbyState.INPROGRESS))
            if (playing < minPlayersNeeded) pre();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!game.getPlayers().contains(event.getPlayer()) || !state.equals(LobbyState.STOP)) return;
        event.setRespawnLocation(lobbySpawn);
    }

    private void pre() {
        state = LobbyState.PRE;
        game.countdown().stopAll();
        game.countdown().newCountdown(30, 0, -1)
                .at(0).broadcastMessage(String.format("&bWaiting on players, minimum of %s required to start.", minPlayersNeeded))
                .start();
    }

    private void inProgress() {
        state = LobbyState.INPROGRESS;
        game.countdown().stopAll();
        game.broadcast("&eMinimum players reached, game will start in 1 minute");
        game.countdown().newCountdown()
                .at(lobbyCountdownToStart).displayBossBar(Bukkit.createBossBar(Util.formatString("&e&lGame starting soon, get ready!"), barColor, barStyle))
                .at(0).callMethod(this, "postLobby")
                .start();
        mapVoting();
    }

    private void mapVoting() {
        if (availableMaps == null || availableMaps.size() == 0) return;
        if (availableMaps.size() == 1) {
            gameMap = game.worlds().loadMap(availableMaps.get(0));
            return;
        }
    }

    public void postLobby() {
        state = LobbyState.STOP;
        if (gameMap == null) {
            game.kickAllPlayers("The game cannot continue as there are no maps available to play on.");
            String newLine = System.getProperty("line.separator");
            game.getPlugin().getLogger().severe(String.format(
                    "[%s] Game has been stopped due to either no maps being available to load or the map selected was not valid, map selected: %s" + newLine +
                    "- Check the config for Game Type %s to ensure that lobby.availableMaps is a list and is not empty" + newLine +
                    "- If the map name above is not null, check to make sure there is a valid map config for that map and that it can be used with this Game Type",
                    game.getName(), gameMap, game.getTypeName()
            ));
            GamesManager.stopGame(game);
            return;
        }
        game.worlds().setCurrentMap(gameMap);
        game.getGameTypeInstance().postLobby(gameMap);
    }

}
