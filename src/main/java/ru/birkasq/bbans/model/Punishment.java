package ru.birkasq.bbans.model;

import ru.birkasq.bbans.api.model.PunishmentData;

import java.util.UUID;

/** Внутренняя (изменяемая) модель наказания. Внешним плагинам отдаётся {@link PunishmentData}. */
public class Punishment {

    private long id;
    private UUID playerUuid;
    private String playerName;
    private PunishmentType type;
    private String reason;
    private UUID moderatorUuid;
    private String moderatorName;
    private int moderatorPriority;
    private long createdAt;
    private Long expiresAt;
    private boolean active;
    private PunishmentStatus status;
    private UUID removedByUuid;
    private String removedByName;
    private Long removedAt;
    private boolean auto;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public UUID getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public PunishmentType getType() { return type; }
    public void setType(PunishmentType type) { this.type = type; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public UUID getModeratorUuid() { return moderatorUuid; }
    public void setModeratorUuid(UUID moderatorUuid) { this.moderatorUuid = moderatorUuid; }

    public String getModeratorName() { return moderatorName; }
    public void setModeratorName(String moderatorName) { this.moderatorName = moderatorName; }

    public int getModeratorPriority() { return moderatorPriority; }
    public void setModeratorPriority(int moderatorPriority) { this.moderatorPriority = moderatorPriority; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public PunishmentStatus getStatus() { return status; }
    public void setStatus(PunishmentStatus status) { this.status = status; }

    public UUID getRemovedByUuid() { return removedByUuid; }
    public void setRemovedByUuid(UUID removedByUuid) { this.removedByUuid = removedByUuid; }

    public String getRemovedByName() { return removedByName; }
    public void setRemovedByName(String removedByName) { this.removedByName = removedByName; }

    public Long getRemovedAt() { return removedAt; }
    public void setRemovedAt(Long removedAt) { this.removedAt = removedAt; }

    public boolean isAuto() { return auto; }
    public void setAuto(boolean auto) { this.auto = auto; }

    /** Срок истёк (expiresAt <= now). Бессрочное наказание не истекает. */
    public boolean isExpired(long now) {
        return expiresAt != null && expiresAt <= now;
    }

    /** Наказание действует прямо сейчас. */
    public boolean isCurrentlyActive(long now) {
        return active && !isExpired(now);
    }

    /** Статус с учётом срока: активное, но просроченное наказание считается EXPIRED. */
    public PunishmentStatus getEffectiveStatus(long now) {
        if (!active) {
            return status;
        }
        return isExpired(now) ? PunishmentStatus.EXPIRED : PunishmentStatus.ACTIVE;
    }

    public PunishmentData toData() {
        long now = System.currentTimeMillis();
        return new PunishmentData(
                id, playerUuid, playerName,
                ru.birkasq.bbans.api.model.PunishmentType.valueOf(type.name()),
                reason, moderatorUuid, moderatorName, createdAt, expiresAt,
                isCurrentlyActive(now),
                ru.birkasq.bbans.api.model.PunishmentStatus.valueOf(getEffectiveStatus(now).name()));
    }
}
