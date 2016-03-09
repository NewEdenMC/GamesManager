package co.neweden.gamesmanager.game.countdown;

import co.neweden.gamesmanager.Game;

import java.util.ArrayList;
import java.util.List;

public class CMain {
    private List<Countdown> countdowns = new ArrayList<>();
    Game game;

    public CMain(Game game) {
        this.game = game;
    }

    public Countdown newCountdown() { return newCountdown(null); }
    public Countdown newCountdown(Integer startAt) { return newCountdown(startAt, null, null); }
    public Countdown newCountdown(Integer startAt, Integer startDelay) { return newCountdown(startAt, startDelay, null); }
    public Countdown newCountdown(Integer startAt, Integer startDelay, Integer repeat) {
        int cId = countdowns.size(); // the new index
        Countdown countdown = new Countdown(game, cId, startAt, startDelay, repeat);
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
