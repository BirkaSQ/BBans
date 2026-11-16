package ru.birkasq.bbans.api.exception;

/** Наказание с указанным ID не найдено. */
public class PunishmentNotFoundException extends BBansException {

    public PunishmentNotFoundException(long id) {
        super("punishment-not-found", "id", String.valueOf(id));
    }
}
