package co.neweden.gamesmanager.game.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.apache.commons.lang.ObjectUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import co.neweden.gamesmanager.Game;

public class MultiConfig extends TypeWrappers {

    private String dataFolderPath = "";
    private String gtFolder = "gameTypeConfigs";
    private String gtFile = "";
    private YamlConfiguration gtConfig = new YamlConfiguration();
    private String mapFolder = "mapConfigs";
    private String mapFile = "";
    private YamlConfiguration mConfig = new YamlConfiguration();
    public enum Type { MAP, GAMETYPE }

    public MultiConfig(Game game) {
        this.game = game;
        dataFolderPath = game.getPlugin().getDataFolder() + File.separator;

        mkdir(gtFolder);
        loadConfigFile(Type.GAMETYPE, gtFolder, game.getTypeName() + ".yml");

        mkdir(mapFolder);
        String mapConfigPath = gtFolder;
        String mapConfigName = game.getTypeName() + ".yml";
        if (game.worlds().getCurrentMap() != null) {
            mapConfigPath = mapFolder;
            mapConfigName = game.worlds().getCurrentMap().getBaseWorldName() + ".yml";
        }
        loadConfigFile(Type.MAP, mapConfigPath, mapConfigName);
    }

    private void mkdir(String folderName) {
        Path path = Paths.get(dataFolderPath + folderName);
        try {
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException e) {
        } catch (IOException e) {
            game.getPlugin().getLogger().severe("IOException has occurred: " + e.getMessage());
        }
    }

    public void switchMap(String mapName) {
        loadConfigFile(Type.MAP, mapFolder, mapName + ".yml");
    }

    private void loadConfigFile(Type configType, String prefixPath, String fileName) {
        String path = dataFolderPath + prefixPath;
        game.getPlugin().getLogger().info(String.format("[%s] Loading config %s", game.getName(), path + File.separator + fileName));

        try {
            switch (configType) {
                case MAP: mConfig = getIndividualConfig(configType, fileName);
                    mapFile = fileName;
                    break;
                case GAMETYPE: gtConfig = getIndividualConfig(configType, fileName);
                    gtFile = fileName;
                    break;
            }
        } catch (FileNotFoundException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Config file %s not found, skipping", game.getName(), path + File.separator + fileName));
        }
    }

    public YamlConfiguration getIndividualConfig(Type configType, String fileName) throws FileNotFoundException {
        YamlConfiguration config = new YamlConfiguration();
        String path = dataFolderPath;
        try {
            switch (configType) {
                case MAP:
                    path = path + mapFolder;
                    config.load(new File(path, fileName));
                    break;
                case GAMETYPE:
                    path = path + gtFolder;
                    config.load(new File(path, fileName));
                    break;
            }
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Cannot load configuration file %s: IOException", game.getName(), path + File.separator + fileName), ex);
        } catch (InvalidConfigurationException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Cannot load configuration file %s: InvalidConfigurationException", game.getName(), path + File.separator + fileName), ex);
        }
        return config;
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
                mConfig.save(new File(dataFolderPath + mapFolder, mapFile));
        } catch (IllegalArgumentException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Tried to save Map Config for map %s but no file object was given.", game.getName(), mapFile));
        } catch (IOException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Could not save config %s to %s", game.getName(), mapFile, mapFolder), ex);
        }

        try {
            if (configType == null || configType.equals(Type.GAMETYPE))
                gtConfig.save(new File(dataFolderPath + gtFolder, gtFile));
        } catch (IllegalArgumentException ex) {
            game.getPlugin().getLogger().warning(String.format("[%s] Tried to save Map Config for map %s but no file object was given.", game.getName(), gtFile));
        } catch (IOException ex) {
            game.getPlugin().getLogger().log(Level.SEVERE, String.format("[%s] Could not save config %s to %s", game.getName(), gtFile, gtFolder), ex);
        }
    }

}
