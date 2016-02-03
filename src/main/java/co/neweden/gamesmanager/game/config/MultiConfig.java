package co.neweden.gamesmanager.game.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.Util;

public class MultiConfig extends TypeWrappers {

    private Game game;
    private YamlConfiguration gtConfig = new YamlConfiguration();
    private File gtConfigFile;
    private YamlConfiguration mConfig = new YamlConfiguration();
    private File mConfigFile;
    public enum Config { MAP, GAMETYPE }

    public MultiConfig(Game game) {
        this.game = game;
        loadConfigFile(Config.GAMETYPE, game.getName() + ".yml");
        loadConfigFile(Config.MAP, game.getName() + ".yml");
    }

    protected void switchMap(String mapName) {
        loadConfigFile(Config.MAP, mapName + ".yml");
    }

    public void loadConfigFile(Config config, String fileName) {
        game.getPlugin().getLogger().info(String.format("[%s] Loading config %s", game.getName(), fileName));
        try {
            switch (config) {
                case MAP: mConfigFile = new File(game.getPlugin().getDataFolder(), fileName);
                    mConfig.load(mConfigFile);
                    break;
                case GAMETYPE: gtConfigFile = new File(game.getPlugin().getDataFolder(), fileName);
                    gtConfig.load(gtConfigFile);
                    break;
            }
        } catch (FileNotFoundException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Config file %s not found, skipping", game.getName(), fileName));
        } catch (IOException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Cannot load configuration file %s: IOException", game.getName(), fileName), ex);
        } catch (InvalidConfigurationException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Cannot load configuration file %s: InvalidConfigurationException", game.getName(), fileName), ex);
        }
    }

    @Override
    public Object get(String path, Object def) {
        // Will return the object from the Map Config at "path",
        // if null, will return the object from the GameType Config at "path",
        // else will return null.
        return mConfig.get(path, gtConfig.get(path, def));
    }

    public void set(String path, Object value, Config config) {
        if (config == Config.MAP) {
            mConfig.set(path, value);
        } else if (config == Config.GAMETYPE) {
            gtConfig.set(path, value);
        }
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

    public void saveConfig() { saveConfig(null); }
    public void saveConfig(Config config) {
        try {
            if (config == null || config.equals(Config.MAP))
                mConfig.save(mConfigFile);
        } catch (IllegalArgumentException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Tried to save Map Config for map %s but no file object was given.", game.getName(), game.getCurrentMapName()));
        } catch (IOException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Could not save config %s to %s", game.getName(), mConfigFile.getName(), mConfigFile.getPath()), ex);
        }

        try {
            if (config == null || config.equals(Config.GAMETYPE))
                gtConfig.save(gtConfigFile);
        } catch (IllegalArgumentException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Tried to save Map Config for map %s but no file object was given.", game.getName(), game.getType()));
        } catch (IOException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Could not save config %s to %s", game.getName(), mConfigFile.getName(), mConfigFile.getPath()), ex);
        }
    }

}
