package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.PlayerService;
import ru.birkasq.bbans.service.WarnResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** /warn <игрок> <причина> */
public class WarnCommand extends BaseCommand {

    public WarnCommand(BBansPlugin plugin) {
        super(plugin, "bbans.warn");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "warn");
            return;
        }
        PlayerService.PlayerInfo target = requireTarget(args[0]);

        WarnResult result = service.warn(target.getUuid(), target.getName(), join(args, 1),
                uuidOf(sender), nameOf(sender));

        long now = System.currentTimeMillis();
        Map<String, String> placeholders = messages.describe(result.getWarning(), now);
        placeholders.put("count", String.valueOf(result.getActiveCount()));
        messages.send(sender, "warn.success", placeholders);

        Punishment auto = result.getAutoPunishment();
        if (auto != null) {
            Map<String, String> autoPlaceholders = messages.describe(auto, now);
            autoPlaceholders.put("limit", String.valueOf(plugin.getConfig().getInt("warnings.auto-punishment.limit", 3)));
            messages.send(sender, "warn.auto-punished", autoPlaceholders);
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return onlineNames(args[0]);
        }
        return Collections.emptyList();
    }
}
