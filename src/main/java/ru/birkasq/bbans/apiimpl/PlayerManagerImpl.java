package ru.birkasq.bbans.apiimpl;

import ru.birkasq.bbans.api.manager.PlayerManager;
import ru.birkasq.bbans.service.PlayerService;

import java.util.Optional;
import java.util.UUID;

public class PlayerManagerImpl implements PlayerManager {

    private final PlayerService players;

    public PlayerManagerImpl(PlayerService players) {
        this.players = players;
    }

    @Override
    public Optional<UUID> findPlayerUuid(String name) {
        return players.findUuid(name);
    }

    @Override
    public Optional<String> findPlayerName(UUID uuid) {
        return players.findName(uuid);
    }

    @Override
    public boolean exists(String name) {
        return players.resolve(name).isPresent();
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return players.isOnline(uuid);
    }
}
