package ru.birkasq.bbans.api.manager;

import java.util.Optional;
import java.util.UUID;

public interface PlayerManager {

    Optional<UUID> findPlayerUuid(String name);

    Optional<String> findPlayerName(UUID uuid);

    boolean exists(String name);

    boolean isOnline(UUID uuid);
}
