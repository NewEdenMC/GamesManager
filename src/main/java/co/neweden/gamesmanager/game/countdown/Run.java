package co.neweden.gamesmanager.game.countdown;

import co.neweden.gamesmanager.Util;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sun.applet.resources.MsgAppletViewer_zh_CN;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public class Run {

    private Countdown parent;
    public enum MsgScope { SERVER, GAME }
    private List<Map.Entry<MsgScope, String>> broadcast = new ArrayList<>();
    private List<Map.Entry<Object, String>> callMethod = new ArrayList<>();

    public Run(Countdown parent) {
        this.parent = parent;
    }

    public Countdown broadcastMessage(String message) { return broadcastMessage(MsgScope.GAME, message); }
    public Countdown broadcastMessage(MsgScope scope, String message) {
        broadcast.add(new AbstractMap.SimpleEntry<>(scope, message));
        return parent;
    }

    public Countdown callMethod(Object instanceOfClass, String method) {
        callMethod.add(new AbstractMap.SimpleEntry<>(instanceOfClass, method));
        return parent;
    }

    protected void run() {
        // broadcast messages
        for (Map.Entry<MsgScope, String> entry : broadcast) {
            String message = formatMessage(entry.getValue());
            switch (entry.getKey()) {
                case SERVER: parent.game.getPlugin().getServer().broadcastMessage(Util.formatString(message));
                    break;
                case GAME: parent.game.broadcast(message);
            }
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
        message = message.replaceAll("%counter%", new Integer(parent.counter * parent.repeat).toString());
        message = message.replaceAll("%totalPlaying%", "" + parent.game.getPlaying().size());
        Set<String> playingPlayerNames = new HashSet<String>();
        for (Player player : parent.game.getPlaying()) {
            playingPlayerNames.add(player.getName());
        }
        message = message.replaceAll("%playing%", StringUtils.join(playingPlayerNames, ", "));
        return message;
    }

}
