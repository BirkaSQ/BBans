package ru.birkasq.bbans.api.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.birkasq.bbans.api.model.PunishmentData;

/**
 * Вызывается после успешного создания наказания (после записи в БД).
 * Может вызываться не из основного потока (isAsynchronous() == true).
 */
public class PunishmentCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PunishmentData punishment;
    private final boolean automatic;

    public PunishmentCreateEvent(PunishmentData punishment, boolean automatic) {
        super(!Bukkit.isPrimaryThread());
        this.punishment = punishment;
        this.automatic = automatic;
    }

    public PunishmentData getPunishment() {
        return punishment;
    }

    /** true — наказание выдано автоматически (лимит WARN). */
    public boolean isAutomatic() {
        return automatic;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
