package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.application.command.UserSettingCommand;
import online.lifeasgame.user.application.result.UserSettingResult;
import online.lifeasgame.user.domain.UserSetting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingWriter userSettingWriter;
    private final UserSettingReader userSettingReader;

    @Transactional
    public Long ensureDefaultIfMissing(Long userId) {
        return userSettingWriter.ensureDefaultIfMissing(userId);
    }

    public UserSettingResult.Settings getSettings(Long userId) {
        UserSetting userSetting = userSettingReader.findByIdOrElseThrow(userId);
        return UserSettingResult.Settings.from(userSetting);
    }

    @Transactional
    public UserSettingResult.Settings updateSettings(Long userId, UserSettingCommand.UpdateSettings command) {
        UserSetting userSetting = userSettingReader.findByIdOrElseThrow(userId);
        userSetting.apply(
                command.volume(),
                command.uiLayoutJson(),
                command.flagsJson()
        );

        return UserSettingResult.Settings.from(userSetting);
    }
}
