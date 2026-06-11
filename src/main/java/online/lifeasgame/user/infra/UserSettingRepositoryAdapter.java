package online.lifeasgame.user.infra;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.domain.UserSetting;
import online.lifeasgame.user.domain.repository.UserSettingRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSettingRepositoryAdapter implements UserSettingRepository {

    private final JpaUserSettingRepository jpaRepository;

    @Override
    public UserSetting save(UserSetting userSetting) {
        return jpaRepository.save(userSetting);
    }

    @Override
    public Optional<UserSetting> findById(Long userId) {
        return jpaRepository.findById(userId);
    }
}
