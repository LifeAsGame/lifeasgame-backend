package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerAchievement;
import online.lifeasgame.character.domain.repository.PlayerAchievementRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlayerAchievementAdapter implements PlayerAchievementRepository {

    private final JpaPlayerAchievementRepository jpaRepository;

    @Override
    public PlayerAchievement save(PlayerAchievement playerAchievement) {
        return jpaRepository.save(playerAchievement);
    }
}
