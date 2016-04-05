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
        if (subCommand.equalsIgnoreCase("join"))
            return joinCommand(sender, args);

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
