package ru.birkasq.bbans.api.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.birkasq.bbans.api.model.PunishmentData;

/**
 * Вызывается при истечении срока наказания.
 * Вызывается из асинхронной задачи (isAsynchronous() == true).
 */
public class PunishmentExpireEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PunishmentData punishment;

    public PunishmentExpireEvent(PunishmentData punishment) {
        super(!Bukkit.isPrimaryThread());
        this.punishment = punishment;
    }

    public PunishmentData getPunishment() {
        return punishment;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
