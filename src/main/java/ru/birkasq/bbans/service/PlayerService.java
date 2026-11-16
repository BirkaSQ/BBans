package ru.birkasq.bbans.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.birkasq.bbans.repository.PunishmentRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Поиск игроков. Основной идентификатор — UUID; имя нужно только для поиска и отображения.
 * Методы могут обращаться к БД — вызывать не из основного потока.
 */
public class PlayerService {

    /** Результат поиска игрока. */
    public static class PlayerInfo {
        private final UUID uuid;
        private final String name;

        public PlayerInfo(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
    }

    private final PunishmentRepository repository;

    public PlayerService(PunishmentRepository repository) {
        this.repository = repository;
    }

    @SuppressWarnings("deprecation")
    public Optional<PlayerInfo> resolve(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(new PlayerInfo(online.getUniqueId(), online.getName()));
        }
        Optional<PunishmentRepository.PlayerRecord> record = repository.findPlayerByName(name);
        if (record.isPresent()) {
            return Optional.of(new PlayerInfo(record.get().getUuid(), record.get().getName()));
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore()) {
            String realName = offline.getName() != null ? offline.getName() : name;
            return Optional.of(new PlayerInfo(offline.getUniqueId(), realName));
        }
        return Optional.empty();
    }

    /** Поиск только среди онлайн-игроков и записей БД (без обращений к Mojang) — для TabCompleter. */
    public Optional<PlayerInfo> resolveKnown(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(new PlayerInfo(online.getUniqueId(), online.getName()));
        }
        Optional<PunishmentRepository.PlayerRecord> record = repository.findPlayerByName(name);
        if (record.isPresent()) {
            return Optional.of(new PlayerInfo(record.get().getUuid(), record.get().getName()));
        }
        return Optional.empty();
    }

    public Optional<UUID> findUuid(String name) {
        return resolve(name).map(PlayerInfo::getUuid);
    }

    public Optional<String> findName(UUID uuid) {
        if (PunishmentService.CONSOLE_UUID.equals(uuid)) {
            return Optional.of(PunishmentService.CONSOLE_NAME);
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return Optional.of(online.getName());
        }
        Optional<PunishmentRepository.PlayerRecord> record = repository.findPlayerByUuid(uuid);
        if (record.isPresent()) {
            return Optional.of(record.get().getName());
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return Optional.ofNullable(name);
    }

    public String nameOrUuid(UUID uuid) {
        return findName(uuid).orElse(uuid.toString());
    }

    public boolean isOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    /** Запоминает/обновляет игрока (имя и приоритет иерархии). */
    public void touch(UUID uuid, String name, int priority) {
        repository.upsertPlayer(uuid, name, priority, System.currentTimeMillis());
    }

    /** Запоминает игрока, если его ещё нет в таблице. */
    public void remember(UUID uuid, String name, int priority) {
        repository.insertPlayerIfAbsent(uuid, name, priority, System.currentTimeMillis());
    }
}
