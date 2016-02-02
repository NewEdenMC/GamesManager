package co.neweden.gamesmanager.game.countdown;

import java.util.ArrayList;

import co.neweden.gamesmanager.Game;

public class CMain {
	
	private ArrayList<Countdown> countdowns = new ArrayList<Countdown>();
	Game game;
	
	public CMain(Game game) {
		this.game = game;
	}
	
	public Countdown newCountdown(int length) { return newCountdown(length, 1, 0); }
	public Countdown newCountdown(int length, int cycles) { return newCountdown(length, cycles, 0); }
	public Countdown newCountdown(int length, int cycles, int startDelay) { 
		int cId = countdowns.size(); // the new index
		Countdown countdown = new Countdown(game, cId, length, cycles, startDelay);
		countdowns.add(countdown);
		return countdown;
	}
	
	public Countdown getCountdownById(int cId) {
		if (cId >= countdowns.size())
			return null;
		else
			return countdowns.get(cId);
	}
	
	public void removeCountdownById(int cId) {
		if (cId >= countdowns.size()) return;
		Countdown countdown = countdowns.get(cId);
		countdown.stop();
		countdowns.remove(cId);
		return;
	}
	
	public void startAll() {
		for (Countdown c : countdowns) {
			c.start();
		}
	}
	
	public void stopAll() {
		for (Countdown c : countdowns) {
			c.stop();
		}
	}
	
	public void removeAll() {
		for (Countdown c : countdowns) {
			c.stop();
		}
		countdowns.clear();
	}
	
}
