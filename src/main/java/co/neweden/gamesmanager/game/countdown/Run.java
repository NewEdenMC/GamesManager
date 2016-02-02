package co.neweden.gamesmanager.game.countdown;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import co.neweden.gamesmanager.Game;

public class Run {
	
	private ArrayList<String> broadcastToServer = new ArrayList<String>();
	private ArrayList<String> broadcastToGame = new ArrayList<String>();
	private ArrayList<BroadcastToPlayer> broadcastToPlayer = new ArrayList<BroadcastToPlayer>();
	private BarAPIInterface barAPIInterface = null;
	private ArrayList<CallMethod> callMethod = new ArrayList<CallMethod>();
	private ArrayList<String> runCommand = new ArrayList<String>();
	private ArrayList<Runnable> runnable = new ArrayList<Runnable>();
	
	public void addBroadcastToServer(String message) { broadcastToServer.add(message); }
	public ArrayList<String> getBroadcastToServer() { return broadcastToServer; }
	
	public void addBroadcastToGame(String message) { broadcastToGame.add(message); }
	public ArrayList<String> getBroadcastToGame() { return broadcastToGame; }
	
	public void addBroadcastToPlayer(Player player, String message) { broadcastToPlayer.add(new BroadcastToPlayer(player, message)); }
	public ArrayList<BroadcastToPlayer> getBroadcastToPlayer() { return broadcastToPlayer; }
	
	public void setBossBarForGame(Game game, String message, int duration) {
		barAPIInterface = new BarAPIInterface(game, message, duration);
	}
	public BarAPIInterface getBossBarForGame() { return barAPIInterface; }
	
	public void addCallMethod(Object instanceOfClass, String method) { callMethod.add(new CallMethod(instanceOfClass, method)); }
	public ArrayList<CallMethod> getCallMethod() { return callMethod; }
	
	public void addRunCommand(String command) { runCommand.add(command); }
	public ArrayList<String> getRunCommand() { return runCommand; }
	
	public void addRunnable(Runnable runnable) { this.runnable.add(runnable); }
	public ArrayList<Runnable> getRunnable() { return this.runnable; }
	
}
