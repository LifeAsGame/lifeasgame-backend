package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingWriter userSettingWriter;

    @Transactional
    public Long ensureDefaultIfMissing(Long userId) {
        return userSettingWriter.ensureDefaultIfMissing(userId);
    }
}
