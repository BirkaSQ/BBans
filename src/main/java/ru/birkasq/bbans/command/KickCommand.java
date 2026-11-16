package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.PlayerService;

import java.util.Collections;
import java.util.List;

/** /kick <игрок> <причина> */
public class KickCommand extends BaseCommand {

    public KickCommand(BBansPlugin plugin) {
        super(plugin, "bbans.kick");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "kick");
            return;
        }
        PlayerService.PlayerInfo target = requireTarget(args[0]);

        Punishment p = service.kick(target.getUuid(), target.getName(), join(args, 1),
                uuidOf(sender), nameOf(sender));

        messages.send(sender, "kick.success", messages.describe(p, System.currentTimeMillis()));
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return onlineNames(args[0]);
        }
        return Collections.emptyList();
    }
}
