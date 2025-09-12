package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.domain.UserSetting;
import online.lifeasgame.user.domain.repository.UserSettingRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSettingWriter {

    private final UserSettingRepository userSettingRepository;

    public Long ensureDefaultIfMissing(Long userId) {
        UserSetting userSetting = userSettingRepository.save(UserSetting.ensureDefault(userId));
        return userSetting.getUserId();
    }
}
