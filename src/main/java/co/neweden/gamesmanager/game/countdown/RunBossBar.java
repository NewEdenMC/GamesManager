package co.neweden.gamesmanager.game.countdown;

import co.neweden.gamesmanager.event.GMPlayerJoinGameEvent;
import co.neweden.gamesmanager.event.GMPlayerLeaveGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class RunBossBar implements Listener {

    private Countdown main;
    protected BossBar bossBar;
    protected Integer time;
    private Integer countdown;

    protected RunBossBar(Countdown countdown, BossBar bar, Integer time) {
        main = countdown;
        bossBar = bar; this.time = time;
        Bukkit.getPluginManager().registerEvents(this, countdown.game.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinGame(GMPlayerJoinGameEvent event) {
        if (main.game.getPlayers().contains(event.getPlayer()))
            bossBar.addPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeaveGame(GMPlayerLeaveGameEvent event) {
        if (main.game.getPlayers().contains(event.getPlayer()))
            bossBar.removePlayer(event.getPlayer());
    }

    protected void start() {
        Set<Player> initPlayers = new HashSet<>();
        initPlayers.addAll(main.game.getPlayers());
        for (Player player : initPlayers) {
            bossBar.addPlayer(player);
        }
        bossBar.setVisible(true);
        countdown = time;
        new BukkitRunnable() {
            @Override
            public void run() {
                bossBar.setProgress((double) countdown / (double) time);
                if (countdown == 0) {
                    bossBar.removeAll();
                    this.cancel();
                } else
                    countdown--;
            }
        }.runTaskTimer(main.game.getPlugin(), 0L, 20L);
    }

}
