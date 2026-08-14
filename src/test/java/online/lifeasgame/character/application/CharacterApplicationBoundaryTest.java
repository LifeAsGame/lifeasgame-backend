package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerTitleError;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Character application boundary")
class CharacterApplicationBoundaryTest {

    private static final Long PLAYER_ID = 230L;
    private static final Long TITLE_ID = 231L;

    @Mock
    private PlayerTitleRepository playerTitleRepository;

    @Mock
    private PlayerReader playerReader;

    @Mock
    private PlayerTitleReader playerTitleReader;

    @Mock
    private PlayerTitleRegistrar playerTitleRegistrar;

    @Mock
    private TitleReader titleReader;

    @Mock
    private PlayerWriter playerWriter;

    @Mock
    private PlayerExpGrantService playerExpGrantService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private PlayerAchievementReader playerAchievementReader;

    @Mock
    private PlayerAchievementWriter playerAchievementWriter;

    @Mock
    private AchievementReader achievementReader;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    private PlayerTitleOwnershipVerifier playerTitleOwnershipVerifier;
    private PlayerTitleRevoker playerTitleRevoker;

    @BeforeEach
    void setUp() {
        playerTitleOwnershipVerifier = new PlayerTitleOwnershipVerifier(playerTitleRepository);
        playerTitleRevoker = new PlayerTitleRevoker(playerTitleRepository);
    }

    @Nested
    @DisplayName("대표 칭호를 선택할 때")
    class ChangeRepresentativeTitle {

        @Test
        @DisplayName("Player를 잠근 뒤 보유한 칭호를 대표 칭호로 변경한다")
        void changesToOwnedTitleAfterLock() {
            Player player = player();
            given(playerReader.getByIdForUpdateOrThrow(PLAYER_ID)).willReturn(player);
            given(playerTitleRepository.existsByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID))
                    .willReturn(true);
            PlayerService service = playerService();

            var result = service.changeRepresentativeTitle(PLAYER_ID, TITLE_ID);

            InOrder order = inOrder(playerReader, playerTitleRepository);
            order.verify(playerReader).getByIdForUpdateOrThrow(PLAYER_ID);
            order.verify(playerTitleRepository).existsByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID);
            assertThat(result.titleId()).isEqualTo(TITLE_ID);
            assertThat(player.getTitleId()).isEqualTo(TITLE_ID);
        }

        @Test
        @DisplayName("보유하지 않은 칭호는 거부하고 기존 대표 칭호를 유지한다")
        void rejectsUnownedTitleWithoutChangingState() {
            Player player = player();
            player.changeRepresentativeTitle(232L);
            given(playerReader.getByIdForUpdateOrThrow(PLAYER_ID)).willReturn(player);
            given(playerTitleRepository.existsByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID))
                    .willReturn(false);

            assertPlayerTitleNotFound(() -> playerService()
                    .changeRepresentativeTitle(PLAYER_ID, TITLE_ID));

            InOrder order = inOrder(playerReader, playerTitleRepository);
            order.verify(playerReader).getByIdForUpdateOrThrow(PLAYER_ID);
            order.verify(playerTitleRepository).existsByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID);
            assertThat(player.getTitleId()).isEqualTo(232L);
        }
    }

    @Nested
    @DisplayName("보유 칭호를 회수할 때")
    class RevokeTitle {

        @Test
        @DisplayName("Player를 잠그고 소유권을 확인한 뒤 대표 칭호를 지우고 소유권을 회수한다")
        void clearsCurrentRepresentativeBeforeRevokingOwnership() {
            Player player = player();
            player.changeRepresentativeTitle(TITLE_ID);
            given(playerReader.getByIdForUpdateOrThrow(PLAYER_ID)).willReturn(player);
            given(playerTitleRepository.existsByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID))
                    .willReturn(true);
            given(playerTitleRepository.deleteByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID))
                    .willAnswer(invocation -> {
                        assertThat(player.getTitleId()).isNull();
                        return 1L;
                    });

            var result = playerTitleService().revokeTitle(PLAYER_ID, TITLE_ID);

            InOrder order = inOrder(playerReader, playerTitleRepository);
            order.verify(playerReader).getByIdForUpdateOrThrow(PLAYER_ID);
            order.verify(playerTitleRepository).existsByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID);
            order.verify(playerTitleRepository).deleteByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID);
            assertThat(result.playerId()).isEqualTo(PLAYER_ID);
            assertThat(result.titleId()).isEqualTo(TITLE_ID);
        }

        @Test
        @DisplayName("소유권이 없으면 대표 칭호를 유지하고 PLAYER_TITLE_NOT_FOUND로 거부한다")
        void rejectsMissingOwnershipWithoutChangingState() {
            Player player = player();
            player.changeRepresentativeTitle(TITLE_ID);
            given(playerReader.getByIdForUpdateOrThrow(PLAYER_ID)).willReturn(player);
            given(playerTitleRepository.existsByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID))
                    .willReturn(false);

            assertPlayerTitleNotFound(() -> playerTitleService().revokeTitle(PLAYER_ID, TITLE_ID));

            assertThat(player.getTitleId()).isEqualTo(TITLE_ID);
            verify(playerTitleRepository, never()).deleteByPlayerIdAndTitleId(PLAYER_ID, TITLE_ID);

            assertPlayerTitleNotFound(() -> playerTitleRevoker.revoke(PLAYER_ID, TITLE_ID));
        }
    }

    @Test
    void returnsPlayerAndAchievementIdsOnRevoke() {
        PlayerAchievementService service = new PlayerAchievementService(
                playerAchievementReader,
                playerAchievementWriter,
                achievementReader,
                playerReader,
                currentPlayerAccessor
        );

        var result = service.revokeAchievement(PLAYER_ID, 232L);

        assertThat(result.playerId()).isEqualTo(PLAYER_ID);
        assertThat(result.achievementId()).isEqualTo(232L);
    }

    private Player player() {
        return Player.linkStart(1L, Name.of("player"), GenderType.MALE);
    }

    private PlayerService playerService() {
        return new PlayerService(
                playerWriter,
                playerReader,
                playerTitleOwnershipVerifier,
                playerExpGrantService,
                domainEventPublisher,
                currentPlayerAccessor
        );
    }

    private PlayerTitleService playerTitleService() {
        return new PlayerTitleService(
                playerTitleReader,
                playerTitleOwnershipVerifier,
                playerTitleRegistrar,
                playerTitleRevoker,
                titleReader,
                playerReader,
                currentPlayerAccessor
        );
    }

    private void assertPlayerTitleNotFound(ThrowingAction action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(PlayerTitleError.PLAYER_TITLE_NOT_FOUND)
                );
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run();
    }
}
