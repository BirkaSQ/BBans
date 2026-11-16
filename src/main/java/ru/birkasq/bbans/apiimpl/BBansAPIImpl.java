package ru.birkasq.bbans.apiimpl;

import ru.birkasq.bbans.api.BBansAPI;
import ru.birkasq.bbans.api.manager.HierarchyManager;
import ru.birkasq.bbans.api.manager.PlayerManager;
import ru.birkasq.bbans.api.manager.PunishmentManager;

public class BBansAPIImpl implements BBansAPI {

    private final PunishmentManager punishmentManager;
    private final HierarchyManager hierarchyManager;
    private final PlayerManager playerManager;

    public BBansAPIImpl(PunishmentManager punishmentManager, HierarchyManager hierarchyManager,
                        PlayerManager playerManager) {
        this.punishmentManager = punishmentManager;
        this.hierarchyManager = hierarchyManager;
        this.playerManager = playerManager;
    }

    @Override
    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    @Override
    public HierarchyManager getHierarchyManager() {
        return hierarchyManager;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    @Override
    public int getApiVersion() {
        return API_VERSION;
    }
}
