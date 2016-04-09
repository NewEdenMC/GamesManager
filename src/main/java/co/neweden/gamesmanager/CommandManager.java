package co.neweden.gamesmanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;

public class CommandManager implements CommandExecutor {

    private GMMain plugin;

    public CommandManager(GMMain plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        ArrayList<String> passArgs = new ArrayList<>(Arrays.asList(args));
        switch (command.getName().toLowerCase()) {
            case "gamesmanager" :
                return gamesManagerCommand(sender, command, passArgs);
            case "join" :
                return joinCommand(sender, passArgs);
        }

        return false;
    }

    private boolean gamesManagerCommand(CommandSender sender, Command command, ArrayList<String> args) {
        if (args.size() == 0) {
            if (!sender.hasPermission("gamesmanager.help")) {
                sender.sendMessage(Util.formatString("&cYou do not have permission to run this command."));
                return true;
            }
            // Return version info if no sub-command
            if (sender.hasPermission("gamesmanager.version"))
                sender.sendMessage(String.format(Util.formatString("&b%s &aversion %s&b"), plugin.getName(), plugin.getDescription().getVersion()));
            else
                sender.sendMessage(String.format(Util.formatString("&b%s"), plugin.getName()));

            // Command help
            sender.sendMessage(String.format(Util.formatString("&bRun &e%s&b followed by the name of a game for commands specific to that game."), command.getLabel()));
            return true;
        }

        String subCommand = args.get(0);
        args.remove(0);

        if (subCommand.equalsIgnoreCase("add"))
            return addCommand(sender, args);

        if (subCommand.equalsIgnoreCase("start"))
            return startCommand(sender, args);

        if (subCommand.equalsIgnoreCase("restart"))
            return reStartCommand(sender, args);

        if (subCommand.equalsIgnoreCase("stop"))
            return stopCommand(sender, args);

        if (subCommand.equalsIgnoreCase("join"))
            return joinCommand(sender, args);

        if (subCommand.equalsIgnoreCase("list"))
            return listCommand(sender);

        if (subCommand.equalsIgnoreCase("vote"))
            return voteCommand(sender, args);

        Game game = GamesManager.getGameByName(args.get(0));
        if (game == null) {
            sender.sendMessage(String.format(Util.formatString("&cThe game %s either does not exist or is disabled."), args.get(0)));
            return true;
        }

        if (!sender.hasPermission("gamesmanager.game." + game.getName())) {
            if (!sender.hasPermission("gamesmanager.game.*")) {
                sender.sendMessage(String.format(Util.formatString("&cYou do not have permission to run commands for game: %s"), game.getName()));
                return true;
            }
        }

        // TODO: doesn't work with new structure
        //String[] gArgs = Arrays.copyOfRange(args, 1, args.length);
        //game.getGame().onCommand(sender, gArgs);

        return true;
    }

    private boolean addCommand(CommandSender sender, ArrayList<String> args) {
        if (!sender.hasPermission("gamesmanager.add")) {
            sender.sendMessage(Util.formatString("&cYou do not have permission to add games."));
            return true;
        }
        if (args.size() < 3) {
            sender.sendMessage(Util.formatString("&eadd <game-name> <game-type> <enabled>&b: you did not provide enough arguments."));
            return true;
        }

        plugin.reloadConfig();

        String name = args.get(0);
        if (plugin.getConfig().isSet("games." + name)) {
            sender.sendMessage(Util.formatString("&cThe game " + name + " already exists"));
            return true;
        }
        String type = args.get(1);
        Boolean enabled;
        if (args.get(2).equalsIgnoreCase("true") || args.get(2).equalsIgnoreCase("false")) {
            enabled = Boolean.valueOf(args.get(2));
        } else {
            sender.sendMessage(Util.formatString("&cThe value " + args.get(2) + " must be a boolean value (either true or false)."));
            return true;
        }

        plugin.getConfig().set("games." + name + ".type", type);
        plugin.getConfig().set("games." + name + ".enabled", enabled);
        plugin.saveConfig();

        sender.sendMessage(Util.formatString("&aNew game has been added, to start it run /gamesmanager start " + name));

        return true;
    }

