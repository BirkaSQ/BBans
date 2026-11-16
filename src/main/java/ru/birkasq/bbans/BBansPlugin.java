package ru.birkasq.bbans;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.birkasq.bbans.api.BBansAPI;
import ru.birkasq.bbans.apiimpl.BBansAPIImpl;
import ru.birkasq.bbans.apiimpl.HierarchyManagerImpl;
import ru.birkasq.bbans.apiimpl.PlayerManagerImpl;
import ru.birkasq.bbans.apiimpl.PunishmentManagerImpl;
import ru.birkasq.bbans.command.BBansCommand;
import ru.birkasq.bbans.command.BanCommand;
import ru.birkasq.bbans.command.BanListCommand;
import ru.birkasq.bbans.command.BaseCommand;
import ru.birkasq.bbans.command.CheckCommand;
import ru.birkasq.bbans.command.KickCommand;
import ru.birkasq.bbans.command.MuteCommand;
import ru.birkasq.bbans.command.MuteListCommand;
import ru.birkasq.bbans.command.UnbanCommand;
import ru.birkasq.bbans.command.UnmuteCommand;
import ru.birkasq.bbans.command.UnwarnCommand;
import ru.birkasq.bbans.command.WarnCommand;
import ru.birkasq.bbans.command.WarningsCommand;
import ru.birkasq.bbans.listener.ChatListener;
import ru.birkasq.bbans.listener.JoinListener;
import ru.birkasq.bbans.logger.PunishmentLogger;
import ru.birkasq.bbans.repository.DatabaseManager;
import ru.birkasq.bbans.repository.PunishmentRepository;
import ru.birkasq.bbans.service.HierarchyServiceImpl;
import ru.birkasq.bbans.service.PlayerService;
import ru.birkasq.bbans.service.PunishmentService;
import ru.birkasq.bbans.util.MessageFormatter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class BBansPlugin extends JavaPlugin {

    private ExecutorService executor;
    private MessageFormatter messages;
    private DatabaseManager database;
    private PunishmentRepository repository;
    private PlayerService playerService;
    private HierarchyServiceImpl hierarchyService;
    private PunishmentLogger punishmentLogger;
    private PunishmentService punishmentService;
    private BBansAPI api;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        executor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "BBans-Worker");
                thread.setDaemon(true);
                return thread;
            }
        });

        messages = new MessageFormatter(this);
        messages.load();

        database = new DatabaseManager(this);
        try {
            database.connect();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Cannot connect to the database. BBans will be disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        repository = new PunishmentRepository(database);
        playerService = new PlayerService(repository);
        hierarchyService = new HierarchyServiceImpl(this, repository);
        punishmentLogger = new PunishmentLogger(this);
        punishmentService = new PunishmentService(this, repository, hierarchyService, playerService,
                messages, punishmentLogger);

        registerCommands();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        registerApi();
        startExpirationTask();

        getLogger().info("BBans " + getDescription().getVersion() + " enabled (API v" + BBansAPI.API_VERSION + ").");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (database != null) {
            database.close();
        }
    }

    // ------------------------------------------------------------------ setup

    private void registerCommands() {
        register("ban", new BanCommand(this));
        register("unban", new UnbanCommand(this));
        register("mute", new MuteCommand(this));
        register("unmute", new UnmuteCommand(this));
        register("warn", new WarnCommand(this));
        register("unwarn", new UnwarnCommand(this));
        register("warnings", new WarningsCommand(this));
        register("check", new CheckCommand(this));
        register("banlist", new BanListCommand(this));
        register("mutelist", new MuteListCommand(this));
        register("kick", new KickCommand(this));
        register("bbans", new BBansCommand(this));
    }

    private void register(String name, BaseCommand handler) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command /" + name + " is missing in plugin.yml");
            return;
        }
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    private void registerApi() {
        api = new BBansAPIImpl(
                new PunishmentManagerImpl(punishmentService, playerService, executor),
                new HierarchyManagerImpl(hierarchyService, punishmentService),
                new PlayerManagerImpl(playerService));
        getServer().getServicesManager().register(BBansAPI.class, api, this, ServicePriority.Normal);
    }

    private void startExpirationTask() {
        long periodTicks = Math.max(5L, getConfig().getLong("expiry-check-seconds", 30L)) * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                try {
                    punishmentService.processExpired();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error while processing expired punishments", e);
                }
            }
        }, 20L * 10L, periodTicks);
    }

    /** Перезагрузка config.yml, messages.yml и иерархии. */
    public void reload() {
        reloadConfig();
        messages.load();
        hierarchyService.reload();
    }

    /** Выполняет задачу в рабочем потоке (БД не блокирует основной поток сервера). */
    public void runAsync(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ignored) {
            // плагин выключается
        }
    }

    // ------------------------------------------------------------------ getters

    public MessageFormatter getMessages() { return messages; }

    public PunishmentService getPunishmentService() { return punishmentService; }

    public PlayerService getPlayerService() { return playerService; }

    public HierarchyServiceImpl getHierarchyService() { return hierarchyService; }

    public PunishmentRepository getRepository() { return repository; }

    public PunishmentLogger getPunishmentLogger() { return punishmentLogger; }

    public BBansAPI getApi() { return api; }
}
