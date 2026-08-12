package online.lifeasgame.home.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.home.application.result.HomeResult;
import online.lifeasgame.lifelog.application.internal.LifeLogActivityReadApi;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.quest.application.internal.QuestProgressReadApi;
import online.lifeasgame.role.application.internal.RoleDisplayReadApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Home world summary composition")
class HomeQueryServiceTest {

    private static final Long PLAYER_ID = 258L;
    private static final Instant NOW =
            Instant.parse("2026-08-12T00:00:00Z");
    private static final Instant WINDOW_START =
            Instant.parse("2026-07-13T00:00:00Z");

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @Mock
    private LifeLogActivityReadApi lifeLogActivityReadApi;

    @Mock
    private QuestProgressReadApi questProgressReadApi;

    @Mock
    private RoleDisplayReadApi roleDisplayReadApi;

    private HomeQueryService service;

    @BeforeEach
    void setUp() {
        service = new HomeQueryService(
                currentPlayerAccessor,
                Clock.fixed(NOW, ZoneOffset.UTC),
                lifeLogActivityReadApi,
                questProgressReadApi,
                roleDisplayReadApi
        );
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willReturn(PLAYER_ID);
    }

    @Nested
    @DisplayName("Home을 조회할 때")
    class ReadHome {

        @Test
        @DisplayName("기록이 없으면 동일 Clock snapshot의 빈 summary를 반환한다")
        void returnsEmptyWorldSummary() {
            givenEmptyProviders();

            HomeResult.Summary result = service.home();

            assertThat(result.generatedAt()).isEqualTo(NOW);
            assertThat(result.recentJournal()).isEmpty();
            assertThat(result.journey().currentQuests()).isEmpty();
            assertThat(result.journey().selectedRoutes()).isEmpty();
            assertThat(result.roleActivity30d()).isEqualTo(
                    new HomeResult.RoleActivity(
                            WINDOW_START,
                            NOW,
                            0,
                            0,
                            0,
                            List.of()
                    )
            );
            verify(lifeLogActivityReadApi).recentJournal(
                    PLAYER_ID,
                    HomeQueryService.RECENT_JOURNAL_LIMIT
            );
            verify(questProgressReadApi).currentQuests(
                    PLAYER_ID,
                    HomeQueryService.CURRENT_QUEST_LIMIT
            );
            verify(questProgressReadApi).selectedRoutes(
                    PLAYER_ID,
                    HomeQueryService.SELECTED_ROUTE_LIMIT
            );
        }

        @Test
        @DisplayName("assigned count를 분모로 role share를 계산하고 missing 이름은 null로 둔다")
        void calculatesRoleDistribution() {
            given(lifeLogActivityReadApi.roleActivity(
                    PLAYER_ID,
                    WINDOW_START,
                    NOW
            )).willReturn(new LifeLogActivityReadApi.RoleActivity(
                    5,
                    4,
                    1,
                    List.of(
                            new LifeLogActivityReadApi.RoleCount(10L, 3),
                            new LifeLogActivityReadApi.RoleCount(20L, 1)
                    )
            ));
            given(roleDisplayReadApi.findNames(
                    PLAYER_ID,
                    List.of(10L, 20L)
            )).willReturn(Map.of(10L, "개발자"));
            given(lifeLogActivityReadApi.recentJournal(
                    PLAYER_ID,
                    HomeQueryService.RECENT_JOURNAL_LIMIT
            )).willReturn(List.of());
            given(questProgressReadApi.currentQuests(
                    PLAYER_ID,
                    HomeQueryService.CURRENT_QUEST_LIMIT
            )).willReturn(List.of());
            given(questProgressReadApi.selectedRoutes(
                    PLAYER_ID,
                    HomeQueryService.SELECTED_ROUTE_LIMIT
            ))
                    .willReturn(List.of());

            HomeResult.RoleActivity result = service.home().roleActivity30d();

            assertThat(result.windowStart()).isEqualTo(WINDOW_START);
            assertThat(result.windowEnd()).isEqualTo(NOW);
            assertThat(result.roles()).containsExactly(
                    new HomeResult.RoleBucket(10L, "개발자", 3, 0.75),
                    new HomeResult.RoleBucket(20L, null, 1, 0.25)
            );
        }

        @Test
        @DisplayName("provider invariant failure를 partial success로 숨기지 않는다")
        void propagatesProviderInvariantFailure() {
            DomainException failure = new DomainException(
                    LifeLogError.LIFE_LOG_SOURCE_UNAVAILABLE
            );
            given(lifeLogActivityReadApi.roleActivity(
                    PLAYER_ID,
                    WINDOW_START,
                    NOW
            )).willReturn(new LifeLogActivityReadApi.RoleActivity(
                    0, 0, 0, List.of()
            ));
            given(roleDisplayReadApi.findNames(PLAYER_ID, List.of()))
                    .willReturn(Map.of());
            given(lifeLogActivityReadApi.recentJournal(
                    PLAYER_ID,
                    HomeQueryService.RECENT_JOURNAL_LIMIT
            )).willThrow(failure);

            assertThatThrownBy(service::home).isSameAs(failure);
        }
    }

    private void givenEmptyProviders() {
        given(lifeLogActivityReadApi.roleActivity(
                PLAYER_ID,
                WINDOW_START,
                NOW
        )).willReturn(new LifeLogActivityReadApi.RoleActivity(
                0, 0, 0, List.of()
        ));
        given(roleDisplayReadApi.findNames(PLAYER_ID, List.of()))
                .willReturn(Map.of());
        given(lifeLogActivityReadApi.recentJournal(
                PLAYER_ID,
                HomeQueryService.RECENT_JOURNAL_LIMIT
        )).willReturn(List.of());
        given(questProgressReadApi.currentQuests(
                PLAYER_ID,
                HomeQueryService.CURRENT_QUEST_LIMIT
        )).willReturn(List.of());
        given(questProgressReadApi.selectedRoutes(
                PLAYER_ID,
                HomeQueryService.SELECTED_ROUTE_LIMIT
        ))
                .willReturn(List.of());
    }
}
