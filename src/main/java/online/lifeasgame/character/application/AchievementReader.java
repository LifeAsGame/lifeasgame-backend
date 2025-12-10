package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import online.lifeasgame.character.domain.error.AchievementError;
import online.lifeasgame.character.domain.repository.AchievementRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class AchievementReader {

    private final AchievementRepository repository;

    public List<Achievement> getByCategories(List<AchievementCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return repository.findAll();
        }

        return repository.findByCategoryIn(categories);
    }

    public Achievement getByIdOrThrow(Long achievementId) {
        return repository.findById(achievementId)
                .orElseThrow(() -> new DomainException(AchievementError.ACHIEVEMENT_NOT_FOUND));
    }
}
