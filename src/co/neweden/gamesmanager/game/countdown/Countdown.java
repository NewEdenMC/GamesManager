package co.neweden.gamesmanager.game.countdown;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.Util;

public class Countdown {
	
	private Game game;
	private ArrayList<Run> runAt = new ArrayList<Run>();
	private BukkitScheduler scheduler;
	private boolean started = false;
	private int countdownId = 0;
	private int cycleLength = 0;
	private int cycleCountdown = 0;
	private int startDelay = 0;
	private Integer countdown = 0;
	private int cycles = 1;
	private int taskID = 0;
	
	public Countdown(Game game, int cId, int length, int cycles, int startDelay) {
		this.game = game;
		for (int i = 0; i < (length + 1); i++) {
			runAt.add(null);
		}
		this.countdownId = cId;
		this.cycleLength = length;
		this.startDelay = startDelay;
		this.cycles = cycles;
		scheduler = Bukkit.getServer().getScheduler();
	}
	
	public Integer getCountdown() { return countdownId; }
	
	public Countdown newCountdownAt(int time, int startAt) { return newCountdownAt(time, startAt, 1, true); }
	public Countdown newCountdownAt(int time, int startAt, int cycles) { return newCountdownAt(time, startAt, cycles, true); }
	public Countdown newCountdownAt(int time, int startAt, boolean stopCurrentCountdown) { return newCountdownAt(time, startAt, 1, stopCurrentCountdown); }
	public Countdown newCountdownAt(int time, int startAt, int cycles, boolean stopCurrentCountdown) {
		if (stopCurrentCountdown == true) stop();
		CMain main = new CMain(game);
		return main.newCountdown(startAt, cycles, this.cycleLength - time);
	}
	
	public Countdown repeat(int numberOfCycles) {
		this.cycles = numberOfCycles;
		return this;
	}
	
	public Countdown broadcastMessageToServerAt(int time, String message) {
		if (time >= runAt.size()) return this;
		if (runAt.get(time) == null) runAt.set(time, new Run());
		runAt.get(time).addBroadcastToServer(message);
		return this;
	}
	
	public Countdown broadcastMessageToGameAt(int time, String message) {
		if (time >= runAt.size()) return this;
		if (runAt.get(time) == null) runAt.set(time, new Run());
		runAt.get(time).addBroadcastToGame(message);
		return this;
	}
	
	public Countdown broadcastMessageToPlayerAt(int time, String message, Player player) {
		if (time >= runAt.size()) return this;
		if (runAt.get(time) == null) runAt.set(time, new Run());
		runAt.get(time).addBroadcastToPlayer(player, message);
		return this;
	}
	
	public Countdown setBossBarForGameAt(int time, String message) { setBossBarForGameAt(time, message, 0); return this; }
	public Countdown setBossBarForGameAt(int time, String message, int duration) {
		if (time >= runAt.size()) return this;
		if (runAt.get(time) == null) runAt.set(time, new Run());
		if (duration == 0) duration = time;
		runAt.get(time).setBossBarForGame(game, message, duration);
		return this;
	}
	
	//public Countdown callMethodAt(int time, Object instanceOfClass, String method) { callMethodAt(time, instanceOfClass, method, null); return this; }
	//public Countdown callMethodAt(int time, Object instanceOfClass, String method, Object... params) {
	public Countdown callMethodAt(int time, Object instanceOfClass, String method) {
		// TODO: Add support for parameters
		if (time >= runAt.size()) return this;
		if (runAt.get(time) == null) runAt.set(time, new Run());
		runAt.get(time).addCallMethod(instanceOfClass, method);
		return this;
	}
	
	public Countdown runCommandAt(int time, String command) {
		if (time >= runAt.size()) return this;
		if (runAt.get(time) == null) runAt.set(time, new Run());
		runAt.get(time).addRunCommand(command);
		return this;
	}
	
	public Countdown runCodeAt(int time, Runnable run) {
		if (time >= runAt.size()) return this;
		if (runAt.get(time) == null) runAt.set(time, new Run());
		runAt.get(time).addRunnable(run);
		return this;
	}
	
	// Duplicate method in BarAPIInterface
	public String formatMessage(String message) {
		message = Util.formatString(message);
		message = message.replaceAll("%counter%", countdown.toString());
		message = message.replaceAll("%totalPlaying%", "" + game.getPlaying().size());
		Set<String> playingPlayerNames = new HashSet<String>();
		for (Player player : game.getPlaying()) {
			playingPlayerNames.add(player.getName());
		}
		message = message.replaceAll("%playing%", StringUtils.join(playingPlayerNames, ", "));
		return message;
	}
	
