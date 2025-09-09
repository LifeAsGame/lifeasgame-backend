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
@Transactional(propagation = Propagation.MANDATORY)
public class PlayerWriter {

    private final PlayerRepository playerRepository;
    private final DomainEventPublisher domainEventPublisher;

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

    public Player changeHp(Long playerId, int hp) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));

        if (hp == 0) {
            return player;
        }

        if (hp >= 0) {
            player.heal(hp);
        } else {
            try {
                player.damage(Math.negateExact(hp));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_HP);
            }
        }

        return player;
    }

    public Player changeHpCapacity(Long playerId, int hpCapacity) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));

        if (hpCapacity == 0) {
            return player;
        }

        if (hpCapacity > 0) {
            player.increaseMaxHp(hpCapacity);
        } else {
            try {
                player.decreaseMaxHp(Math.negateExact(hpCapacity));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_HP_CAPACITY);
            }
        }

        return player;
    }
}
