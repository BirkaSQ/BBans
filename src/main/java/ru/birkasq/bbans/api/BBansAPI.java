package ru.birkasq.bbans.api;

import ru.birkasq.bbans.api.manager.HierarchyManager;
import ru.birkasq.bbans.api.manager.PlayerManager;
import ru.birkasq.bbans.api.manager.PunishmentManager;

/**
 * Публичный API BBans. Получение:
 * <pre>
 * RegisteredServiceProvider&lt;BBansAPI&gt; p = Bukkit.getServicesManager().getRegistration(BBansAPI.class);
 * BBansAPI api = p.getProvider();
 * </pre>
 * Стабильным считается только пакет {@code ru.birkasq.bbans.api}.
 */
public interface BBansAPI {

    int API_VERSION = 1;

    PunishmentManager getPunishmentManager();

    HierarchyManager getHierarchyManager();

    PlayerManager getPlayerManager();

    int getApiVersion();
}
