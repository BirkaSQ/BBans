package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.PlayerService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** /warnings <игрок> */
public class WarningsCommand extends BaseCommand {

    public WarningsCommand(BBansPlugin plugin) {
        super(plugin, "bbans.warnings");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            usage(sender, "warnings");
            return;
        }
        PlayerService.PlayerInfo target = requireTarget(args[0]);
        List<Punishment> warnings = service.getActiveWarnings(target.getUuid());

        if (warnings.isEmpty()) {
            messages.send(sender, "warnings.empty", "player", target.getName());
            return;
        }
        messages.send(sender, "warnings.header", "player", target.getName());

        long now = System.currentTimeMillis();
        for (int i = 0; i < warnings.size(); i++) {
            Map<String, String> placeholders = messages.describe(warnings.get(i), now);
            placeholders.put("index", String.valueOf(i + 1));
            messages.send(sender, "warnings.entry", placeholders);
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
