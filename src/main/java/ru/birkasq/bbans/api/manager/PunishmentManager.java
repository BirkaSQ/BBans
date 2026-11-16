package ru.birkasq.bbans.api.manager;

import ru.birkasq.bbans.api.model.PunishmentData;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Работа с наказаниями. Все операции проходят через тот же business layer,
 * что и команды (иерархия, owner, история, события, логирование).
 *
 * <p>Синхронные методы блокируют вызывающий поток обращением к БД — из основного
 * потока сервера предпочтительнее использовать *Async-варианты.
 * Параметр moderatorUuid == null означает консоль.
 * Ошибки бизнес-логики выбрасываются как {@link ru.birkasq.bbans.api.exception.BBansException}.</p>
 */
public interface PunishmentManager {

    // ---------- чтение ----------

    Optional<PunishmentData> getActiveBan(UUID playerUuid);

    Optional<PunishmentData> getActiveMute(UUID playerUuid);

    List<PunishmentData> getActiveWarnings(UUID playerUuid);

    /** Вся история игрока, новые записи первыми. */
    List<PunishmentData> getHistory(UUID playerUuid);

    /** Активные BAN / MUTE / WARN. */
    List<PunishmentData> getActivePunishments(UUID playerUuid);

    long getActiveWarningCount(UUID playerUuid);

    boolean hasActiveBan(UUID playerUuid);

    boolean hasActiveMute(UUID playerUuid);

    Optional<PunishmentData> getPunishment(long punishmentId);

    // ---------- создание ----------

    /** @param duration срок, null — навсегда */
    PunishmentData ban(UUID targetUuid, String reason, UUID moderatorUuid, Duration duration);

    /** @param duration срок, null — навсегда */
    PunishmentData mute(UUID targetUuid, String reason, UUID moderatorUuid, Duration duration);

    PunishmentData warn(UUID targetUuid, String reason, UUID moderatorUuid);

    PunishmentData kick(UUID targetUuid, String reason, UUID moderatorUuid);

    // ---------- снятие ----------

    void unban(UUID playerUuid, UUID moderatorUuid);

    void unmute(UUID playerUuid, UUID moderatorUuid);

    void removePunishment(long punishmentId, UUID moderatorUuid);

    // ---------- async ----------

    CompletableFuture<Optional<PunishmentData>> getActiveBanAsync(UUID uuid);

    CompletableFuture<Optional<PunishmentData>> getActiveMuteAsync(UUID uuid);

    CompletableFuture<List<PunishmentData>> getWarningsAsync(UUID uuid);

    CompletableFuture<List<PunishmentData>> getHistoryAsync(UUID uuid);

    CompletableFuture<PunishmentData> banAsync(UUID targetUuid, String reason, UUID moderatorUuid, Duration duration);

    CompletableFuture<PunishmentData> muteAsync(UUID targetUuid, String reason, UUID moderatorUuid, Duration duration);

    CompletableFuture<PunishmentData> warnAsync(UUID targetUuid, String reason, UUID moderatorUuid);

    CompletableFuture<Void> unbanAsync(UUID playerUuid, UUID moderatorUuid);

    CompletableFuture<Void> unmuteAsync(UUID playerUuid, UUID moderatorUuid);
}
