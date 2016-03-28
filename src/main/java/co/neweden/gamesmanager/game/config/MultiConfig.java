package co.neweden.gamesmanager.game.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private YamlConfiguration gtConfig = new YamlConfiguration();
    private File gtConfigFile;
    private YamlConfiguration mConfig = new YamlConfiguration();
    private File mConfigFile;
    public enum Type { MAP, GAMETYPE }

    public MultiConfig(Game game) {
        this.game = game;

        mkdir("gameTypeConfigs");
        loadConfigFile(Type.GAMETYPE, "gameTypeConfigs", game.getTypeName() + ".yml");

        mkdir("mapConfigs");
        String mapConfigPath = "gameTypeConfigs";
        String mapConfigName = game.getTypeName() + ".yml";
        if (game.worlds().getCurrentMap() != null) {
            mapConfigPath = "mapConfigs";
            mapConfigName = game.worlds().getCurrentMap().getBaseWorldName() + ".yml";
        }
        loadConfigFile(Type.MAP, mapConfigPath, mapConfigName);
    }

    private void mkdir(String folderName) {
        Path path = Paths.get(game.getPlugin().getDataFolder().getPath() + File.separator + folderName);
        try {
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException e) {
        } catch (IOException e) {
            game.getPlugin().getLogger().severe("IOException has occurred: " + e.getMessage());
        }
    }

    public void switchMap(String mapName) {
        loadConfigFile(Type.MAP, "mapConfigs", mapName + ".yml");
    }

    public void loadConfigFile(Type configType, String prefixPath, String fileName) {
        String path = game.getPlugin().getDataFolder() + File.separator + prefixPath;
        game.getPlugin().getLogger().info(String.format("[%s] Loading config %s", game.getName(), path + File.separator + fileName));

        try {
            switch (configType) {
                case MAP: mConfigFile = new File(path, fileName);
                    mConfig.load(mConfigFile);
                    break;
                case GAMETYPE: gtConfigFile = new File(path, fileName);
                    gtConfig.load(gtConfigFile);
                    break;
            }
        } catch (FileNotFoundException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Config file %s not found, skipping", game.getName(), path + File.separator + fileName));
        } catch (IOException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Cannot load configuration file %s: IOException", game.getName(), path + File.separator + fileName), ex);
        } catch (InvalidConfigurationException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Cannot load configuration file %s: InvalidConfigurationException", game.getName(), path + File.separator + fileName), ex);
        }
    }

    @Override
    public Object get(String path, Object def) {
        // Will return the object from the Map Config at "path",
        // if null, will return the object from the GameType Config at "path",
        // else will return null.
        return mConfig.get(path, gtConfig.get(path, def));
    }

    public void set(String path, Object value, Type configType) {
        if (configType == Type.MAP) {
            mConfig.set(path, value);
        } else if (configType == Type.GAMETYPE) {
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
    public void saveConfig(Type configType) {
        try {
            if (configType == null || configType.equals(Type.MAP))
                mConfig.save(mConfigFile);
        } catch (IllegalArgumentException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Tried to save Map Config for map %s but no file object was given.", game.getName(), game.getCurrentMapName()));
        } catch (IOException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Could not save config %s to %s", game.getName(), mConfigFile.getName(), mConfigFile.getPath()), ex);
        }

        try {
            if (configType == null || configType.equals(Type.GAMETYPE))
                gtConfig.save(gtConfigFile);
        } catch (IllegalArgumentException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Tried to save Map Config for map %s but no file object was given.", game.getName(), game.getTypeName()));
        } catch (IOException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Could not save config %s to %s", game.getName(), mConfigFile.getName(), mConfigFile.getPath()), ex);
        }
    }

}
