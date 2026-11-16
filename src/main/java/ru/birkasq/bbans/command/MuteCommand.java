package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.PlayerService;

import java.util.Collections;
import java.util.List;

/** /mute <игрок> [время] <причина> */
public class MuteCommand extends BaseCommand {

    public MuteCommand(BBansPlugin plugin) {
        super(plugin, "bbans.mute");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "mute");
            return;
        }
        TimedArgs parsed = parseTimed(args);
        if (parsed == null) {
            usage(sender, "mute");
            return;
        }
        PlayerService.PlayerInfo target = requireTarget(args[0]);

        Punishment p = service.mute(target.getUuid(), target.getName(), parsed.reason, parsed.durationMs,
                uuidOf(sender), nameOf(sender));

        messages.send(sender, "mute.success", messages.describe(p, System.currentTimeMillis()));
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
