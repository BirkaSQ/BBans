package ru.birkasq.bbans.logger;

import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.model.PunishmentType;
import ru.birkasq.bbans.util.TimeParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/** Логирование действий модераторов (консоль + plugins/BBans/logs/actions.log). */
public class PunishmentLogger {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BBansPlugin plugin;
    private final Object fileLock = new Object();

    public PunishmentLogger(BBansPlugin plugin) {
        this.plugin = plugin;
    }

    public void logCreate(Punishment p, boolean auto) {
        String verb;
        switch (p.getType()) {
            case BAN:  verb = "banned"; break;
            case MUTE: verb = "muted"; break;
            case WARN: verb = "warned"; break;
            default:   verb = "kicked"; break;
        }
        String term = "";
        if (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.MUTE) {
            term = p.getExpiresAt() == null
                    ? " permanently"
                    : " for " + TimeParser.formatCompact(p.getExpiresAt() - p.getCreatedAt());
        }
        String prefix = auto ? "AUTO " + p.getType().name() + ": " : "";
        write(prefix + p.getModeratorName() + " " + verb + " " + p.getPlayerName() + term
                + ". Reason: " + p.getReason());
    }

    public void logRemove(Punishment p, String removerName) {
        String verb;
        switch (p.getType()) {
            case BAN:  verb = "unbanned"; break;
            case MUTE: verb = "unmuted"; break;
            default:   verb = "removed " + p.getType().name() + " of"; break;
        }
        write(removerName + " " + verb + " " + p.getPlayerName() + " (#" + p.getId() + ").");
    }

    private void write(String line) {
        if (plugin.getConfig().getBoolean("logging.console", true)) {
            plugin.getLogger().info(line);
        }
        if (plugin.getConfig().getBoolean("logging.file", true)) {
            synchronized (fileLock) {
                try {
                    File dir = new File(plugin.getDataFolder(), "logs");
                    if (!dir.exists() && !dir.mkdirs()) {
                        return;
                    }
                    File file = new File(dir, "actions.log");
                    String text = "[" + LocalDateTime.now().format(TS) + "] " + line + System.lineSeparator();
                    Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Cannot write actions.log", e);
                }
            }
        }
    }
}
