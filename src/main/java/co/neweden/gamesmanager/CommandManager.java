package co.neweden.gamesmanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class CommandManager implements CommandExecutor {

    private GMMain plugin;

    public CommandManager(GMMain plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (args.length == 0) {
            // Return version info if no sub-command
            if (sender.hasPermission("gamesmanager.version"))
                sender.sendMessage(String.format(Util.formatString("&b%s &aversion %s&b"), plugin.getName(), plugin.getDescription().getVersion()));
            else
                sender.sendMessage(String.format(Util.formatString("&b%s"), plugin.getName()));

            // Command help
            sender.sendMessage(String.format(Util.formatString("&bRun &e%s&b followed by the name of a game for commands specific to that game."), commandLabel));
            return true;
        }

        Game game = GamesManager.getGameByName(args[0]);
        if (game == null) {
            sender.sendMessage(String.format(Util.formatString("&cThe game %s either does not exist or is disabled."), args[0]));
            return true;
        }

        if (!sender.hasPermission("gamesmanager.game." + game.getName())) {
            if (!sender.hasPermission("gamesmanager.game.*")) {
                sender.sendMessage(String.format(Util.formatString("&cYou do not have permission to run commands for game: %s"), game.getName()));
                return true;
            }
        }

        String[] gArgs = Arrays.copyOfRange(args, 1, args.length);
        // TODO: doesn't work with new structure
        //game.getGame().onCommand(sender, gArgs);

        return true;
    }

}
