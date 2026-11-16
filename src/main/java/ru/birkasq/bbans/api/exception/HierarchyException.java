package ru.birkasq.bbans.api.exception;

/** Действие запрещено правилами иерархии. */
public class HierarchyException extends BBansException {

    public HierarchyException(String key, String... placeholders) {
        super(key, placeholders);
    }
}
