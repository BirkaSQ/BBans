package ru.birkasq.bbans.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.HierarchyServiceImpl;
import ru.birkasq.bbans.service.PlayerService;
import ru.birkasq.bbans.service.PunishmentService;

import java.util.Optional;
import java.util.logging.Level;

/** Проверка бана при входе и обновление данных игрока (имя + приоритет иерархии) в БД. */
public class JoinListener implements Listener {

    private final BBansPlugin plugin;
    private final PunishmentService service;
    private final PlayerService players;
    private final HierarchyServiceImpl hierarchy;

    public JoinListener(BBansPlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getPunishmentService();
        this.players = plugin.getPlayerService();
        this.hierarchy = plugin.getHierarchyService();
    }

    /**
     * Срабатывает до входа игрока на сервер (асинхронно), поэтому забаненный игрок
     * не успевает зайти — в том числе если бан выдали, пока он был офлайн.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            Optional<Punishment> ban = service.getActiveBan(event.getUniqueId());
            if (ban.isPresent()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, service.getBanScreen(ban.get()));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Cannot check ban for " + event.getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        save(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        save(event.getPlayer());
    }

    private void save(Player player) {
        // Приоритет считаем в основном потоке (права), запись в БД — асинхронно
        final java.util.UUID uuid = player.getUniqueId();
        final String name = player.getName();
        final int priority = hierarchy.getPriority(player);
        plugin.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    players.touch(uuid, name, priority);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Cannot save player " + name, e);
                }
            }
        });
    }
}
