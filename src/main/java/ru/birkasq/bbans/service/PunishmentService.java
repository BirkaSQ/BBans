package ru.birkasq.bbans.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.api.event.PunishmentCreateEvent;
import ru.birkasq.bbans.api.event.PunishmentExpireEvent;
import ru.birkasq.bbans.api.event.PunishmentRemoveEvent;
import ru.birkasq.bbans.api.exception.BBansException;
import ru.birkasq.bbans.api.exception.HierarchyException;
import ru.birkasq.bbans.api.exception.PunishmentNotFoundException;
import ru.birkasq.bbans.logger.PunishmentLogger;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.model.PunishmentStatus;
import ru.birkasq.bbans.model.PunishmentType;
import ru.birkasq.bbans.repository.PunishmentRepository;
import ru.birkasq.bbans.util.MessageFormatter;
import ru.birkasq.bbans.util.TimeParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Главный business layer. Команды и публичный API работают только через этот класс.
 *
 * <p>Методы блокируют поток обращением к БД, поэтому вызывать их нужно не из основного потока
 * (команды и API-обёртки делают это сами). События и логирование выполняются только после
 * успешной записи в БД.</p>
 */
public class PunishmentService {

    public static final UUID CONSOLE_UUID = new UUID(0L, 0L);
    public static final String CONSOLE_NAME = "Console";

    private final BBansPlugin plugin;
    private final PunishmentRepository repository;
    private final HierarchyService hierarchy;
    private final PlayerService players;
    private final MessageFormatter messages;
    private final PunishmentLogger logger;

    /** Сериализует операции "проверил — записал", чтобы не создать два активных бана одновременно. */
    private final Object writeLock = new Object();

    public PunishmentService(BBansPlugin plugin, PunishmentRepository repository, HierarchyService hierarchy,
                             PlayerService players, MessageFormatter messages, PunishmentLogger logger) {
        this.plugin = plugin;
        this.repository = repository;
        this.hierarchy = hierarchy;
        this.players = players;
        this.messages = messages;
        this.logger = logger;
    }

    // ================================================================== create

    /** @param durationMs срок в миллисекундах, null — навсегда */
    public Punishment ban(UUID target, String targetName, String reason, Long durationMs,
                          UUID modUuid, String modName) {
        return create(PunishmentType.BAN, target, targetName, reason, durationMs, modUuid, modName);
    }

    /** @param durationMs срок в миллисекундах, null — навсегда */
    public Punishment mute(UUID target, String targetName, String reason, Long durationMs,
                           UUID modUuid, String modName) {
        return create(PunishmentType.MUTE, target, targetName, reason, durationMs, modUuid, modName);
    }

    public Punishment kick(UUID target, String targetName, String reason, UUID modUuid, String modName) {
        if (Bukkit.getPlayer(target) == null) {
            throw new BBansException("player-not-online", "player", targetName);
        }
        return create(PunishmentType.KICK, target, targetName, reason, null, modUuid, modName);
    }

    public WarnResult warn(UUID target, String targetName, String reason, UUID modUuid, String modName) {
        Punishment warning = create(PunishmentType.WARN, target, targetName, reason, null, modUuid, modName);

        long count = repository.countActive(target, PunishmentType.WARN, System.currentTimeMillis());
        Punishment auto = null;
        if (plugin.getConfig().getBoolean("warnings.auto-punishment.enabled", false)) {
            int limit = Math.max(1, plugin.getConfig().getInt("warnings.auto-punishment.limit", 3));
            if (count >= limit) {
                auto = applyAutoPunishment(warning, limit);
            }
        }
        return new WarnResult(warning, auto, count);
    }

    private Punishment create(PunishmentType type, UUID target, String targetName, String reason,
                              Long durationMs, UUID modUuid, String modName) {
        String finalReason = normalizeReason(reason);
        if (!CONSOLE_UUID.equals(modUuid) && modUuid.equals(target)) {
            throw new BBansException("cannot-self");
        }

        Punishment p;
        synchronized (writeLock) {
            long now = System.currentTimeMillis();
            if (type == PunishmentType.BAN && repository.findActive(target, PunishmentType.BAN, now).isPresent()) {
                throw new BBansException("already-banned", "player", targetName);
            }
            if (type == PunishmentType.MUTE && repository.findActive(target, PunishmentType.MUTE, now).isPresent()) {
                throw new BBansException("already-muted", "player", targetName);
            }
            if (!hierarchy.canPunish(modUuid, target)) {
                throw new HierarchyException("hierarchy-punish", "player", targetName);
            }
            p = build(type, target, targetName, finalReason, durationMs, modUuid, modName, false);
            players.remember(target, targetName, 0);
            repository.insert(p);
        }

        afterCreate(p, false);
        return p;
    }

