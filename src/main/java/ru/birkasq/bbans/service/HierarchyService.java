package ru.birkasq.bbans.service;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.hierarchy.HierarchyGroup;
import ru.birkasq.bbans.model.Punishment;

import java.util.UUID;

/** Вся логика иерархии находится здесь и больше нигде. */
public interface HierarchyService {

    HierarchyGroup getGroup(CommandSender sender);

    HierarchyGroup getGroup(UUID uuid);

    int getPriority(CommandSender sender);

    int getPriority(UUID uuid);

    boolean canPunish(CommandSender sender, OfflinePlayer target);

    boolean canPunish(UUID moderatorUuid, UUID targetUuid);

    boolean canModifyPunishment(CommandSender sender, Punishment punishment);

    boolean canModifyPunishment(UUID moderatorUuid, Punishment punishment);

    boolean isOwner(CommandSender sender, Punishment punishment);

    boolean isOwner(UUID moderatorUuid, Punishment punishment);
}
