package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.PlayerService;

import java.util.Collections;
import java.util.List;

/** /ban <игрок> [время] <причина> */
public class BanCommand extends BaseCommand {

    public BanCommand(BBansPlugin plugin) {
        super(plugin, "bbans.ban");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "ban");
            return;
        }
        TimedArgs parsed = parseTimed(args);
        if (parsed == null) {
            usage(sender, "ban");
            return;
        }
        PlayerService.PlayerInfo target = requireTarget(args[0]);

        Punishment p = service.ban(target.getUuid(), target.getName(), parsed.reason, parsed.durationMs,
                uuidOf(sender), nameOf(sender));

        messages.send(sender, "ban.success", messages.describe(p, System.currentTimeMillis()));
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return onlineNames(args[0]);
        }
        if (args.length == 2) {
            return filter(DURATION_SUGGESTIONS, args[1]);
        }
        return Collections.emptyList();
    }
}
