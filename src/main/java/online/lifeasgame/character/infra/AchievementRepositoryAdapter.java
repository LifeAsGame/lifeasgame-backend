package online.lifeasgame.character.infra;


import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import online.lifeasgame.character.domain.repository.AchievementRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AchievementRepositoryAdapter implements AchievementRepository {

    private final JpaAchievementRepository jpaRepository;

    @Override
    public Achievement save(Achievement achievement) {
        return jpaRepository.save(achievement);
    }

    @Override
    public List<Achievement> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Achievement> findByCategoryIn(List<AchievementCategory> achievementCategories) {
        return jpaRepository.findByCategoryIn(achievementCategories);
    }

    @Override
    public Optional<Achievement> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public void delete(Long achievementId) {
        jpaRepository.deleteById(achievementId);
    }
}
