package ru.birkasq.bbans.hierarchy;

import org.bukkit.configuration.ConfigurationSection;

/** Группа иерархии (из config.yml) и её правила. */
public class HierarchyGroup {

    private static final HierarchyGroup CONSOLE =
            new HierarchyGroup("console", "", Integer.MAX_VALUE, true, true, true, true, true, true, true, true, false);

    private final String name;
    private final String permission;
    private final int priority;

    private final boolean punishLower;
    private final boolean punishEqual;
    private final boolean punishHigher;
    private final boolean modifyOwn;
    private final boolean modifyLower;
    private final boolean modifyEqual;
    private final boolean modifyHigher;

    private final boolean console;
    private final boolean isDefault;

    private HierarchyGroup(String name, String permission, int priority,
                           boolean punishLower, boolean punishEqual, boolean punishHigher,
                           boolean modifyOwn, boolean modifyLower, boolean modifyEqual, boolean modifyHigher,
                           boolean console, boolean isDefault) {
        this.name = name;
        this.permission = permission;
        this.priority = priority;
        this.punishLower = punishLower;
        this.punishEqual = punishEqual;
        this.punishHigher = punishHigher;
        this.modifyOwn = modifyOwn;
        this.modifyLower = modifyLower;
        this.modifyEqual = modifyEqual;
        this.modifyHigher = modifyHigher;
        this.console = console;
        this.isDefault = isDefault;
    }

    public static HierarchyGroup console() {
        return CONSOLE;
    }

    /** Группа для игроков без hierarchy-permission. */
    public static HierarchyGroup defaults(int priority) {
        return new HierarchyGroup("default", "", priority, true, false, false, true, false, false, false, false, true);
    }

    public static HierarchyGroup fromConfig(String name, ConfigurationSection section) {
        String permission = section.getString("permission", "bbans.hierarchy." + name);
        int priority = section.getInt("priority", 0);
        ConfigurationSection r = section.getConfigurationSection("rules");
        return new HierarchyGroup(name, permission, priority,
                bool(r, "punish-lower", true),
                bool(r, "punish-equal", false),
                bool(r, "punish-higher", false),
                bool(r, "modify-own", true),
                bool(r, "modify-lower", false),
                bool(r, "modify-equal", false),
                bool(r, "modify-higher", false),
                false, false);
    }

    private static boolean bool(ConfigurationSection s, String key, boolean def) {
        return s == null ? def : s.getBoolean(key, def);
    }

    public String getName() { return name; }
    public String getPermission() { return permission; }
    public int getPriority() { return priority; }
    public boolean isPunishLower() { return punishLower; }
    public boolean isPunishEqual() { return punishEqual; }
    public boolean isPunishHigher() { return punishHigher; }
    public boolean isModifyOwn() { return modifyOwn; }
    public boolean isModifyLower() { return modifyLower; }
    public boolean isModifyEqual() { return modifyEqual; }
    public boolean isModifyHigher() { return modifyHigher; }
    public boolean isConsole() { return console; }
    public boolean isDefault() { return isDefault; }
}
