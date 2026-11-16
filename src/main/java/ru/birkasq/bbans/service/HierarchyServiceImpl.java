package ru.birkasq.bbans.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.hierarchy.HierarchyGroup;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.repository.PunishmentRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Иерархия строится на обычных Bukkit-permissions (bbans.hierarchy.*) и не зависит от LuckPerms.
 * Для офлайн-игроков используется приоритет, сохранённый в БД при их последнем выходе.
 */
public class HierarchyServiceImpl implements HierarchyService {

    private final BBansPlugin plugin;
    private final PunishmentRepository repository;

    private volatile boolean enabled = true;
    private volatile HierarchyGroup defaultGroup = HierarchyGroup.defaults(0);
    private volatile List<HierarchyGroup> groups = Collections.emptyList();

    public HierarchyServiceImpl(BBansPlugin plugin, PunishmentRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        reload();
    }

    public void reload() {
        this.enabled = plugin.getConfig().getBoolean("hierarchy.enabled", true);
        this.defaultGroup = HierarchyGroup.defaults(plugin.getConfig().getInt("hierarchy.default-priority", 0));

        List<HierarchyGroup> loaded = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("hierarchy.groups");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection groupSection = section.getConfigurationSection(key);
                if (groupSection != null) {
                    loaded.add(HierarchyGroup.fromConfig(key, groupSection));
                }
            }
        }
        // Группа с наибольшим priority проверяется первой
        Collections.sort(loaded, new Comparator<HierarchyGroup>() {
            @Override
            public int compare(HierarchyGroup a, HierarchyGroup b) {
                return Integer.compare(b.getPriority(), a.getPriority());
            }
        });
        this.groups = loaded;
    }

    // ------------------------------------------------------------------ groups

    @Override
    public HierarchyGroup getGroup(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return HierarchyGroup.console();
        }
        return groupOf((Player) sender);
    }

    @Override
    public HierarchyGroup getGroup(UUID uuid) {
        if (PunishmentService.CONSOLE_UUID.equals(uuid)) {
            return HierarchyGroup.console();
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return groupOf(online);
        }
        Optional<PunishmentRepository.PlayerRecord> record = repository.findPlayerByUuid(uuid);
        int priority = record.isPresent() ? record.get().getPriority() : defaultGroup.getPriority();
        return groupForPriority(priority);
    }

    @Override
    public int getPriority(CommandSender sender) {
        return getGroup(sender).getPriority();
    }

    @Override
    public int getPriority(UUID uuid) {
        return getGroup(uuid).getPriority();
    }

    private HierarchyGroup groupOf(Player player) {
        for (HierarchyGroup group : groups) {
            if (player.hasPermission(group.getPermission())) {
                return group;
            }
        }
        return defaultGroup;
    }

    private HierarchyGroup groupForPriority(int priority) {
        for (HierarchyGroup group : groups) {
            if (group.getPriority() == priority) {
                return group;
            }
        }
        return defaultGroup;
    }

    // ------------------------------------------------------------------ punish

    @Override
    public boolean canPunish(CommandSender sender, OfflinePlayer target) {
        return canPunish(uuidOf(sender), target.getUniqueId());
    }

    @Override
    public boolean canPunish(UUID moderatorUuid, UUID targetUuid) {
        if (!enabled) {
            return true;
        }
        HierarchyGroup moderator = getGroup(moderatorUuid);
        if (moderator.isConsole()) {
            return true;
        }
        HierarchyGroup target = getGroup(targetUuid);
        if (target.isDefault()) {
            // Обычный игрок без hierarchy-permission: наказать может любой модератор
            return true;
        }
        int m = moderator.getPriority();
        int t = target.getPriority();
        if (t < m) {
            return moderator.isPunishLower();
        }
        if (t == m) {
            return moderator.isPunishEqual();
        }
        return moderator.isPunishHigher();
    }

    // ------------------------------------------------------------------ modify

    @Override
    public boolean canModifyPunishment(CommandSender sender, Punishment punishment) {
        return canModifyPunishment(uuidOf(sender), punishment);
    }

    @Override
    public boolean canModifyPunishment(UUID moderatorUuid, Punishment punishment) {
        if (!enabled) {
            return true;
        }
        HierarchyGroup moderator = getGroup(moderatorUuid);
        if (moderator.isConsole()) {
            return true;
        }
        // 1. Владелец определяется только по UUID
        if (isOwner(moderatorUuid, punishment)) {
            return moderator.isModifyOwn();
        }
        // 2. Чужое наказание: сравниваем приоритеты
        int owner = ownerPriority(punishment);
        int m = moderator.getPriority();
        if (m > owner) {
            return moderator.isModifyLower();
        }
        if (m == owner) {
            return moderator.isModifyEqual();
        }
        return moderator.isModifyHigher();
    }

    @Override
    public boolean isOwner(CommandSender sender, Punishment punishment) {
        return isOwner(uuidOf(sender), punishment);
    }

    @Override
    public boolean isOwner(UUID moderatorUuid, Punishment punishment) {
        return punishment.getModeratorUuid().equals(moderatorUuid);
    }

    private int ownerPriority(Punishment punishment) {
        UUID owner = punishment.getModeratorUuid();
        if (PunishmentService.CONSOLE_UUID.equals(owner)) {
            return Integer.MAX_VALUE;
        }
        Player online = Bukkit.getPlayer(owner);
        if (online != null) {
            return groupOf(online).getPriority();
        }
        Optional<PunishmentRepository.PlayerRecord> record = repository.findPlayerByUuid(owner);
        return record.isPresent() ? record.get().getPriority() : punishment.getModeratorPriority();
    }

    private static UUID uuidOf(CommandSender sender) {
        return sender instanceof Player ? ((Player) sender).getUniqueId() : PunishmentService.CONSOLE_UUID;
    }
}
