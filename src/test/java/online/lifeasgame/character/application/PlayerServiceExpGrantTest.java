package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.Player;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService EXP API")
class PlayerServiceExpGrantTest {

    @Mock
    private PlayerReader playerReader;

    @Mock
    private PlayerTitleOwnershipVerifier playerTitleOwnershipVerifier;

    @Mock
    private PlayerExpGrantService playerExpGrantService;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    private PlayerService service;

    @BeforeEach
    void setUp() {
        service = new PlayerService(
                playerReader,
                playerTitleOwnershipVerifier,
                playerExpGrantService,
                currentPlayerAccessor
        );
    }

    @Nested
    @DisplayName("기존 Admin EXP 지급 기능을 호출할 때")
    class GrantExperience {

        @Test
        @DisplayName("행동 서비스에 위임하고 기존 ExpGranted 계약으로 반환한다")
        void delegatesAndKeepsResultContract() {
            given(playerExpGrantService.grantExp(1L, 30L)).willReturn(new Player.GainResult(
                    30L, 30L, 0L, 1, 1, 0L, 30L, 30L, 70L, 100L, 0.3
            ));

            var result = service.grantExp(1L, 30L);

            assertThat(result.playerId()).isEqualTo(1L);
            assertThat(result.requestedExp()).isEqualTo(30L);
            assertThat(result.appliedExp()).isEqualTo(30L);
            assertThat(result.level()).isEqualTo(1);
            assertThat(result.totalExp()).isEqualTo(30L);
            verify(playerExpGrantService).grantExp(1L, 30L);
        }
    }
}
