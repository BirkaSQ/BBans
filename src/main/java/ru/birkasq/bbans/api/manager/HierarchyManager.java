package ru.birkasq.bbans.api.manager;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.api.model.HierarchyData;
import ru.birkasq.bbans.api.model.PunishmentData;

import java.util.UUID;

public interface HierarchyManager {

    HierarchyData getHierarchy(UUID playerUuid);

    HierarchyData getHierarchy(CommandSender sender);

    boolean canPunish(UUID moderatorUuid, UUID targetUuid);

    boolean canModifyPunishment(UUID moderatorUuid, PunishmentData punishment);
}
