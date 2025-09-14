package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerAchievement;
import online.lifeasgame.character.domain.repository.PlayerAchievementRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PlayerAchievementWriter {

    private final PlayerAchievementRepository repository;

    public PlayerAchievement grantAchievement(PlayerAchievement playerAchievement) {
        return repository.save(playerAchievement);
    }
}
