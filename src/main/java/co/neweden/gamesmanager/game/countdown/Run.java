package co.neweden.gamesmanager.game.countdown;

import co.neweden.gamesmanager.Util;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sun.applet.resources.MsgAppletViewer_zh_CN;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public class Run {

    private Countdown parent;
    private Integer time;
    private List<String> broadcast = new ArrayList<>();
    private List<List<Object>> broadcastTitle = new ArrayList();
    private List<RunBossBar> bar = new ArrayList<>();
    private List<Map.Entry<Object, String>> callMethod = new ArrayList<>();

    public Run(Countdown parent, Integer time) {
        this.parent = parent;
        this.time = time;
    }

    public Countdown broadcastMessage(String message) {
        broadcast.add(message);
        return parent;
    }

    public Countdown broadcastTitle(String title, String subTitle) { return broadcastTitle(title, subTitle, 5); }
    public Countdown broadcastTitle(String title, String subTitle, Integer duration) { return broadcastTitle(title, subTitle, 10L, duration * 20L, 10L); }
    public Countdown broadcastTitle(String title, String subTitle, Long fadeIn, Long stay, Long fadeOut) {
        List<Object> list = new ArrayList<>();
        list.add(title); list.add(subTitle); list.add(fadeIn); list.add(stay); list.add(fadeOut);
        broadcastTitle.add(list);
        return parent;
    }

    public Countdown displayBossBar(BossBar bossBar) { return displayBossBar(bossBar, null); }
    public Countdown displayBossBar(BossBar bossBar, Integer time) {
        if (time == null) time = this.time;
        bar.add(new RunBossBar(parent, bossBar, time));
        return parent;
    }

    public Countdown callMethod(Object instanceOfClass, String method) {
        callMethod.add(new AbstractMap.SimpleEntry<>(instanceOfClass, method));
        return parent;
    }

    protected void run() {
        // broadcast messages
        for (String message : broadcast) {
            parent.game.broadcast(formatMessage(message));
        }

        // title
        for (List<Object> entry : broadcastTitle) {
            parent.game.broadcastTitle(
                    formatMessage(entry.get(0).toString()), entry.get(1).toString(), (long) entry.get(2), (long) entry.get(3), (long) entry.get(4)
            );
        }

        // bossBar
        for (RunBossBar runBossBar : bar) {
            runBossBar.start();
        }

        // call method
        for (Map.Entry<Object, String> entry : callMethod) {
            runMethod(entry.getKey(), entry.getValue());
        }
    }

    private void runMethod(Object object, String methodName) {
        Class<? extends Object> cls = object.getClass();
        Class<?> noparams[] = {};
        Method method;
        try {
            method = cls.getDeclaredMethod(methodName, noparams);
        } catch (SecurityException e) {
            parent.game.getPlugin().getLogger().severe(String.format("[%s] Unable to call method %s in class %s: a security exception occurred.", parent.game.getName(), methodName, object.toString()));
            throw e;
        } catch (NoSuchMethodException e) {
            parent.game.getPlugin().getLogger().severe(String.format("[%s] Unable to call method %s in class %s: the method either does not exist or is not visible (is the method private?).", parent.game.getName(), methodName, object.toString()));
            throw new NullPointerException();
        }
        try {
            method.invoke(object, (Object[])null);
        } catch (IllegalArgumentException e) {
            parent.game.getPlugin().getLogger().severe(String.format("[%s] Unable to call method %s in class %s: illegal arguments exception.", parent.game.getName(), methodName, object.toString()));
            throw e;
        } catch (IllegalAccessException e) {
            parent.game.getPlugin().getLogger().severe(String.format("[%s] Unable to call method %s in class %s: illegal access exception.", parent.game.getName(), methodName, object.toString()));
            throw new IllegalArgumentException();
        } catch (InvocationTargetException e) {
            parent.game.getPlugin().getLogger().severe(String.format("[%s] Unable to call method %s in class %s: invocation target exception.", parent.game.getName(), methodName, object.toString()));
            throw new IllegalArgumentException();
        }

    }

    public String formatMessage(String message) {
        message = Util.formatString(message);
        message = message.replaceAll("%counter%", Integer.toString(parent.counter * parent.repeat));
        message = message.replaceAll("%totalPlaying%", "" + parent.game.getPlaying().size());
        Set<String> playingPlayerNames = new HashSet<>();
        for (Player player : parent.game.getPlaying()) {
            playingPlayerNames.add(player.getName());
        }
        message = message.replaceAll("%playing%", StringUtils.join(playingPlayerNames, ", "));
        return message;
    }

}
