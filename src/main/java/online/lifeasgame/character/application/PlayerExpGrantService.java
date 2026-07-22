package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.service.LevelingPolicy;
import online.lifeasgame.core.event.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerExpGrantService {

    private final PlayerReader playerReader;
    private final LevelingPolicy levelingPolicy;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public Player.GainResult grantExp(Long playerId, long amount) {
        Player player = playerReader.getByIdForUpdateOrThrow(playerId);
        return grantExp(player, amount);
    }

    Player.GainResult grantExp(Player player, long amount) {
        Player.GainResult result = player.gainExp(amount, levelingPolicy);

        domainEventPublisher.publishAll(player.pullEvents());

        return result;
    }
}
