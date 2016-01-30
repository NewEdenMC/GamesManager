package co.neweden.gamesmanager.game.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.Util;

public class MultiConfig extends TypeWrappers {

    private Game game;
    private YamlConfiguration gtConfig = new YamlConfiguration();
    private YamlConfiguration mConfig = new YamlConfiguration();

    public MultiConfig(Game game) {
        this.game = game;
        loadConfigFile(gtConfig, game.getName() + ".yml");
    }

    protected void switchMap(String mapName) {
        loadConfigFile(mConfig, mapName + ".yml");
    }

    public void loadConfigFile(YamlConfiguration config, String fileName) {
        game.getPlugin().getLogger().info(String.format("[%s] Loading config %s", game.getName(), fileName));
        try {
            config.load(new File(game.getPlugin().getDataFolder(), fileName));
        } catch (FileNotFoundException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Config file %s not found, skipping", game.getName(), fileName));
        } catch (IOException ex) {
            game.getPlugin().getLogger().severe(String.format("[%s] Cannot load configuration file %s: IOException", game.getName(), fileName));
            ex.printStackTrace();
        } catch (InvalidConfigurationException ex) {
            game.getPlugin().getLogger().severe(String.format("[%s] Cannot load configuration file %s: InvalidConfigurationException", game.getName(), fileName));
            ex.printStackTrace();
        }
    }

    @Override
    public Object get(String path, Object def) {
        // Will return the object from the Map Config at "path",
        // if null, will return the object from the GameType Config at "path",
        // else will return null.
        return mConfig.get(path, gtConfig.get(path, def));
    }

    public Boolean isSet(String path) {
        if (mConfig.isSet(path))
            return true;
        else {
            if (gtConfig.isSet(path))
                return true;
        }
        return false;
    }

}
