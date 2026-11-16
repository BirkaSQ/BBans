package ru.birkasq.bbans.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.model.PunishmentStatus;
import ru.birkasq.bbans.model.PunishmentType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Загрузка messages.yml, подстановка плейсхолдеров, цвета, форматирование дат и сроков. */
public class MessageFormatter {

    private final BBansPlugin plugin;
    private volatile FileConfiguration messages = new YamlConfiguration();
    private volatile DateTimeFormatter dateFormat =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    public MessageFormatter(BBansPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        InputStream in = plugin.getResource("messages.yml");
        if (in != null) {
            // Недостающие ключи берутся из встроенного messages.yml
            cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
        }
        this.messages = cfg;

        String pattern = plugin.getConfig().getString("date-format", "dd.MM.yyyy HH:mm");
        try {
            this.dateFormat = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid date-format in config.yml: " + pattern);
            this.dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
        }
    }

    // ------------------------------------------------------------------ messages

    /** Сообщение с цветами и подставленными плейсхолдерами (значения плейсхолдеров не раскрашиваются). */
    public String format(String path, Map<String, String> placeholders) {
        String raw = messages.getString(path);
        if (raw == null) {
            return ChatColor.RED + "[missing message: " + path + "]";
        }
        raw = raw.replace("{prefix}", messages.getString("prefix", ""));
        String out = ChatColor.translateAlternateColorCodes('&', raw);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    /** Пары "ключ, значение, ключ, значение...". */
    public String format(String path, Object... keyValues) {
        return format(path, pairs(keyValues));
    }

    public void send(CommandSender target, String path, Map<String, String> placeholders) {
        String text = format(path, placeholders);
        if (text.isEmpty()) {
            return;
        }
        for (String line : text.split("\n")) {
            target.sendMessage(line);
        }
    }

    public void send(CommandSender target, String path, Object... keyValues) {
        send(target, path, pairs(keyValues));
    }

    public static Map<String, String> pairs(Object... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), String.valueOf(keyValues[i + 1]));
        }
        return map;
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    // ------------------------------------------------------------------ time

    public String formatDate(long millis) {
        return dateFormat.format(Instant.ofEpochMilli(millis));
    }

    public String permanent() {
        return color(messages.getString("time.permanent", "Permanent"));
    }

    public String formatDuration(long millis) {
        return TimeParser.format(millis,
                messages.getString("time.day", "d"),
                messages.getString("time.hour", "h"),
                messages.getString("time.minute", "m"),
                messages.getString("time.second", "s"));
    }

    /** Срок наказания (весь, от создания до окончания). */
    public String formatTerm(long createdAt, Long expiresAt) {
        return expiresAt == null ? permanent() : formatDuration(expiresAt - createdAt);
    }

    /** Оставшееся время. */
    public String formatRemaining(Long expiresAt, long now) {
        return expiresAt == null ? permanent() : formatDuration(expiresAt - now);
    }

    public String statusText(Punishment p, long now) {
        if (p.getType() == PunishmentType.KICK) {
            return color(messages.getString("status.kick", "-"));
        }
        PunishmentStatus st = p.getEffectiveStatus(now);
        return color(messages.getString("status." + st.name().toLowerCase(), st.name()));
    }

    /** Все плейсхолдеры, описывающие наказание. */
    public Map<String, String> describe(Punishment p, long now) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(p.getId()));
        map.put("uuid", String.valueOf(p.getPlayerUuid()));
        map.put("player", p.getPlayerName());
        map.put("type", p.getType().name());
        map.put("reason", p.getReason());
        map.put("moderator", p.getModeratorName());
        map.put("date", formatDate(p.getCreatedAt()));
        map.put("duration", formatTerm(p.getCreatedAt(), p.getExpiresAt()));
        map.put("remaining", formatRemaining(p.getExpiresAt(), now));
        map.put("status", statusText(p, now));
        return map;
    }
}
