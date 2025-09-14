package online.lifeasgame.character.infra;


import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.repository.AchievementRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AchievementRepositoryAdapter implements AchievementRepository {

    private final JpaAchievementRepository jpaRepository;

    @Override
    public Achievement save(Achievement achievement) {
        return jpaRepository.save(achievement);
    }
}
