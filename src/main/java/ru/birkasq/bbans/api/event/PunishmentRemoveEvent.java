package ru.birkasq.bbans.api.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.birkasq.bbans.api.model.PunishmentData;

import java.util.UUID;

/**
 * Вызывается после успешного снятия наказания.
 * Может вызываться не из основного потока (isAsynchronous() == true).
 */
public class PunishmentRemoveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PunishmentData punishment;
    private final UUID removerUuid;

    public PunishmentRemoveEvent(PunishmentData punishment, UUID removerUuid) {
        super(!Bukkit.isPrimaryThread());
        this.punishment = punishment;
        this.removerUuid = removerUuid;
    }

    public PunishmentData getPunishment() {
        return punishment;
    }

    public UUID getRemoverUuid() {
        return removerUuid;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
