package online.lifeasgame.user.infra;

import java.time.Instant;
import online.lifeasgame.user.domain.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaUserSettingRepository extends JpaRepository<UserSetting, Long> {

    // The primary key serializes concurrent initializers; existing preferences stay untouched.
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO user_settings (user_id, volume, ui_layout, flags, created_at, updated_at)
            VALUES (:userId, :volume, :uiLayout, :flags, :now, :now)
            ON DUPLICATE KEY UPDATE user_id = user_id
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") Long userId,
            @Param("volume") int volume,
            @Param("uiLayout") String uiLayout,
            @Param("flags") String flags,
            @Param("now") Instant now
    );
}