    private Punishment applyAutoPunishment(Punishment warning, int limit) {
        String typeName = plugin.getConfig().getString("warnings.auto-punishment.type", "BAN");
        PunishmentType type = "MUTE".equalsIgnoreCase(typeName) ? PunishmentType.MUTE : PunishmentType.BAN;

        String durationRaw = plugin.getConfig().getString("warnings.auto-punishment.duration", "1d");
        Long durationMs = null;
        if (durationRaw != null && TimeParser.isDuration(durationRaw)) {
            durationMs = TimeParser.parse(durationRaw);
        }

        String reason = ChatColor.stripColor(messages.format("warnings.auto-reason", "limit", limit));

        Punishment p;
        synchronized (writeLock) {
            long now = System.currentTimeMillis();
            if (repository.findActive(warning.getPlayerUuid(), type, now).isPresent()) {
                return null; // уже наказан — повторно не наказываем
            }
            // Автором считается модератор, выдавший WARN: так история и ownership сохраняют источник
            p = build(type, warning.getPlayerUuid(), warning.getPlayerName(), reason, durationMs,
                    warning.getModeratorUuid(), warning.getModeratorName(), true);
            repository.insert(p);
        }

        afterCreate(p, true);
        return p;
    }

    private Punishment build(PunishmentType type, UUID target, String targetName, String reason, Long durationMs,
                             UUID modUuid, String modName, boolean auto) {
        long now = System.currentTimeMillis();
        Punishment p = new Punishment();
        p.setPlayerUuid(target);
        p.setPlayerName(targetName);
        p.setType(type);
        p.setReason(reason);
        p.setModeratorUuid(modUuid);
        p.setModeratorName(modName);
        p.setModeratorPriority(hierarchy.getPriority(modUuid));
        p.setCreatedAt(now);
        if ((type == PunishmentType.BAN || type == PunishmentType.MUTE) && durationMs != null) {
            p.setExpiresAt(now + Math.min(durationMs, Long.MAX_VALUE - now));
        } else {
            p.setExpiresAt(null);
        }
        // KICK — историческое событие: не бывает активным
        boolean history = type == PunishmentType.KICK;
        p.setActive(!history);
        p.setStatus(history ? PunishmentStatus.EXPIRED : PunishmentStatus.ACTIVE);
        p.setAuto(auto);
        return p;
    }

    private void afterCreate(Punishment p, boolean auto) {
        Bukkit.getPluginManager().callEvent(new PunishmentCreateEvent(p.toData(), auto));
        logger.logCreate(p, auto);

        long now = System.currentTimeMillis();
        Player online = Bukkit.getPlayer(p.getPlayerUuid());
        switch (p.getType()) {
            case BAN:
                kickLater(p.getPlayerUuid(), getBanScreen(p));
                break;
            case KICK:
                kickLater(p.getPlayerUuid(), messages.format("kick.screen",
                        "reason", p.getReason(), "moderator", p.getModeratorName()));
                break;
            case MUTE:
                if (online != null) {
                    messages.send(online, "mute.notify",
                            "reason", p.getReason(), "remaining", messages.formatRemaining(p.getExpiresAt(), now));
                }
                break;
            case WARN:
                if (online != null) {
                    messages.send(online, "warn.notify", "reason", p.getReason());
                }
                break;
            default:
                break;
        }
    }

    // ================================================================== remove

    public Punishment unban(UUID target, UUID modUuid, String modName) {
        Punishment p;
        synchronized (writeLock) {
            long now = System.currentTimeMillis();
            p = repository.findActive(target, PunishmentType.BAN, now)
                    .orElseThrow(() -> new BBansException("no-active-ban", "player", players.nameOrUuid(target)));
            markRemoved(p, modUuid, modName, now);
        }
        afterRemove(p, modUuid, modName);
        return p;
    }

    public Punishment unmute(UUID target, UUID modUuid, String modName) {
        Punishment p;
        synchronized (writeLock) {
            long now = System.currentTimeMillis();
            p = repository.findActive(target, PunishmentType.MUTE, now)
                    .orElseThrow(() -> new BBansException("no-active-mute", "player", players.nameOrUuid(target)));
            markRemoved(p, modUuid, modName, now);
        }
        afterRemove(p, modUuid, modName);
        return p;
    }

    /**
     * @param index номер активного предупреждения (как в /warnings, с 1); null — последнее
     */
    public Punishment unwarn(UUID target, Integer index, UUID modUuid, String modName) {
        Punishment p;
        synchronized (writeLock) {
            long now = System.currentTimeMillis();
            List<Punishment> warns = repository.findActiveWarnings(target, now);
            if (warns.isEmpty()) {
                throw new BBansException("no-active-warns", "player", players.nameOrUuid(target));
            }
            if (index == null) {
                p = warns.get(warns.size() - 1);
            } else {
                if (index < 1 || index > warns.size()) {
                    throw new BBansException("invalid-warn-number", "max", String.valueOf(warns.size()));
                }
                p = warns.get(index - 1);
            }
            markRemoved(p, modUuid, modName, now);
        }
        afterRemove(p, modUuid, modName);
        return p;
    }

