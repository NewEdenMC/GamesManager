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
    private List<Map.Entry<Countdown.Scope, String>> broadcast = new ArrayList<>();
    private List<Map.Entry<Countdown.Scope, List<String>>> broadcastTitle = new ArrayList();
    private List<Map.Entry<Countdown.Scope, RunBossBar>> bar = new ArrayList<>();
    private List<Map.Entry<Object, String>> callMethod = new ArrayList<>();

    public Run(Countdown parent, Integer time) {
        this.parent = parent;
        this.time = time;
    }

    public Countdown broadcastMessage(String message) { return broadcastMessage(Countdown.Scope.GAME, message); }
    public Countdown broadcastMessage(Countdown.Scope scope, String message) {
        broadcast.add(new AbstractMap.SimpleEntry<>(scope, message));
        return parent;
    }

    public Countdown broadcastTitle(String title, String subTitle) { return broadcastTitle(Countdown.Scope.GAME, title, subTitle, 5); }
    public Countdown broadcastTitle(Countdown.Scope scope, String title, String subTitle, Integer duration) {
        List<String> list = new ArrayList<>();
        list.add(String.format("times 10 %s 10", Integer.toString(duration * 20)));
        list.add(String.format("title {\"text\":\"%s\"}", ChatColor.translateAlternateColorCodes('&', title)));
        list.add(String.format("subtitle {\"text\":\"%s\"}", ChatColor.translateAlternateColorCodes('&', subTitle)));

        broadcastTitle.add(new AbstractMap.SimpleEntry<>(scope, list));
        return parent;
    }

    public Countdown displayBossBar(BossBar bossBar) { return displayBossBar(Countdown.Scope.GAME, bossBar); }
    public Countdown displayBossBar(BossBar bossBar, Integer time) { return displayBossBar(Countdown.Scope.GAME, bossBar, time); }
    public Countdown displayBossBar(Countdown.Scope scope, BossBar bossBar) { return displayBossBar(scope, bossBar, null); }
    public Countdown displayBossBar(Countdown.Scope scope, BossBar bossBar, Integer time) {
        if (time == null) time = this.time;
        bar.add(new AbstractMap.SimpleEntry<>(scope, new RunBossBar(parent, scope, bossBar, time)));
        return parent;
    }

    public Countdown callMethod(Object instanceOfClass, String method) {
        callMethod.add(new AbstractMap.SimpleEntry<>(instanceOfClass, method));
        return parent;
    }

    protected void run() {
        // broadcast messages
        for (Map.Entry<Countdown.Scope, String> entry : broadcast) {
            String message = formatMessage(entry.getValue());
            switch (entry.getKey()) {
                case SERVER: parent.game.getPlugin().getServer().broadcastMessage(Util.formatString(message));
                    break;
                case GAME: parent.game.broadcast(message);
            }
        }

        // title
        for (Map.Entry<Countdown.Scope, List<String>> entry : broadcastTitle) {
            for (String command : entry.getValue()) {
                String message = formatMessage(command);
                switch (entry.getKey()) {
                    case SERVER:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a " + message);
                        break;
                    case GAME:
                        for (Player player : parent.game.getPlayers()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + player.getName() + " " + message);
                        }
                }
            }
        }

        // bossBar
        for (Map.Entry<Countdown.Scope, RunBossBar> entry : bar) {
            entry.getValue().start();
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
