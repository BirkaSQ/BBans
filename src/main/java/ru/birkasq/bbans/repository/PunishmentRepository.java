package ru.birkasq.bbans.repository;

import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.model.PunishmentStatus;
import ru.birkasq.bbans.model.PunishmentType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Только работа с данными: никаких permissions, иерархии, сообщений и бизнес-правил. */
public class PunishmentRepository {

    /** Запись из таблицы players. */
    public static class PlayerRecord {
        private final UUID uuid;
        private final String name;
        private final int priority;

        public PlayerRecord(UUID uuid, String name, int priority) {
            this.uuid = uuid;
            this.name = name;
            this.priority = priority;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public int getPriority() { return priority; }
    }

    private static final String COLUMNS = "id, player_uuid, player_name, type, reason, moderator_uuid, "
            + "moderator_name, moderator_priority, created_at, expires_at, active, status, "
            + "removed_by_uuid, removed_by_name, removed_at, auto";

    /** Условие "наказание действует сейчас"; параметр — текущее время. */
    private static final String ACTIVE_NOW = "active = 1 AND (expires_at IS NULL OR expires_at > ?)";

    private final DatabaseManager db;

    public PunishmentRepository(DatabaseManager db) {
        this.db = db;
    }

    // ================================================================== punishments

    public Punishment insert(Punishment p) {
        return db.execute(c -> {
            String sql = "INSERT INTO punishments (player_uuid, player_name, type, reason, moderator_uuid, "
                    + "moderator_name, moderator_priority, created_at, expires_at, active, status, "
                    + "removed_by_uuid, removed_by_name, removed_at, auto) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                ps.setString(i++, p.getPlayerUuid().toString());
                ps.setString(i++, p.getPlayerName());
                ps.setString(i++, p.getType().name());
                ps.setString(i++, p.getReason());
                ps.setString(i++, p.getModeratorUuid().toString());
                ps.setString(i++, p.getModeratorName());
                ps.setInt(i++, p.getModeratorPriority());
                ps.setLong(i++, p.getCreatedAt());
                setNullableLong(ps, i++, p.getExpiresAt());
                ps.setInt(i++, p.isActive() ? 1 : 0);
                ps.setString(i++, p.getStatus().name());
                setNullableString(ps, i++, p.getRemovedByUuid() == null ? null : p.getRemovedByUuid().toString());
                setNullableString(ps, i++, p.getRemovedByName());
                setNullableLong(ps, i++, p.getRemovedAt());
                ps.setInt(i, p.isAuto() ? 1 : 0);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        p.setId(keys.getLong(1));
                    }
                }
            }
            return p;
        });
    }

    public Optional<Punishment> findById(long id) {
        return db.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM punishments WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Punishment>empty();
                }
            }
        });
    }

    public Optional<Punishment> findActive(UUID uuid, PunishmentType type, long now) {
        return db.execute(c -> {
            String sql = "SELECT " + COLUMNS + " FROM punishments WHERE player_uuid = ? AND type = ? AND "
                    + ACTIVE_NOW + " ORDER BY id DESC LIMIT 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, type.name());
                ps.setLong(3, now);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Punishment>empty();
                }
            }
        });
    }

    public Optional<Punishment> findActiveBan(UUID uuid, long now) {
        return findActive(uuid, PunishmentType.BAN, now);
    }

    public Optional<Punishment> findActiveMute(UUID uuid, long now) {
        return findActive(uuid, PunishmentType.MUTE, now);
    }

    /** Активные WARN, старые первыми (порядок совпадает с нумерацией в /warnings). */
    public List<Punishment> findActiveWarnings(UUID uuid, long now) {
        return db.execute(c -> {
            String sql = "SELECT " + COLUMNS + " FROM punishments WHERE player_uuid = ? AND type = 'WARN' AND "
                    + ACTIVE_NOW + " ORDER BY created_at ASC, id ASC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, now);
                return readAll(ps);
            }
        });
    }

    public List<Punishment> findActivePunishments(UUID uuid, long now) {
        return db.execute(c -> {
            String sql = "SELECT " + COLUMNS + " FROM punishments WHERE player_uuid = ? AND "
                    + "type IN ('BAN','MUTE','WARN') AND " + ACTIVE_NOW + " ORDER BY id DESC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, now);
                return readAll(ps);
            }
        });
    }

    public long countActive(UUID uuid, PunishmentType type, long now) {
        return db.execute(c -> {
            String sql = "SELECT COUNT(*) FROM punishments WHERE player_uuid = ? AND type = ? AND " + ACTIVE_NOW;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, type.name());
                ps.setLong(3, now);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    /** История игрока, новые записи первыми. */
    public List<Punishment> findHistory(UUID uuid, long offset, int limit) {
        return db.execute(c -> {
            String sql = "SELECT " + COLUMNS + " FROM punishments WHERE player_uuid = ? "
                    + "ORDER BY id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, limit);
                ps.setLong(3, offset);
                return readAll(ps);
            }
        });
    }

    public long countHistory(UUID uuid) {
        return db.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM punishments WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    public List<Punishment> findActiveByType(PunishmentType type, long now, long offset, int limit) {
        return db.execute(c -> {
            String sql = "SELECT " + COLUMNS + " FROM punishments WHERE type = ? AND " + ACTIVE_NOW
                    + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, type.name());
                ps.setLong(2, now);
                ps.setInt(3, limit);
                ps.setLong(4, offset);
                return readAll(ps);
            }
        });
    }

    public long countActiveByType(PunishmentType type, long now) {
        return db.execute(c -> {
            String sql = "SELECT COUNT(*) FROM punishments WHERE type = ? AND " + ACTIVE_NOW;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, type.name());
                ps.setLong(2, now);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    /** Имена игроков с активными наказаниями данного типа (для TabCompleter). */
    public List<String> findActiveNames(PunishmentType type, long now, int limit) {
        return db.execute(c -> {
            String sql = "SELECT DISTINCT player_name FROM punishments WHERE type = ? AND " + ACTIVE_NOW
                    + " LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, type.name());
                ps.setLong(2, now);
                ps.setInt(3, limit);
                List<String> names = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        names.add(rs.getString(1));
                    }
                }
                return names;
            }
        });
    }

    /** Наказания, которые помечены активными, но срок которых уже истёк. */
    public List<Punishment> findExpired(long now) {
        return db.execute(c -> {
            String sql = "SELECT " + COLUMNS + " FROM punishments WHERE active = 1 "
                    + "AND expires_at IS NOT NULL AND expires_at <= ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, now);
                return readAll(ps);
            }
        });
    }

    /** @return true, если запись была активной и теперь переведена в EXPIRED */
    public boolean markExpired(long id) {
        return db.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE punishments SET active = 0, status = 'EXPIRED' WHERE id = ? AND active = 1")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /**
     * Снимает наказание (запись физически не удаляется).
     *
     * @return true, если запись была активной
     */
    public boolean deactivate(long id, PunishmentStatus status, UUID removerUuid, String removerName, long now) {
        return db.execute(c -> {
            String sql = "UPDATE punishments SET active = 0, status = ?, removed_by_uuid = ?, "
                    + "removed_by_name = ?, removed_at = ? WHERE id = ? AND active = 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, status.name());
                ps.setString(2, removerUuid.toString());
                ps.setString(3, removerName);
                ps.setLong(4, now);
                ps.setLong(5, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    // ================================================================== players

    public void upsertPlayer(UUID uuid, String name, int priority, long now) {
        db.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR REPLACE INTO players (uuid, name, priority, last_seen) VALUES (?,?,?,?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setInt(3, priority);
                ps.setLong(4, now);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void insertPlayerIfAbsent(UUID uuid, String name, int priority, long now) {
        db.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO players (uuid, name, priority, last_seen) VALUES (?,?,?,?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setInt(3, priority);
                ps.setLong(4, now);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<PlayerRecord> findPlayerByName(String name) {
        return db.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT uuid, name, priority FROM players WHERE name = ? COLLATE NOCASE "
                            + "ORDER BY last_seen DESC LIMIT 1")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapPlayer(rs)) : Optional.<PlayerRecord>empty();
                }
            }
        });
    }

    public Optional<PlayerRecord> findPlayerByUuid(UUID uuid) {
        return db.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT uuid, name, priority FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapPlayer(rs)) : Optional.<PlayerRecord>empty();
                }
            }
        });
    }

    // ================================================================== helpers

    private static List<Punishment> readAll(PreparedStatement ps) throws SQLException {
        List<Punishment> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private static PlayerRecord mapPlayer(ResultSet rs) throws SQLException {
        return new PlayerRecord(UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getInt("priority"));
    }

    private static Punishment map(ResultSet rs) throws SQLException {
        Punishment p = new Punishment();
        p.setId(rs.getLong("id"));
        p.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        p.setPlayerName(rs.getString("player_name"));
        p.setType(PunishmentType.valueOf(rs.getString("type")));
        p.setReason(rs.getString("reason"));
        p.setModeratorUuid(UUID.fromString(rs.getString("moderator_uuid")));
        p.setModeratorName(rs.getString("moderator_name"));
        p.setModeratorPriority(rs.getInt("moderator_priority"));
        p.setCreatedAt(rs.getLong("created_at"));
        long expires = rs.getLong("expires_at");
        p.setExpiresAt(rs.wasNull() ? null : expires);
        p.setActive(rs.getInt("active") == 1);
        p.setStatus(PunishmentStatus.valueOf(rs.getString("status")));
        String removedBy = rs.getString("removed_by_uuid");
        p.setRemovedByUuid(removedBy == null ? null : UUID.fromString(removedBy));
        p.setRemovedByName(rs.getString("removed_by_name"));
        long removedAt = rs.getLong("removed_at");
        p.setRemovedAt(rs.wasNull() ? null : removedAt);
        p.setAuto(rs.getInt("auto") == 1);
        return p;
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setLong(index, value);
        }
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }
}
