package co.neweden.gamesmanager.game;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.GamesManager;
import co.neweden.gamesmanager.Util;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.*;

public class Lobby implements Listener {

    private Game game;
    public LobbyState state = LobbyState.STOP;
    public Location lobbySpawn;
    public Integer minPlayersNeeded;
    public Integer lobbyCountdownToStart;
    public List<String> availableMaps = new ArrayList<>();
    private Map<Player, String> votes = new HashMap<>();
    private Boolean votingActive = false;
    private String selectedMap;
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
        if (votingActive) {
            sendVotingMessage(event.getPlayer());
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
                .at(lobbyCountdownToStart).callMethod(this, "mapVoting")
                .at(5).callMethod(this, "stopVoting")
                .at(0).callMethod(this, "postLobby")
                .start();
    }

    public void mapVoting() {
        if (availableMaps == null || availableMaps.size() == 0) return;
        selectedMap = availableMaps.get(0);
        if (availableMaps.size() > 1) {
            for (Player player : game.getPlayers()) {
                votingActive = true;
                sendVotingMessage(player);
            }
        }
    }

    private void sendVotingMessage(Player player) {
        player.sendMessage(Util.formatString("&eVoting for maps is now active!"));
        player.sendMessage(Util.formatString("&bTo vote for a map click on its name in the list below, to change your vote just click again."));
        for (String map : availableMaps) {
            TextComponent text = new TextComponent(Util.formatString("&f- "));
            text.addExtra(Util.formatString("&b") + map);
            text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gamesmanager vote " + map));
            text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Util.formatString("&eClick to vote for &e" + map)).create()));
            player.spigot().sendMessage(text);
        }
    }

    public void voteCommand(Player player, String mapName) {
        if (!votingActive) {
            player.sendMessage(Util.formatString("&cVoting for maps is currently not available."));
            return;
        }
        if (availableMaps.contains(mapName)) {
            votes.put(player, mapName);
            game.broadcast(String.format("&c%s&7 voted for &c%s", player.getDisplayName(), mapName));
        } else {
            player.sendMessage(String.format(Util.formatString("&cThe map %s is not available to vote for."), mapName));
        }
    }

    public void stopVoting() {
        if (!votingActive) return;
        votingActive = false;
        TreeMap<String, Integer> count = new TreeMap<>();
        for (String map : votes.values()) {
            if (count.get(map) == null)
                count.put(map, 1);
            else
                count.put(map, count.get(map) + 1);
        }
        if (count.size() > 0) selectedMap = count.lastKey();
        game.broadcast("&eVoting is now finished, the chosen map is " + selectedMap);
    }

    public void postLobby() {
        state = LobbyState.STOP;
        GMMap gameMap = game.worlds().loadMap(selectedMap);
        if (gameMap == null) {
            game.kickAllPlayers("The game cannot continue as there are no maps available to play on.");
            String newLine = System.getProperty("line.separator");
            game.getPlugin().getLogger().severe(String.format(
                    "[%s] Game has been stopped due to either no maps being available to load or the map selected was not valid, map selected: %s" + newLine +
                    "- Check the config for Game Type %s to ensure that lobby.availableMaps is a list and is not empty" + newLine +
                    "- If the map name above is not null, check to make sure there is a valid map config for that map and that it can be used with this Game Type",
                    game.getName(), selectedMap, game.getTypeName()
            ));
            GamesManager.stopGame(game);
            return;
        }
        game.worlds().setCurrentMap(gameMap);
        game.getGameTypeInstance().postLobby(gameMap);
    }

}
