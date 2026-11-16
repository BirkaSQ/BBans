package ru.birkasq.bbans.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.api.exception.BBansException;
import ru.birkasq.bbans.service.PlayerService;
import ru.birkasq.bbans.service.PunishmentService;
import ru.birkasq.bbans.util.MessageFormatter;
import ru.birkasq.bbans.util.TimeParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Общая часть команд: проверка права, асинхронное выполнение (БД не блокирует основной поток),
 * перевод BBansException в сообщения из messages.yml. Команды не содержат бизнес-логики —
 * только разбор аргументов, вызов сервиса и отправка результата.
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected static final List<String> DURATION_SUGGESTIONS = Arrays.asList("30m", "1h", "1d", "7d", "30d");

    protected final BBansPlugin plugin;
    protected final PunishmentService service;
    protected final PlayerService players;
    protected final MessageFormatter messages;
    private final String permission;

    protected BaseCommand(BBansPlugin plugin, String permission) {
        this.plugin = plugin;
        this.service = plugin.getPunishmentService();
        this.players = plugin.getPlayerService();
        this.messages = plugin.getMessages();
        this.permission = permission;
    }

    /** Выполнение команды (в асинхронном потоке, если {@link #isAsync()} == true). */
    protected abstract void execute(CommandSender sender, String[] args);

    protected boolean isAsync() {
        return true;
    }

    protected List<String> complete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public final boolean onCommand(final CommandSender sender, Command command, final String label,
                                   final String[] args) {
        if (!sender.hasPermission(permission)) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    execute(sender, args);
                } catch (BBansException e) {
                    messages.send(sender, "errors." + e.getKey(), e.getPlaceholders());
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "Error while executing /" + label, t);
                    messages.send(sender, "errors.internal");
                }
            }
        };
        if (isAsync()) {
            plugin.runAsync(task);
        } else {
            task.run();
        }
        return true;
    }

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        try {
            List<String> result = complete(sender, args);
            return result == null ? Collections.<String>emptyList() : result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ------------------------------------------------------------------ helpers

    protected void usage(CommandSender sender, String command) {
        messages.send(sender, "usage." + command);
    }

    protected PlayerService.PlayerInfo requireTarget(String name) {
        return players.resolve(name).orElseThrow(() -> new BBansException("player-not-found", "player", name));
    }

    protected static java.util.UUID uuidOf(CommandSender sender) {
        return sender instanceof Player ? ((Player) sender).getUniqueId() : PunishmentService.CONSOLE_UUID;
    }

    protected static String nameOf(CommandSender sender) {
        return sender instanceof Player ? sender.getName() : PunishmentService.CONSOLE_NAME;
    }

    protected static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    protected static int parsePage(String[] args, int index) {
        if (args.length <= index) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(args[index]));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    protected static int pageCount(long total, int pageSize) {
        return (int) Math.max(1L, (total + pageSize - 1) / pageSize);
    }

    /** Разобранные аргументы вида "[время] причина". */
    protected static class TimedArgs {
        final Long durationMs;
        final String reason;

        TimedArgs(Long durationMs, String reason) {
            this.durationMs = durationMs;
            this.reason = reason;
        }
    }

    /**
     * Разбор "<игрок> [время] <причина...>". Возвращает null, если причина не указана.
     * Если args[1] похоже на срок (30s, 10m, 2h, 7d, 2w, 1mo, 1y) — это срок, иначе — начало причины.
     */
    protected TimedArgs parseTimed(String[] args) {
        Long duration = null;
        int reasonStart = 1;
        if (args.length > 1 && TimeParser.isDuration(args[1])) {
            duration = TimeParser.parse(args[1]);
            if (duration == null) {
                throw new BBansException("invalid-duration");
            }
            reasonStart = 2;
        }
        if (args.length <= reasonStart) {
            return null;
        }
        return new TimedArgs(duration, join(args, reasonStart));
    }

    protected static List<String> onlineNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return filter(names, prefix);
    }

    protected static List<String> filter(Collection<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }
}
