package online.lifeasgame.character.application;

import online.lifeasgame.character.application.query.PlayerTitleQuery;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerTitleError;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class CharacterApplicationBoundaryTest {

    private static final Long PLAYER_ID = 230L;
    private static final Long TITLE_ID = 231L;

    @Mock
    private PlayerTitleQuery playerTitleQuery;

    @Mock
    private PlayerTitleRepository playerTitleRepository;

    @Mock
    private PlayerReader playerReader;

    @Mock
    private PlayerTitleReader playerTitleReader;

    @Mock
    private PlayerTitleWriter playerTitleWriter;

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

    private PlayerTitleReader actualPlayerTitleReader;

    @BeforeEach
    void setUp() {
        actualPlayerTitleReader = new PlayerTitleReader(
                playerTitleQuery,
                playerTitleRepository
        );
    }

    @Test
    void acceptsOwnedTitle() {
        given(playerTitleRepository.existsByPlayerIdAndTitleId(
                PLAYER_ID,
                TITLE_ID
        )).willReturn(true);

        assertThatCode(() -> actualPlayerTitleReader.assertHasTitle(
                PLAYER_ID,
                TITLE_ID
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingTitle() {
        given(playerTitleRepository.existsByPlayerIdAndTitleId(
                PLAYER_ID,
                TITLE_ID
        )).willReturn(false);

        assertThatThrownBy(() -> actualPlayerTitleReader.assertHasTitle(
                PLAYER_ID,
                TITLE_ID
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(PlayerTitleError.PLAYER_TITLE_NOT_FOUND)
        );
    }

    @Test
    void locksPlayerBeforeChangingRepresentativeTitle() {
        Player player = player();
        given(playerReader.getByIdForUpdateOrThrow(PLAYER_ID))
                .willReturn(player);
        PlayerService service = new PlayerService(
                playerWriter,
                playerReader,
                playerTitleReader,
                playerExpGrantService,
                domainEventPublisher,
                currentPlayerAccessor
        );

        var result = service.changeRepresentativeTitle(PLAYER_ID, TITLE_ID);

        InOrder locks = inOrder(playerReader, playerTitleReader);
        locks.verify(playerReader).getByIdForUpdateOrThrow(PLAYER_ID);
        locks.verify(playerTitleReader).assertHasTitle(PLAYER_ID, TITLE_ID);
        assertThat(result.titleId()).isEqualTo(TITLE_ID);
        assertThat(player.getTitleId()).isEqualTo(TITLE_ID);
    }

    @Test
    void locksPlayerAndClearsRepresentativeTitleBeforeRevoke() {
        Player player = player();
        player.changeRepresentativeTitle(TITLE_ID);
        given(playerReader.getByIdForUpdateOrThrow(PLAYER_ID))
                .willReturn(player);
        PlayerTitleService service = new PlayerTitleService(
                playerTitleReader,
                playerTitleWriter,
                titleReader,
                playerReader,
                currentPlayerAccessor
        );

        var result = service.revokeTitle(PLAYER_ID, TITLE_ID);

        InOrder order = inOrder(
                playerReader,
                playerTitleReader,
                playerTitleWriter
        );
        order.verify(playerReader).getByIdForUpdateOrThrow(PLAYER_ID);
        order.verify(playerTitleReader).assertHasTitle(PLAYER_ID, TITLE_ID);
        order.verify(playerTitleWriter).revoke(PLAYER_ID, TITLE_ID);
        assertThat(player.getTitleId()).isNull();
        assertThat(result.playerId()).isEqualTo(PLAYER_ID);
        assertThat(result.titleId()).isEqualTo(TITLE_ID);
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
}
