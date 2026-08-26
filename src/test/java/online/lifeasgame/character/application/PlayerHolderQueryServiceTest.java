package online.lifeasgame.character.application;

import online.lifeasgame.character.application.view.PlayerAchievementView;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.AchievementCategory;
import online.lifeasgame.character.domain.CertificationCategory;
import online.lifeasgame.character.domain.HobbyCategory;
import online.lifeasgame.character.domain.PlayerHobbyStatus;
import online.lifeasgame.character.domain.TitleCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Player holder query boundary")
class PlayerHolderQueryServiceTest {

    private static final long PLAYER_ID = 314L;

    @Mock
    private PlayerAchievementReader achievementReader;

    @Mock
    private PlayerCertificationReader certificationReader;

    @Mock
    private PlayerHobbyReader hobbyReader;

    @Mock
    private PlayerTitleReader titleReader;

    @Mock
    private PlayerAchievementView achievement;

    @Mock
    private PlayerCertificationView certification;

    @Mock
    private PlayerHobbyView hobby;

    @Mock
    private PlayerTitleView title;

    private PlayerHolderQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new PlayerHolderQueryService(
                achievementReader,
                certificationReader,
                hobbyReader,
                titleReader
        );
    }

    @Test
    @DisplayName("explicit playerId로 네 holder projection을 조회한다")
    void readsAllHoldersForExplicitPlayer() {
        given(achievementReader.getViewsByPlayerId(PLAYER_ID))
                .willReturn(List.of(achievement));
        given(certificationReader.getViewByPlayerId(PLAYER_ID))
                .willReturn(List.of(certification));
        given(hobbyReader.getViewsByPlayerId(PLAYER_ID))
                .willReturn(List.of(hobby));
        given(titleReader.getViewsByPlayerId(PLAYER_ID))
                .willReturn(List.of(title));
        given(achievement.getCategory()).willReturn(AchievementCategory.STORY);
        given(certification.getCategory()).willReturn(CertificationCategory.PROGRAMMING);
        given(hobby.getCategory()).willReturn(HobbyCategory.SPORTS);
        given(hobby.getStatus()).willReturn(PlayerHobbyStatus.ACTIVE);
        given(title.getCategory()).willReturn(TitleCategory.ACHIEVEMENT);

        assertThat(queryService.getAchievementInfos(PLAYER_ID))
                .singleElement()
                .extracting(info -> info.category())
                .isEqualTo("STORY");
        assertThat(queryService.getCertificationInfos(PLAYER_ID))
                .singleElement()
                .extracting(info -> info.category())
                .isEqualTo("PROGRAMMING");
        assertThat(queryService.getHobbyInfos(PLAYER_ID))
                .singleElement()
                .extracting(info -> info.status())
                .isEqualTo("ACTIVE");
        assertThat(queryService.getTitleInfos(PLAYER_ID))
                .singleElement()
                .extracting(info -> info.category())
                .isEqualTo("ACHIEVEMENT");

        verify(achievementReader).getViewsByPlayerId(PLAYER_ID);
        verify(certificationReader).getViewByPlayerId(PLAYER_ID);
        verify(hobbyReader).getViewsByPlayerId(PLAYER_ID);
        verify(titleReader).getViewsByPlayerId(PLAYER_ID);
    }
}
