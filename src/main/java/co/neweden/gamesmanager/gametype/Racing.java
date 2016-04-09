package co.neweden.gamesmanager.gametype;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.GameType;
import co.neweden.gamesmanager.GamesManager;
import co.neweden.gamesmanager.Util;
import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;
import co.neweden.gamesmanager.game.GMMap;
import org.bukkit.command.CommandSender;
import co.neweden.gamesmanager.game.Lobby;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Racing extends Game implements GameType, Listener{
    
    private String status;
    private GMMap gameMap;
    private List<DyeColor> colorsFromConfig;
    private HashMap<Player,List<DyeColor>> checkpoints;
    
    public Racing() {
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }
    
    @Override
    public void start() {
        status = "lobby";
        spectate().playersSpectateOnDeath(true);
        setPVP(false);
    }
    
    @EventHandler (priority = EventPriority.MONITOR)
    public void onJoin(GMPlayerJoinGameEvent event) {
        if (event.isCancelled() || !getPlayers().contains(event.getPlayer())) return;
        if (status.equals("lobby")) {
            Bukkit.broadcastMessage(String.format(Util.formatString("&a%s has joined a race, type &e/join %s&a now to play!"), event.getPlayer().getName(), getName()));
        }
        resetDataForPlayer(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(GMPlayerLeaveGameEvent event) {
        if (event.isCancelled() || !getPlayers().contains(event.getPlayer())) return;
        int playing = getPlaying().size() - 1;
        if (status.equals("inprogress")) {
            if (playing <= 1) { endGame(getPlaying().iterator().next()); return; }
            Bukkit.broadcastMessage(String.format(Util.formatString("%s has won the race"), getPlaying().iterator().next().getName()));
        }
    }
    
    @EventHandler
    public void onVehicleExit(VehicleExitEvent event){
        if (!getPlayers().contains((Player)event.getExited())) return;
        if (status.equals("inprogress")) {
            event.setCancelled(true);
        }
    }
    @Override
    public void onCommand(CommandSender sender, String[] args) {
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event){
        if (event.isCancelled() || !getPlayers().contains(event.getPlayer())){return;}
        if (!status.equals("inprogress")) {return;}
        Location loc = event.getPlayer().getLocation();
        loc.setY(loc.getY()-1);
        if (!(loc.getBlock().getType() == Material.WOOL)) {return;}
        DyeColor dye = DyeColor.getByData(loc.getBlock().getData());
        if (!colorsFromConfig.contains(dye)) {return;}
        if (checkpoints.get(event.getPlayer()).size() == colorsFromConfig.size()) {
            if (dye == colorsFromConfig.get(0)) {
                endGame(event.getPlayer());
            }
        }
        if (checkpoints.get(event.getPlayer()).contains(dye)) {return;}
        checkpoints.get(event.getPlayer()).add(dye);
    }
    
    @Override
    public void postLobby(GMMap map) {
        gameMap = map;
        startGame();
    }
    
    private void startGame(){
        status = "inprogress";
        spectate().enableSpectateMode();
        reservedSlots().onlyKickSpectators(true);
        initializeColorsFromConfig();
        
        List<Location> spawnsList = getConfig().getLocationList("gamespawns", null, true);
        Location[] spawns = spawnsList.toArray(new Location[spawnsList.size()]);
        Player[] players = getPlaying().toArray(new Player[getPlaying().size()]);
        
        for (int i=0; i < spawns.length; i++) {
            if (i < players.length && i < spawns.length) {
                Location pos = spawns[i];
                players[i].teleport(pos);
            }
        }
        freezePlayers().enable();
        enablePlayerCollision(false);
        new BukkitRunnable() {
            @Override
            public void run() {
                resetDataForPlayers();
            }
        }.runTaskLater(getPlugin(), 1L);
        
        countdown().newCountdown()
                .at(15).broadcastMessage("Race starting soon!")
                .at(5).broadcastTitle("&b5","")
                .at(4).broadcastTitle("&b4","")
                .at(3).broadcastTitle("&b3","")
                .at(2).broadcastTitle("&b2","")
                .at(1).broadcastTitle("&b1","")
                .at(0).broadcastTitle("GO", "",2)
                .at(0).callMethod(this, "inProgress")
                .start();
    }
    public void inProgress(){
        HashMap<Player, Location>playerCheckpoints = new HashMap<>();
        for (Player player : getPlaying()) {
            player.getWorld().spawnEntity(player.getLocation(), EntityType.BOAT).setPassenger(player);
            playerCheckpoints.put(player,null);
        }
        freezePlayers().disable();
    }
    
    private void endGame(Player winner) {
        status = "endgame";
        
        if (winner != null) {
            broadcastTitle(winner.getName() + " WON!", "");
            randomFireworks(winner, 10);
        }
        
        freezePlayers().disable();
        enablePlayerCollision(true);
        countdown().stopAll();
        stats().stopListening();
        final Game game = this;
        new BukkitRunnable() {
            @Override
            public void run() {
                spectate().disableSpectateMode();
                GamesManager.restartGame(game);
            }
        }.runTaskLater(getPlugin(), 15 * 20L);
    }
    
    private void initializeColorsFromConfig() {
        colorsFromConfig = new ArrayList<>();
        for (String s : getConfig().getStringList("gamecheckpoints")) {
            colorsFromConfig.add(DyeColor.valueOf(s));
        }
        
        checkpoints = new HashMap<Player,List<DyeColor>>();
        for (Player p : getPlayers()) {
            checkpoints.put(p,new ArrayList<DyeColor>());
        }
    }
    
    private void enablePlayerCollision(boolean enabled){
        for (Player p : getPlayers()) {
            p.spigot().setCollidesWithEntities(enabled);
        }
    }
}
