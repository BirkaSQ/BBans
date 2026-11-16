package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.model.PunishmentType;
import ru.birkasq.bbans.service.PlayerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** /unwarn <игрок> [номер] */
public class UnwarnCommand extends BaseCommand {

    public UnwarnCommand(BBansPlugin plugin) {
        super(plugin, "bbans.unwarn");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            usage(sender, "unwarn");
            return;
        }
        Integer index = null;
        if (args.length == 2) {
            try {
                index = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                usage(sender, "unwarn");
                return;
            }
        }
        PlayerService.PlayerInfo target = requireTarget(args[0]);

        Punishment p = service.unwarn(target.getUuid(), index, uuidOf(sender), nameOf(sender));

        messages.send(sender, "unwarn.success", messages.describe(p, System.currentTimeMillis()));
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(service.getActivePunishedNames(PunishmentType.WARN, 100), args[0]);
        }
        if (args.length == 2) {
            Optional<PlayerService.PlayerInfo> target = players.resolveKnown(args[0]);
            if (target.isPresent()) {
                int count = service.getActiveWarnings(target.get().getUuid()).size();
                List<String> numbers = new ArrayList<>();
                for (int i = 1; i <= count; i++) {
                    numbers.add(String.valueOf(i));
                }
                return filter(numbers, args[1]);
            }
        }
        return Collections.emptyList();
    }
}