    private boolean startCommand(CommandSender sender, ArrayList<String> args) {
        if (!sender.hasPermission("gamesmanager.start")) {
            sender.sendMessage(Util.formatString("&cYou do not have permission to start games."));
            return true;
        }
        if (args.size() == 0) {
            sender.sendMessage(Util.formatString("&estart <game-name>&b: you did not provide the name of the game you want to start."));
            return true;
        }
        String name = args.get(0);
        if (GamesManager.getGameByName(name) != null) {
            sender.sendMessage(Util.formatString("&cThe game you are trying to start is already running, try stopping it first."));
            return true;
        }
        Game game = GamesManager.startGame(name);
        if (game == null)
            sender.sendMessage(Util.formatString("&cUnable to start game " + name + " check console for any errors"));
        else
            sender.sendMessage(Util.formatString("&aGame has been started successfully"));
        return true;
    }

    private boolean reStartCommand(CommandSender sender, ArrayList<String> args) {
        if (!sender.hasPermission("gamesmanager.start") || !sender.hasPermission("gamesmanager.stop")) {
            sender.sendMessage(Util.formatString("&cYou do not have permission to restart games, you need both permission to start and stop games."));
            return true;
        }
        if (args.size() == 0) {
            sender.sendMessage(Util.formatString("&erestart <game-name>&b: you did not provide the name of the game you want to restart."));
            return true;
        }
        stopCommand(sender, args);
        startCommand(sender, args);
        return true;
    }

    private boolean stopCommand(CommandSender sender, ArrayList<String> args) {
        if (!sender.hasPermission("gamesmanager.stop")) {
            sender.sendMessage(Util.formatString("&cYou do not have permission to stop games."));
            return true;
        }
        if (args.size() == 0) {
            sender.sendMessage(Util.formatString("&estop <game-name>&b: you did not provide the name of the game you want to stop."));
            return true;
        }
        Game game = GamesManager.getGameByName(args.get(0));
        if (game == null) {
            sender.sendMessage(Util.formatString("&cThe game you are trying to stop is not running, try starting it first."));
            return true;
        }
        Boolean stopped = GamesManager.stopGame(game);
        if (!stopped)
            sender.sendMessage(Util.formatString("&cUnable to stop game " + args.get(0) + " check console for any errors"));
        else
            sender.sendMessage(Util.formatString("&aGame has been stopped successfully"));
        return true;
    }

    private boolean joinCommand(CommandSender sender, ArrayList<String> args) {
        if (!sender.hasPermission("gamesmanager.join")) {
            sender.sendMessage(Util.formatString("&cYou do not have permission to join a game."));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Util.formatString("&cOnly players can run this command."));
        }
        if (args.size() == 0) {
            sender.sendMessage(Util.formatString("&ejoin <game-name>&b: you did not provide the name of the game you want to join."));
            return true;
        }

        Game game = GamesManager.getGameByName(args.get(0));
        if (game == null) {
            sender.sendMessage(String.format(Util.formatString("&cThe game %s either does not exist or is disabled."), args.get(0)));
            return true;
        }

        GamesManager.joinPlayerToGame((Player) sender, game);

        return true;
    }

    private boolean listCommand(CommandSender sender) {
        if (!sender.hasPermission("gamesmanager.list")) {
            sender.sendMessage(Util.formatString("&cYou do not have permission to list games."));
            return true;
        }
        sender.sendMessage(Util.formatString("&aAvailable games in GamesManager"));
        for (String key : plugin.getConfig().getKeys(true)) {
            if (key.length() <= 6 ||
                !key.substring(0, 6).equalsIgnoreCase("games.")) continue;
            String game = key.substring(6);
            if (game.contains(".")) continue;
            String running;
            if (GamesManager.getGameByName(game) != null)
                running = "&aRunning";
            else
                running = "&cNot running";
            sender.sendMessage(Util.formatString("&f- " + game + " (" + running + "&f)"));
        }
        return true;
    }

    private boolean voteCommand(CommandSender sender, ArrayList<String> args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Util.formatString("&cOnly players can run this command."));
        }
        if (args.size() == 0) {
            sender.sendMessage(Util.formatString("&evote <map-name>&b: you did not provide the name of the map you want to vote for."));
            return true;
        }

        Player player = (Player) sender;
        Game game = GamesManager.getGameByWorld(player.getWorld());
        if (game == null) {
            sender.sendMessage(Util.formatString("&cYou are not currently in a game."));
            return true;
        }

        game.lobby().voteCommand(player, args.get(0));

        return true;
    }

}
