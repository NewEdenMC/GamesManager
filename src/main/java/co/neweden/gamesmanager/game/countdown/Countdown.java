package co.neweden.gamesmanager.game.countdown;

import co.neweden.gamesmanager.Game;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Countdown {

    protected Game game;
    private Integer cID;
    private Integer startAt;
    private Integer startDelay;
    protected Integer repeat;
    private Map<Integer, Run> runAt = new TreeMap<>();
    private Integer last = 0;
    private BukkitTask scheduler;
    protected Integer counter = 0;

    public Countdown(Game game, Integer cID, Integer startAt, Integer startDelay, Integer repeat) {
        this.game = game;
        this.cID = cID;
        this.startAt = startAt;

        if (startDelay == null)
            this.startDelay = 0;
        else
            this.startDelay = startDelay;

        if (repeat == null)
            this.repeat = 1;
        else
            this.repeat = repeat;
    }

    public Integer getCID() { return cID; }

    public Run at(int seconds) {
        if (runAt.get(seconds) != null) {
            return runAt.get(seconds);
        } else {
            Run run = new Run(this);
            runAt.put(seconds, run);
            if (seconds > last) last = seconds;
            return run;
        }
    }

    public void start() {
        if (repeat == 0) return;

        // Start counting down from the greatest amount of seconds
        counter = last;

        // if we have been told to start at a specific number of seconds, then start there
        if (startAt != null)
            counter = startAt;

        scheduler = new BukkitRunnable() {
            @Override
            public void run() {
                // Count down in seconds, calling any Run objects as we reach them
                if (runAt.get(counter) != null) {
                    runAt.get(counter).run();
                }
                if (counter > 0) {
                    counter--;
                } else {
                    if (repeat > 0) repeat--;
                    if (repeat != 0) {
                        startDelay = 0;
                        start();
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(game.getPlugin(), startDelay * 20L, 20L);
    }

    public void stop() {
        scheduler.cancel();
    }

}
