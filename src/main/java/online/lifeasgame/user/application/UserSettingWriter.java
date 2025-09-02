package online.lifeasgame.user.application;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.domain.UserSetting;
import online.lifeasgame.user.domain.repository.UserSettingRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSettingWriter {

    private final UserSettingRepository userSettingRepository;

    public Long ensureDefaultIfMissing(Long userId) {
        Optional<UserSetting> existing = userSettingRepository.findById(userId); // NEW
        if (existing.isPresent()) {
            return existing.get().getUserId();
        }

        UserSetting userSetting = userSettingRepository.save(UserSetting.ensureDefault(userId));
        return userSetting.getUserId();
    }
}