	private void runNow(int index) {
		Run run;
		try { run = runAt.get(index); } catch (NullPointerException e) { return; }
		try { run = runAt.get(index); } catch (IndexOutOfBoundsException e) { return; }
		try {
			if (run.getBroadcastToServer().isEmpty() == false) {
				for (String broadcast : run.getBroadcastToServer()) {
					Bukkit.broadcastMessage(formatMessage(broadcast));
				}
			}
		} catch (NullPointerException e) { }
		try {
			if (run.getBroadcastToGame().isEmpty() == false) {
				for (String broadcast : run.getBroadcastToGame()) {
					game.broadcast(formatMessage(broadcast));
				}
			}
		} catch (NullPointerException e) { }
		try {
			if (run.getBroadcastToPlayer().isEmpty() == false) {
				for (BroadcastToPlayer broadcast : run.getBroadcastToPlayer()) {
					broadcast.getPlayer().sendMessage(formatMessage(broadcast.getMessage()));
				}
			}
		} catch (NullPointerException e) { }
		try {
			if (run.getBossBarForGame() != null) {
				run.getBossBarForGame().startForGame();
			}
		} catch (NullPointerException e) { }
		try {
			if (run.getCallMethod().isEmpty() == false) {
				for (CallMethod call : run.getCallMethod()) {
					callMethod(call);
				}
			}
		} catch (NullPointerException e) { }
		try {
			if (run.getRunCommand().isEmpty() == false) {
				for (String command : run.getRunCommand()) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
				}
			}
		} catch (NullPointerException e) { }
		try {
			if (run.getRunnable().isEmpty() == false) {
				for (Runnable runnable : run.getRunnable()) {
					runnable.run();
				}
			}
		} catch (NullPointerException e) { }
	}
	
	private void callMethod(CallMethod call) {
		Object object = call.getInstance();
		Class<? extends Object> cls = object.getClass();
		Class<?> noparams[] = {};
		Method method;
		try {
			method = cls.getDeclaredMethod(call.getMethod(), noparams);
		} catch (SecurityException e) {
			Bukkit.getServer().getLogger().log(Level.WARNING, String.format("[%s] Unable to call method %s in class %s for game %s, a security exception occured see the stack trace below.", game.getPlugin().getDescription().getName(), call.getMethod(), call.getInstance().getClass().toString(), game.getName()));
			e.printStackTrace();
			return;
		} catch (NoSuchMethodException e) {
			Bukkit.getServer().getLogger().log(Level.WARNING, String.format("[%s] Unable to call method %s in class %s for game %s, the method either does not exist or is not visible (is the method private?) see the stack trace below.", game.getPlugin().getDescription().getName(), call.getMethod(), call.getInstance().getClass().toString(), game.getName()));
			e.printStackTrace();
			return;
		}
		try {
			method.invoke(object, (Object[])null);
		} catch (IllegalArgumentException e) {
			Bukkit.getServer().getLogger().log(Level.WARNING, String.format("[%s] Unable to call method %s in class %s for game %s, see the stack trace below.", game.getPlugin().getDescription().getName(), call.getMethod(), call.getInstance().getClass().toString(), game.getName()));
			e.printStackTrace();
			return;
		} catch (IllegalAccessException e) {
			Bukkit.getServer().getLogger().log(Level.WARNING, String.format("[%s] Unable to call method %s in class %s for game %s, see the stack trace below.", game.getPlugin().getDescription().getName(), call.getMethod(), call.getInstance().getClass().toString(), game.getName()));
			e.printStackTrace();
			return;
		} catch (InvocationTargetException e) {
			Bukkit.getServer().getLogger().log(Level.WARNING, String.format("[%s] An exception occured when calling the method %s in class %s for game %s, see the stack trace below.", game.getPlugin().getDescription().getName(), call.getMethod(), call.getInstance().getClass().toString(), game.getName()));
			e.printStackTrace();
			return;
		}
	}
	
	public Countdown start() {
		if (started == true) return this;
		started = true;
		if (startDelay > 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					cycle();
				}
			}.runTaskLater(game.getPlugin(), startDelay * 20L);
		} else {
			cycle();
		}
		return this;
	}
	
	private void cycle() {
		cycleCountdown = cycleLength;
		if (cycles >= 0) countdown = cycles * cycleLength;
		else countdown = cycleLength;
		taskID = scheduler.scheduleSyncRepeatingTask(game.getPlugin(), new Runnable() {
			@Override
			public void run() {
				if (cycles == 0) { stop(); return; }
				runNow(cycleCountdown);
				cycleCountdown--;
				countdown--;
				if (cycleCountdown == -1 && cycles == 1) { stop(); return; }
				if (cycleCountdown == 0 && cycles == -1) cycleCountdown = cycleLength;
				if (cycleCountdown == 0 && cycles >= 2) { cycles--; cycleCountdown = cycleLength; } 
			}
		}, 0L, 20L);
	}
	
	public int getTaskID() { return this.taskID; }
	
	public Countdown stop() {
		started = false;
		if (taskID > 0)
			scheduler.cancelTask(taskID);
		return this;
	}
	
}
