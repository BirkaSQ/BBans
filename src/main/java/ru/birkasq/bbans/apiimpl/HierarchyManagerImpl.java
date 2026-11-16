package ru.birkasq.bbans.apiimpl;

import org.bukkit.command.CommandSender;
import ru.birkasq.bbans.api.manager.HierarchyManager;
import ru.birkasq.bbans.api.model.HierarchyData;
import ru.birkasq.bbans.api.model.PunishmentData;
import ru.birkasq.bbans.hierarchy.HierarchyGroup;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.HierarchyService;
import ru.birkasq.bbans.service.PunishmentService;

import java.util.Optional;
import java.util.UUID;

public class HierarchyManagerImpl implements HierarchyManager {

    private final HierarchyService hierarchy;
    private final PunishmentService punishments;

    public HierarchyManagerImpl(HierarchyService hierarchy, PunishmentService punishments) {
        this.hierarchy = hierarchy;
        this.punishments = punishments;
    }

    @Override
    public HierarchyData getHierarchy(UUID playerUuid) {
        return toData(hierarchy.getGroup(playerUuid));
    }

    @Override
    public HierarchyData getHierarchy(CommandSender sender) {
        return toData(hierarchy.getGroup(sender));
    }

    @Override
    public boolean canPunish(UUID moderatorUuid, UUID targetUuid) {
        return hierarchy.canPunish(moderatorUuid, targetUuid);
    }

    @Override
    public boolean canModifyPunishment(UUID moderatorUuid, PunishmentData punishment) {
        Optional<Punishment> internal = punishments.findById(punishment.getId());
        return internal.isPresent() && hierarchy.canModifyPunishment(moderatorUuid, internal.get());
    }

    private static HierarchyData toData(HierarchyGroup group) {
        return new HierarchyData(group.getName(), group.getPriority());
    }
}
