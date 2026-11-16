package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.PlayerService;

import java.util.Collections;
import java.util.List;

/** /check <игрок> [страница] — полная история, 5 записей на страницу. */
public class CheckCommand extends BaseCommand {

    private static final int PAGE_SIZE = 5;

    public CheckCommand(BBansPlugin plugin) {
        super(plugin, "bbans.check");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            usage(sender, "check");
            return;
        }
        PlayerService.PlayerInfo target = requireTarget(args[0]);

        long total = service.countHistory(target.getUuid());
        if (total == 0) {
            messages.send(sender, "check.empty", "player", target.getName());
            return;
        }
        int pages = pageCount(total, PAGE_SIZE);
        int page = Math.min(parsePage(args, 1), pages);

        List<Punishment> entries = service.getHistoryPage(target.getUuid(), (long) (page - 1) * PAGE_SIZE, PAGE_SIZE);

        messages.send(sender, "check.header", "player", target.getName(),
                "page", page, "pages", pages, "total", total);
        long now = System.currentTimeMillis();
        for (Punishment p : entries) {
            messages.send(sender, "check.entry", messages.describe(p, now));
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
