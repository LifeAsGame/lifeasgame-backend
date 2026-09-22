package online.lifeasgame.user.domain.repository;

import java.util.Optional;
import online.lifeasgame.user.domain.UserSetting;

public interface UserSettingRepository {
    void insertIfAbsent(UserSetting userSetting);

    Optional<UserSetting> findById(Long userId);
}
