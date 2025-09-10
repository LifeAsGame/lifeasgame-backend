package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.CoreStatDelta;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.Player.GainResult;
import online.lifeasgame.character.domain.event.PlayerRegistered;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import online.lifeasgame.character.domain.service.LevelingPolicy;
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
    private final LevelingPolicy levelingPolicy;
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

    public Player changeHp(Long playerId, int delta) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));

        if (delta >= 0) {
            player.heal(delta);
        } else {
            try {
                player.damage(Math.negateExact(delta));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_HP);
            }
        }

        return player;
    }

    public Player changeHpCapacity(Long playerId, int delta) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));

        if (delta >= 0) {
            player.increaseMaxHp(delta);
        } else {
            try {
                player.decreaseMaxHp(Math.negateExact(delta));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_HP_CAPACITY);
            }
        }

        return player;
    }

    public Player changeMp(Long playerId, int delta) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));

        if (delta >= 0) {
            player.restoreMana(delta);
        } else {
            try {
                player.spendMana(Math.negateExact(delta));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_MP);
            }
        }

        return player;
    }

    public Player changeMpCapacity(Long playerId, int delta) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));

        if (delta >= 0) {
            player.increaseMaxMp(delta);
        } else {
            try {
                player.decreaseMaxMp(Math.negateExact(delta));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_MP_CAPACITY);
            }
        }

        return player;
    }

    public GainResult grantExp(Long playerId, long delta) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));
        return player.gainExp(delta, levelingPolicy);
    }

    public Player grantCoreStats(Long playerId, CoreStatDelta delta) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));
        player.gainCoreStats(delta);
        return player;
    }
}
