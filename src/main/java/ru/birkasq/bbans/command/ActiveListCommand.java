package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.model.PunishmentType;

import java.util.List;

/** Общая часть /banlist и /mutelist (10 записей на страницу). */
public abstract class ActiveListCommand extends BaseCommand {

    private static final int PAGE_SIZE = 10;

    private final PunishmentType type;
    private final String messagesKey;

    protected ActiveListCommand(BBansPlugin plugin, String permission, PunishmentType type, String messagesKey) {
        super(plugin, permission);
        this.type = type;
        this.messagesKey = messagesKey;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        long total = service.countActive(type);
        if (total == 0) {
            messages.send(sender, messagesKey + ".empty");
            return;
        }
        int pages = pageCount(total, PAGE_SIZE);
        int page = Math.min(parsePage(args, 0), pages);

        List<Punishment> entries = service.getActiveList(type, (long) (page - 1) * PAGE_SIZE, PAGE_SIZE);

        messages.send(sender, messagesKey + ".header", "page", page, "pages", pages, "total", total);
        long now = System.currentTimeMillis();
        for (Punishment p : entries) {
            messages.send(sender, messagesKey + ".entry", messages.describe(p, now));
        }
    }
}
