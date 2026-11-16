package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.model.PunishmentType;
import ru.birkasq.bbans.service.PlayerService;

import java.util.Collections;
import java.util.List;

/** /unban <игрок> */
public class UnbanCommand extends BaseCommand {

    public UnbanCommand(BBansPlugin plugin) {
        super(plugin, "bbans.unban");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            usage(sender, "unban");
            return;
        }
        PlayerService.PlayerInfo target = requireTarget(args[0]);

        Punishment p = service.unban(target.getUuid(), uuidOf(sender), nameOf(sender));

        messages.send(sender, "unban.success", messages.describe(p, System.currentTimeMillis()));
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(service.getActivePunishedNames(PunishmentType.BAN, 100), args[0]);
        }
        return Collections.emptyList();
    }
}
