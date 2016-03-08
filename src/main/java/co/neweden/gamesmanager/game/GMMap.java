package co.neweden.gamesmanager.game;

import org.bukkit.World;
import org.bukkit.event.Listener;

public class GMMap implements Listener {

    private World world;
    private String baseWorldName;

    public GMMap(World world, String baseWorldName) {
        this.world = world;
        this.baseWorldName = baseWorldName;
    }

    public World getWorld() {
        return world;
    }

    public String getBaseWorldName() {
        return baseWorldName;
    }

}
