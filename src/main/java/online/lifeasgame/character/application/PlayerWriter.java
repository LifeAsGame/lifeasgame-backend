package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.event.PlayerRegistered;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlayerWriter {

    private final PlayerRepository playerRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long register(Player player) {
        if (playerRepository.existsByUserId(player.getUserId())) {
            throw new DomainException(PlayerError.PLAYER_ALREADY_EXISTS);
        }

        Player savedPlayer = playerRepository.save(player);

        domainEventPublisher.publish(
                PlayerRegistered.of(savedPlayer.getId())
        );

        return savedPlayer.getId();
    }
}
