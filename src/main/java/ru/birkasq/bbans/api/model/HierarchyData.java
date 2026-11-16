package ru.birkasq.bbans.api.model;

/** Положение игрока в иерархии. */
public class HierarchyData {

    private final String group;
    private final int priority;

    public HierarchyData(String group, int priority) {
        this.group = group;
        this.priority = priority;
    }

    /** Имя группы из config.yml ("default" — если группы нет, "console" — для консоли). */
    public String getGroup() {
        return group;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "HierarchyData{group=" + group + ", priority=" + priority + "}";
    }
}
