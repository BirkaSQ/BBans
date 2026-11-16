package ru.birkasq.bbans.command;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.BBansPlugin;

import java.util.Collections;
import java.util.List;

/** /bbans reload */
public class BBansCommand extends BaseCommand {

    public BBansCommand(BBansPlugin plugin) {
        super(plugin, "bbans.reload");
    }

    @Override
    protected boolean isAsync() {
        return false; // перезагрузка конфигурации выполняется в основном потоке
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            messages.send(sender, "reload.success");
            return;
        }
        usage(sender, "bbans");
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(Collections.singletonList("reload"), args[0]);
        }
        return Collections.emptyList();
    }
}
