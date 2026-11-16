package ru.birkasq.bbans.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор и форматирование сроков: 30s, 10m, 2h, 7d, 2w, 1mo, 1y (можно комбинировать: 1d12h).
 */
public final class TimeParser {

    private static final Pattern FULL = Pattern.compile("^(?:\\d+(?:mo|[smhdwy]))+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN = Pattern.compile("(\\d+)(mo|[smhdwy])", Pattern.CASE_INSENSITIVE);

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private TimeParser() {
    }

    /** Похожа ли строка на срок (формат корректен, значение не проверяется). */
    public static boolean isDuration(String input) {
        return input != null && FULL.matcher(input).matches();
    }

    /**
     * @return миллисекунды или null, если формат неверный, значение равно нулю или переполнено
     */
    public static Long parse(String input) {
        if (!isDuration(input)) {
            return null;
        }
        long total = 0L;
        try {
            Matcher m = TOKEN.matcher(input);
            while (m.find()) {
                long value = Long.parseLong(m.group(1));
                long unit = unitMillis(m.group(2).toLowerCase());
                total = Math.addExact(total, Math.multiplyExact(value, unit));
            }
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
        return total > 0 ? total : null;
    }

    private static long unitMillis(String unit) {
        switch (unit) {
            case "s":  return SECOND;
            case "m":  return MINUTE;
            case "h":  return HOUR;
            case "d":  return DAY;
            case "w":  return 7 * DAY;
            case "mo": return 30 * DAY;
            case "y":  return 365 * DAY;
            default:   throw new IllegalArgumentException(unit);
        }
    }

    /** Две старшие ненулевые единицы, например "2d 5h". */
    public static String format(long millis, String d, String h, String m, String s) {
        long secs = Math.max(0L, millis) / 1000L;
        long days = secs / 86400L;
        long hours = (secs % 86400L) / 3600L;
        long minutes = (secs % 3600L) / 60L;
        long seconds = secs % 60L;

        List<String> parts = new ArrayList<>();
        if (days > 0) parts.add(days + d);
        if (hours > 0) parts.add(hours + h);
        if (minutes > 0) parts.add(minutes + m);
        if (seconds > 0) parts.add(seconds + s);
        if (parts.isEmpty()) {
            return "0" + s;
        }
        return parts.size() == 1 ? parts.get(0) : parts.get(0) + " " + parts.get(1);
    }

    public static String formatCompact(long millis) {
        return format(millis, "d", "h", "m", "s");
    }
}
