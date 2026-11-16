package ru.birkasq.bbans.apiimpl;

import ru.birkasq.bbans.api.manager.PunishmentManager;
import ru.birkasq.bbans.api.model.PunishmentData;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.PlayerService;
import ru.birkasq.bbans.service.PunishmentService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Реализация публичного API поверх PunishmentService (та же бизнес-логика, что и у команд). */
public class PunishmentManagerImpl implements PunishmentManager {

    private final PunishmentService service;
    private final PlayerService players;
    private final Executor executor;

    public PunishmentManagerImpl(PunishmentService service, PlayerService players, Executor executor) {
        this.service = service;
        this.players = players;
        this.executor = executor;
    }

    // ------------------------------------------------------------------ read

    @Override
    public Optional<PunishmentData> getActiveBan(UUID playerUuid) {
        return service.getActiveBan(playerUuid).map(Punishment::toData);
    }

    @Override
    public Optional<PunishmentData> getActiveMute(UUID playerUuid) {
        return service.getActiveMute(playerUuid).map(Punishment::toData);
    }

    @Override
    public List<PunishmentData> getActiveWarnings(UUID playerUuid) {
        return toData(service.getActiveWarnings(playerUuid));
    }

    @Override
    public List<PunishmentData> getHistory(UUID playerUuid) {
        return toData(service.getHistory(playerUuid));
    }

    @Override
    public List<PunishmentData> getActivePunishments(UUID playerUuid) {
        return toData(service.getActivePunishments(playerUuid));
    }

    @Override
    public long getActiveWarningCount(UUID playerUuid) {
        return service.getActiveWarningCount(playerUuid);
    }

    @Override
    public boolean hasActiveBan(UUID playerUuid) {
        return service.getActiveBan(playerUuid).isPresent();
    }

    @Override
    public boolean hasActiveMute(UUID playerUuid) {
        return service.getActiveMute(playerUuid).isPresent();
    }

    @Override
    public Optional<PunishmentData> getPunishment(long punishmentId) {
        return service.findById(punishmentId).map(Punishment::toData);
    }

    // ------------------------------------------------------------------ create

    @Override
    public PunishmentData ban(UUID targetUuid, String reason, UUID moderatorUuid, Duration duration) {
        UUID mod = moderator(moderatorUuid);
        return service.ban(targetUuid, players.nameOrUuid(targetUuid), reason, millis(duration),
                mod, players.nameOrUuid(mod)).toData();
    }

    @Override
    public PunishmentData mute(UUID targetUuid, String reason, UUID moderatorUuid, Duration duration) {
        UUID mod = moderator(moderatorUuid);
        return service.mute(targetUuid, players.nameOrUuid(targetUuid), reason, millis(duration),
                mod, players.nameOrUuid(mod)).toData();
    }

    @Override
    public PunishmentData warn(UUID targetUuid, String reason, UUID moderatorUuid) {
        UUID mod = moderator(moderatorUuid);
        return service.warn(targetUuid, players.nameOrUuid(targetUuid), reason,
                mod, players.nameOrUuid(mod)).getWarning().toData();
    }

    @Override
    public PunishmentData kick(UUID targetUuid, String reason, UUID moderatorUuid) {
        UUID mod = moderator(moderatorUuid);
        return service.kick(targetUuid, players.nameOrUuid(targetUuid), reason,
                mod, players.nameOrUuid(mod)).toData();
    }

    // ------------------------------------------------------------------ remove

    @Override
    public void unban(UUID playerUuid, UUID moderatorUuid) {
        UUID mod = moderator(moderatorUuid);
        service.unban(playerUuid, mod, players.nameOrUuid(mod));
    }

    @Override
    public void unmute(UUID playerUuid, UUID moderatorUuid) {
        UUID mod = moderator(moderatorUuid);
        service.unmute(playerUuid, mod, players.nameOrUuid(mod));
    }

    @Override
    public void removePunishment(long punishmentId, UUID moderatorUuid) {
        UUID mod = moderator(moderatorUuid);
        service.removePunishment(punishmentId, mod, players.nameOrUuid(mod));
    }

    // ------------------------------------------------------------------ async

    @Override
    public CompletableFuture<Optional<PunishmentData>> getActiveBanAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getActiveBan(uuid), executor);
    }

    @Override
    public CompletableFuture<Optional<PunishmentData>> getActiveMuteAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getActiveMute(uuid), executor);
    }

    @Override
    public CompletableFuture<List<PunishmentData>> getWarningsAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getActiveWarnings(uuid), executor);
    }

    @Override
    public CompletableFuture<List<PunishmentData>> getHistoryAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getHistory(uuid), executor);
    }

    @Override
    public CompletableFuture<PunishmentData> banAsync(UUID targetUuid, String reason, UUID moderatorUuid, Duration duration) {
        return CompletableFuture.supplyAsync(() -> ban(targetUuid, reason, moderatorUuid, duration), executor);
    }

    @Override
    public CompletableFuture<PunishmentData> muteAsync(UUID targetUuid, String reason, UUID moderatorUuid, Duration duration) {
        return CompletableFuture.supplyAsync(() -> mute(targetUuid, reason, moderatorUuid, duration), executor);
    }

    @Override
    public CompletableFuture<PunishmentData> warnAsync(UUID targetUuid, String reason, UUID moderatorUuid) {
        return CompletableFuture.supplyAsync(() -> warn(targetUuid, reason, moderatorUuid), executor);
    }

    @Override
    public CompletableFuture<Void> unbanAsync(UUID playerUuid, UUID moderatorUuid) {
        return CompletableFuture.runAsync(() -> unban(playerUuid, moderatorUuid), executor);
    }

    @Override
    public CompletableFuture<Void> unmuteAsync(UUID playerUuid, UUID moderatorUuid) {
        return CompletableFuture.runAsync(() -> unmute(playerUuid, moderatorUuid), executor);
    }

    // ------------------------------------------------------------------ helpers

    private static UUID moderator(UUID moderatorUuid) {
        return moderatorUuid == null ? PunishmentService.CONSOLE_UUID : moderatorUuid;
    }

    private static Long millis(Duration duration) {
        if (duration == null) {
            return null;
        }
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive (or null for permanent)");
        }
        return duration.toMillis();
    }

    private static List<PunishmentData> toData(List<Punishment> list) {
        List<PunishmentData> result = new ArrayList<>(list.size());
        for (Punishment p : list) {
            result.add(p.toData());
        }
        return result;
    }
}
