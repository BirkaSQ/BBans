package ru.birkasq.bbans.service;

import ru.birkasq.bbans.model.Punishment;

/** Результат выдачи предупреждения. */
public class WarnResult {

    private final Punishment warning;
    private final Punishment autoPunishment;
    private final long activeCount;

    public WarnResult(Punishment warning, Punishment autoPunishment, long activeCount) {
        this.warning = warning;
        this.autoPunishment = autoPunishment;
        this.activeCount = activeCount;
    }

    public Punishment getWarning() { return warning; }

    /** Автоматическое наказание или null, если лимит не достигнут. */
    public Punishment getAutoPunishment() { return autoPunishment; }

    public long getActiveCount() { return activeCount; }
}
