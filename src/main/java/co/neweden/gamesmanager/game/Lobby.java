package co.neweden.gamesmanager.game;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.GamesManager;
import co.neweden.gamesmanager.Util;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;
import co.neweden.gamesmanager.game.config.MultiConfig;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.ChatPaginator;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.util.*;

public class Lobby implements Listener {

    private Game game;
    public LobbyState state = LobbyState.STOP;
    public Location lobbySpawn;
    public Integer minPlayersNeeded;
    public Integer lobbyCountdownToStart;
    public List<String> availableMaps = new ArrayList<>();
    private ArrayList<BaseComponent> componentMapsList = new ArrayList<>();
    private Map<String, YamlConfiguration> mapConfigs = new HashMap<>();
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
        GMMap map = game.worlds().loadMap(lobbyWorld);
        game.worlds().setCurrentMap(map);

        map.getWorld().setPVP(false);
        new BlockManager(game).listenInWorld(map).filterType(BlockManager.FilterType.WHITELIST).startListening();

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

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player) ||
            !game.getPlayers().contains(event.getEntity()) || state.equals(LobbyState.STOP)) return;
        event.setCancelled(true);
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

    private void buildVotingList() {
        for (String map : availableMaps) {
            try {
                YamlConfiguration mapConfig = game.getConfig().getIndividualConfig(MultiConfig.Type.MAP, map + ".yml");
                mapConfigs.put(map, mapConfig);
                String displayName = mapConfig.getString("mapInfo.displayName", map);
                String description = mapConfig.getString("mapInfo.description", null);

                TextComponent line = new TextComponent(Util.formatString("&f- &b" + displayName));
                line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gamesmanager vote " + map));

                ComponentBuilder hover = new ComponentBuilder(Util.formatString("&eClick to vote for &e" + displayName));
                if (description != null)
                    hover.append(Util.formatString("\n&eDescription:\n" + Util.addLineBreaks(description, (ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH / 3) * 2)));

                line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover.create()));
                componentMapsList.add(line);
            } catch (FileNotFoundException ex) {
                game.getPlugin().getLogger().warning(String.format("[%s] Config file for map %s not found, this map will not be available for voting", game.getName(), map));
            }
        }
    }

    public void mapVoting() {
        if (availableMaps == null || availableMaps.size() == 0) return;
        selectedMap = availableMaps.get(0);
        if (availableMaps.size() > 1) {
            buildVotingList();
            votingActive = true;
            for (Player player : game.getPlayers()) {
                sendVotingMessage(player);
            }
        }
    }

    private void sendVotingMessage(Player player) {
        player.sendMessage(Util.formatString("&eVoting for maps is now active!"));
        player.sendMessage(Util.formatString("&bTo vote for a map click on its name in the list below, to change your vote just click again."));
        for (BaseComponent component : componentMapsList) {
            player.spigot().sendMessage(component);
        }
    }

    public void voteCommand(Player player, String mapName) {
        if (!votingActive) {
            player.sendMessage(Util.formatString("&cVoting for maps is currently not available."));
            return;
        }
        if (availableMaps.contains(mapName)) {
            votes.put(player, mapName);
            game.broadcast(String.format("&c%s&7 voted for &c%s", player.getDisplayName(), mapConfigs.get(mapName).getString("displayName", mapName)));
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
        game.broadcast("&eVoting is now finished, the chosen map is " + mapConfigs.get(selectedMap).getString("displayName", selectedMap));
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
