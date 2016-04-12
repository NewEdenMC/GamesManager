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
    private BukkitTask worldUpdater = null;
    private WeatherType forceWeatherType = null;
    private Long forceTime;

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
        worldUpdater();
    }

    private void worldUpdater() {
        if (config.isString("forceWeather")) {
            String weather = config.getString("forceWeather");
            if (weather.equalsIgnoreCase("CLEAR")) forceWeather(WeatherType.CLEAR);
            else if (weather.equalsIgnoreCase("DOWNFALL")) forceWeather(WeatherType.DOWNFALL);
            else game.getPlugin().getLogger().warning(String.format("[%s] forceWeather in %s config, value %s is not valid, acceptable values are CLEAR or DOWNFALL", game.getName(), baseWorldName, weather));
        }

        forceTime = config.getLong("forceTime", -1);

        worldUpdater = new BukkitRunnable() {
            @Override
            public void run() {
                if (forceWeatherType != null) {
                    if (forceWeatherType.equals(WeatherType.DOWNFALL))
                        world.setStorm(true);
                    else
                        world.setStorm(false);
                }
                if (forceTime != -1) world.setTime(forceTime);
            }
        }.runTaskTimer(game.getPlugin(), 0L, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        if (event.getWorld().equals(world))
            worldUpdater.cancel();
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

    public void forceWeather(WeatherType type) { forceWeatherType = type; }

    public void foraceTime(Long time) { forceTime = time; }

}
