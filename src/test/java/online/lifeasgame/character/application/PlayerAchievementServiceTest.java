package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import online.lifeasgame.character.domain.PlayerAchievement;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Player Achievement service")
class PlayerAchievementServiceTest {

    private static final Long PLAYER_ID = 260L;
    private static final Long ACHIEVEMENT_ID = 2601L;

    @Mock
    private PlayerAchievementReader playerAchievementReader;

    @Mock
    private PlayerAchievementWriter playerAchievementWriter;

    @Mock
    private AchievementReader achievementReader;

    @Mock
    private PlayerReader playerReader;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    private PlayerAchievementService service;

    @BeforeEach
    void setUp() {
        service = new PlayerAchievementService(
                playerAchievementReader,
                playerAchievementWriter,
                achievementReader,
                playerReader,
                currentPlayerAccessor
        );
    }

    @Nested
    @DisplayName("업적을 지급할 때")
    class GrantAchievement {

        @Test
        @DisplayName("Player와 Achievement 확인 후 기존 획득 의미를 보존한다")
        void grantsExistingAchievement() {
            Achievement achievement = Achievement.create(
                    "HOME_FIRST",
                    "첫 Home",
                    AchievementCategory.STORY,
                    "Home feed 업적"
            );
            given(achievementReader.getByIdOrThrow(ACHIEVEMENT_ID))
                    .willReturn(achievement);
            given(playerAchievementWriter.create(any(PlayerAchievement.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            var result = service.grantAchievement(
                    PLAYER_ID,
                    ACHIEVEMENT_ID
            );

            assertThat(result.playerId()).isEqualTo(PLAYER_ID);
            assertThat(result.achievementId()).isEqualTo(ACHIEVEMENT_ID);
            assertThat(result.code()).isEqualTo("HOME_FIRST");
            assertThat(result.name()).isEqualTo("첫 Home");
            assertThat(result.category()).isEqualTo("STORY");
            assertThat(result.acquiredAt()).isNotNull();
            verify(playerReader).assertExistsById(PLAYER_ID);
            verify(achievementReader).getByIdOrThrow(ACHIEVEMENT_ID);
        }
    }

    @Nested
    @DisplayName("업적을 회수할 때")
    class RevokeAchievement {

        @Test
        @DisplayName("Player와 Achievement 확인 후 기존 ownership key로 삭제한다")
        void revokesExistingAchievement() {
            var result = service.revokeAchievement(
                    PLAYER_ID,
                    ACHIEVEMENT_ID
            );

            assertThat(result.playerId()).isEqualTo(PLAYER_ID);
            assertThat(result.achievementId()).isEqualTo(ACHIEVEMENT_ID);
            verify(playerReader).assertExistsById(PLAYER_ID);
            verify(achievementReader).assertExistsById(ACHIEVEMENT_ID);
            verify(playerAchievementWriter).revoke(
                    PLAYER_ID,
                    ACHIEVEMENT_ID
            );
        }
    }
}
