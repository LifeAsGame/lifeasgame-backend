package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.user.domain.UserSetting;
import online.lifeasgame.user.domain.error.UserSettingError;
import online.lifeasgame.user.domain.repository.UserSettingRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSettingReader {

    private final UserSettingRepository userSettingRepository;


    public UserSetting findByIdOrElseThrow(Long userId) {
        return userSettingRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserSettingError.USER_SETTING_NOT_FOUND));
    }
}
