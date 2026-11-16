package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.model.PunishmentType;
import ru.birkasq.bbans.service.PlayerService;

import java.util.Collections;
import java.util.List;

/** /unmute <игрок> */
public class UnmuteCommand extends BaseCommand {

    public UnmuteCommand(BBansPlugin plugin) {
        super(plugin, "bbans.unmute");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            usage(sender, "unmute");
            return;
        }
        PlayerService.PlayerInfo target = requireTarget(args[0]);

        Punishment p = service.unmute(target.getUuid(), uuidOf(sender), nameOf(sender));

        messages.send(sender, "unmute.success", messages.describe(p, System.currentTimeMillis()));
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(service.getActivePunishedNames(PunishmentType.MUTE, 100), args[0]);
        }
        return Collections.emptyList();
    }
}
