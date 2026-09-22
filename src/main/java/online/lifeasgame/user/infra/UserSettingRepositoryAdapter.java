package online.lifeasgame.user.infra;

import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.domain.UserSetting;
import online.lifeasgame.user.domain.repository.UserSettingRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSettingRepositoryAdapter implements UserSettingRepository {

    private final JpaUserSettingRepository jpaRepository;
    private final Clock clock;

    @Override
    public void insertIfAbsent(UserSetting userSetting) {
        jpaRepository.insertIfAbsent(
                userSetting.getUserId(),
                userSetting.getVolume().getValue(),
                userSetting.getUiLayoutJson(),
                userSetting.getFlagsJson(),
                clock.instant()
        );
    }

    @Override
    public Optional<UserSetting> findById(Long userId) {
        return jpaRepository.findById(userId);
    }
}
