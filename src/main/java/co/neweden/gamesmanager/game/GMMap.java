package co.neweden.gamesmanager.game;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.game.config.MultiConfig;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.FileNotFoundException;

public class GMMap implements Listener {

    Game game;
    private World world;
    private String baseWorldName;
    private YamlConfiguration config;

    public GMMap(Game game, World world, String baseWorldName) {
        this.game = game;
        this.world = world;
        this.baseWorldName = baseWorldName;
        Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
        try {
            config = game.getConfig().getIndividualConfig(MultiConfig.Type.MAP, baseWorldName + ".yml");
        } catch (FileNotFoundException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Unable to fine map config for map %s, this could be a problem.", game.getName(), baseWorldName));
        }
    }

    public World getWorld() {
        return world;
    }

    public String getBaseWorldName() {
        return baseWorldName;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        if (!event.getWorld().equals(world)) return;
        weatherTask.cancel();
    }

    private WeatherType forceWeatherType = null;
    private BukkitTask weatherTask = null;
    public void forceWeather(WeatherType type) {
        forceWeatherType = type;
        if (weatherTask == null) return;
        weatherTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (forceWeatherType == null) this.cancel();
                if (forceWeatherType.equals(WeatherType.DOWNFALL))
                    world.setStorm(true);
                else
                    world.setStorm(false);
            }
        }.runTaskTimer(game.getPlugin(), 0L, 20L);
    }

}
