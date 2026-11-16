package ru.birkasq.bbans.api.model;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Неизменяемое представление наказания для внешних плагинов. */
public class PunishmentData {

    private final long id;
    private final UUID playerUuid;
    private final String playerName;
    private final PunishmentType type;
    private final String reason;
    private final UUID moderatorUuid;
    private final String moderatorName;
    private final long createdAt;
    private final Long expiresAt;
    private final boolean active;
    private final PunishmentStatus status;

    public PunishmentData(long id, UUID playerUuid, String playerName, PunishmentType type,
                          String reason, UUID moderatorUuid, String moderatorName,
                          long createdAt, Long expiresAt, boolean active, PunishmentStatus status) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.type = type;
        this.reason = reason;
        this.moderatorUuid = moderatorUuid;
        this.moderatorName = moderatorName;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.status = status;
    }

    public long getId() { return id; }

    public UUID getPlayerUuid() { return playerUuid; }

    public String getPlayerName() { return playerName; }

    public PunishmentType getType() { return type; }

    public String getReason() { return reason; }

    public UUID getModeratorUuid() { return moderatorUuid; }

    public String getModeratorName() { return moderatorName; }

    /** Время создания (epoch millis). */
    public long getCreatedAt() { return createdAt; }

    /** Время окончания (epoch millis) или null, если наказание бессрочное. */
    public Long getExpiresAt() { return expiresAt; }

    /** true — наказание действует прямо сейчас. */
    public boolean isActive() { return active; }

    public PunishmentStatus getStatus() { return status; }

    public boolean isPermanent() { return expiresAt == null; }

    /** Оставшееся время; пусто, если наказание бессрочное. */
    public Optional<Duration> getRemainingTime() {
        if (expiresAt == null) {
            return Optional.empty();
        }
        long left = expiresAt - System.currentTimeMillis();
        return Optional.of(Duration.ofMillis(Math.max(0L, left)));
    }

    @Override
    public String toString() {
        return "PunishmentData{id=" + id + ", type=" + type + ", player=" + playerName
                + ", status=" + status + "}";
    }
}
