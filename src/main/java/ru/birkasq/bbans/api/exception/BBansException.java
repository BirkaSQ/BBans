package ru.birkasq.bbans.api.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Базовое исключение BBans. Содержит ключ ошибки (используется для сообщения
 * из messages.yml: errors.&lt;key&gt;) и значения плейсхолдеров.
 */
public class BBansException extends RuntimeException {

    private final String key;
    private final Map<String, String> placeholders;

    /**
     * @param key          ключ ошибки
     * @param placeholders пары "ключ, значение, ключ, значение..."
     */
    public BBansException(String key, String... placeholders) {
        super(key);
        this.key = key;
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            map.put(placeholders[i], placeholders[i + 1]);
        }
        this.placeholders = Collections.unmodifiableMap(map);
    }

    public String getKey() {
        return key;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }
}
