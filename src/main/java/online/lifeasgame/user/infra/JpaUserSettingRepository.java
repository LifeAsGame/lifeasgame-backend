package online.lifeasgame.user.infra;

import online.lifeasgame.user.domain.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserSettingRepository extends JpaRepository<UserSetting, Long> {
}
