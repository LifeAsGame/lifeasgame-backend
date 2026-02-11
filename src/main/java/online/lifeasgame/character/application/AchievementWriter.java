package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.repository.AchievementRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class AchievementWriter {

    private final AchievementRepository repository;

    public Achievement create(Achievement achievement) {
        return repository.save(achievement);
    }

    public void delete(Long achievementId) {
        repository.delete(achievementId);
    }
}
