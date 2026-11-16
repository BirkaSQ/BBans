package ru.birkasq.bbans.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.Punishment;
import ru.birkasq.bbans.service.PunishmentService;
import ru.birkasq.bbans.util.MessageFormatter;

import java.util.Optional;
import java.util.logging.Level;

/** Блокирует чат (и выбранные команды) для замьюченных игроков. */
public class ChatListener implements Listener {

    private final BBansPlugin plugin;
    private final PunishmentService service;
    private final MessageFormatter messages;

    public ChatListener(BBansPlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getPunishmentService();
        this.messages = plugin.getMessages();
    }

    // AsyncPlayerChatEvent уже выполняется вне основного потока — блокирующий запрос к БД допустим
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        try {
            Optional<Punishment> mute = service.getActiveMute(player.getUniqueId());
            if (mute.isPresent()) {
                event.setCancelled(true);
                send(player, "mute.chat-blocked", mute.get());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Cannot check mute for " + player.getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.length() < 2) {
            return;
        }
        String label = message.substring(1).split(" ")[0].toLowerCase();
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        boolean blocked = false;
        for (String command : plugin.getConfig().getStringList("mute.blocked-commands")) {
            if (command.equalsIgnoreCase(label)) {
                blocked = true;
                break;
            }
        }
        if (!blocked) {
            return;
        }
        Player player = event.getPlayer();
        try {
            // Запрос выполняется только для команд из списка, поэтому нагрузка небольшая
            Optional<Punishment> mute = service.getActiveMute(player.getUniqueId());
            if (mute.isPresent()) {
                event.setCancelled(true);
                send(player, "mute.command-blocked", mute.get());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Cannot check mute for " + player.getName(), e);
        }
    }

    private void send(Player player, String path, Punishment mute) {
        messages.send(player, path, messages.describe(mute, System.currentTimeMillis()));
    }
}