    /** Снятие по ID (используется публичным API). */
    public Punishment removePunishment(long id, UUID modUuid, String modName) {
        Punishment p;
        synchronized (writeLock) {
            long now = System.currentTimeMillis();
            p = repository.findById(id).orElseThrow(() -> new PunishmentNotFoundException(id));
            if (p.getType() == PunishmentType.KICK) {
                throw new BBansException("not-removable");
            }
            if (!p.isCurrentlyActive(now)) {
                throw new BBansException("not-active");
            }
            markRemoved(p, modUuid, modName, now);
        }
        afterRemove(p, modUuid, modName);
        return p;
    }

    /** Проверка owner/hierarchy + запись в БД. Вызывается внутри writeLock. */
    private void markRemoved(Punishment p, UUID modUuid, String modName, long now) {
        if (!hierarchy.canModifyPunishment(modUuid, p)) {
            throw new HierarchyException("hierarchy-modify", "player", p.getPlayerName());
        }
        if (!repository.deactivate(p.getId(), PunishmentStatus.REMOVED, modUuid, modName, now)) {
            throw new BBansException("not-active");
        }
        p.setActive(false);
        p.setStatus(PunishmentStatus.REMOVED);
        p.setRemovedByUuid(modUuid);
        p.setRemovedByName(modName);
        p.setRemovedAt(now);
    }

    private void afterRemove(Punishment p, UUID modUuid, String modName) {
        Bukkit.getPluginManager().callEvent(new PunishmentRemoveEvent(p.toData(), modUuid));
        logger.logRemove(p, modName);
    }

    // ================================================================== expiration

    /** Переводит истёкшие наказания в EXPIRED и вызывает PunishmentExpireEvent. */
    public void processExpired() {
        long now = System.currentTimeMillis();
        for (Punishment p : repository.findExpired(now)) {
            if (repository.markExpired(p.getId())) {
                p.setActive(false);
                p.setStatus(PunishmentStatus.EXPIRED);
                Bukkit.getPluginManager().callEvent(new PunishmentExpireEvent(p.toData()));
            }
        }
    }

    // ================================================================== queries

    public Optional<Punishment> getActiveBan(UUID uuid) {
        return repository.findActive(uuid, PunishmentType.BAN, System.currentTimeMillis());
    }

    public Optional<Punishment> getActiveMute(UUID uuid) {
        return repository.findActive(uuid, PunishmentType.MUTE, System.currentTimeMillis());
    }

    public List<Punishment> getActiveWarnings(UUID uuid) {
        return repository.findActiveWarnings(uuid, System.currentTimeMillis());
    }

    public long getActiveWarningCount(UUID uuid) {
        return repository.countActive(uuid, PunishmentType.WARN, System.currentTimeMillis());
    }

    public List<Punishment> getActivePunishments(UUID uuid) {
        return repository.findActivePunishments(uuid, System.currentTimeMillis());
    }

    public List<Punishment> getHistory(UUID uuid) {
        return repository.findHistory(uuid, 0, Integer.MAX_VALUE);
    }

    public List<Punishment> getHistoryPage(UUID uuid, long offset, int limit) {
        return repository.findHistory(uuid, offset, limit);
    }

    public long countHistory(UUID uuid) {
        return repository.countHistory(uuid);
    }

    public List<Punishment> getActiveList(PunishmentType type, long offset, int limit) {
        return repository.findActiveByType(type, System.currentTimeMillis(), offset, limit);
    }

    public long countActive(PunishmentType type) {
        return repository.countActiveByType(type, System.currentTimeMillis());
    }

    public List<String> getActivePunishedNames(PunishmentType type, int limit) {
        List<String> names = repository.findActiveNames(type, System.currentTimeMillis(), limit);
        return new ArrayList<>(names);
    }

    public Optional<Punishment> findById(long id) {
        return repository.findById(id);
    }

    // ================================================================== helpers

    /** Текст экрана блокировки (при кике и при попытке входа). */
    public String getBanScreen(Punishment p) {
        return messages.format("ban.screen",
                "reason", p.getReason(),
                "moderator", p.getModeratorName(),
                "remaining", messages.formatRemaining(p.getExpiresAt(), System.currentTimeMillis()),
                "duration", messages.formatTerm(p.getCreatedAt(), p.getExpiresAt()));
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return plugin.getConfig().getString("default-reason", "Не указана");
        }
        return reason.trim();
    }

    /** Кик выполняется в основном потоке. */
    private void kickLater(final UUID uuid, final String message) {
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.kickPlayer(message);
                }
            }
        });
    }
}
