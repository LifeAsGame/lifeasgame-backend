package online.lifeasgame.character.application;

import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.command.TitleCommand;
import online.lifeasgame.character.domain.error.PlayerTitleError;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Testcontainers
class CharacterTransactionBoundaryIntegrationTest {

    private static final AtomicLong SEQUENCE = new AtomicLong(230_000L);

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            "mysql:8.0.39"
    ).withDatabaseName("lifeasgame_character_230")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
    }

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerTitleService playerTitleService;

    @Autowired
    private TitleService titleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private PlayerTitleRepository playerTitleRepository;

    @Test
    void persistsRenameAndKeepsNameOnValidationFailure() {
        Long playerId = createPlayer();

        playerService.rename(
                playerId,
                new PlayerCommand.Renamed("renamed-player")
        );

        assertThat(playerName(playerId)).isEqualTo("renamed-player");
        assertThatThrownBy(() -> playerService.rename(
                playerId,
                new PlayerCommand.Renamed(" ")
        )).isInstanceOf(IllegalArgumentException.class);
        assertThat(playerName(playerId)).isEqualTo("renamed-player");
    }

    @Test
    void revokesCurrentRepresentativeTitleAtomically() {
        Fixture fixture = fixture(false);

        playerTitleService.revokeTitle(fixture.playerId(), fixture.titleA());

        assertThat(playerTitleId(fixture.playerId())).isNull();
        assertThat(hasTitle(fixture.playerId(), fixture.titleA())).isFalse();
    }

    @Test
    void keepsDifferentRepresentativeTitleOnRevoke() {
        Fixture fixture = fixture(true);
        playerService.changeRepresentativeTitle(
                fixture.playerId(),
                fixture.titleB()
        );

        playerTitleService.revokeTitle(fixture.playerId(), fixture.titleA());

        assertThat(playerTitleId(fixture.playerId()))
                .isEqualTo(fixture.titleB());
        assertThat(hasTitle(fixture.playerId(), fixture.titleA())).isFalse();
        assertThat(hasTitle(fixture.playerId(), fixture.titleB())).isTrue();
    }

    @Test
    void keepsBothRowsWhenRevokeDeleteFails() {
        Fixture fixture = fixture(false);
        doThrow(new RuntimeException("delete failed"))
                .when(playerTitleRepository)
                .deleteByPlayerIdAndTitleId(
                        fixture.playerId(),
                        fixture.titleA()
                );

        assertThatThrownBy(() -> playerTitleService.revokeTitle(
                fixture.playerId(),
                fixture.titleA()
        )).isInstanceOf(RuntimeException.class)
                .hasMessage("delete failed");

        assertThat(playerTitleId(fixture.playerId()))
                .isEqualTo(fixture.titleA());
        assertThat(hasTitle(fixture.playerId(), fixture.titleA())).isTrue();
    }

    @Test
    void keepsStateWhenOwnershipCheckFails() {
        Fixture fixture = fixture(false);

        assertThatThrownBy(() -> playerTitleService.revokeTitle(
                fixture.playerId(),
                Long.MAX_VALUE
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(PlayerTitleError.PLAYER_TITLE_NOT_FOUND)
        );

        assertThat(playerTitleId(fixture.playerId()))
                .isEqualTo(fixture.titleA());
        assertThat(hasTitle(fixture.playerId(), fixture.titleA())).isTrue();
    }

    @Test
    void serializesConcurrentChangeAndRevokeWithoutStaleTitle() throws Exception {
        Fixture fixture = fixture(false);

        List<Throwable> outcomes = race(
                () -> playerService.changeRepresentativeTitle(
                        fixture.playerId(),
                        fixture.titleA()
                ),
                () -> playerTitleService.revokeTitle(
                        fixture.playerId(),
                        fixture.titleA()
                )
        );

        assertThat(outcomes.get(1)).isNull();
        if (outcomes.get(0) != null) {
            assertThat(outcomes.get(0)).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(PlayerTitleError.PLAYER_TITLE_NOT_FOUND)
            );
        }
        Long representativeTitleId = playerTitleId(fixture.playerId());
        boolean associationExists = hasTitle(
                fixture.playerId(),
                fixture.titleA()
        );
        assertThat(representativeTitleId).isNull();
        assertThat(associationExists).isFalse();
        assertThat(representativeTitleId == null || associationExists).isTrue();
    }

    private List<Throwable> race(
            ThrowingAction first,
            ThrowingAction second
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();

        try {
            for (ThrowingAction action : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        action.run();
                        return null;
                    } catch (Throwable failure) {
                        return failure;
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Throwable> outcomes = new ArrayList<>();
            for (Future<Throwable> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Fixture fixture(boolean includeTitleB) {
        Long playerId = createPlayer();
        Long titleA = createTitle("A");
        playerTitleService.createTitle(playerId, titleA);
        Long titleB = null;
        if (includeTitleB) {
            titleB = createTitle("B");
            playerTitleService.createTitle(playerId, titleB);
        }
        playerService.changeRepresentativeTitle(playerId, titleA);
        return new Fixture(playerId, titleA, titleB);
    }

    private Long createPlayer() {
        long sequence = SEQUENCE.incrementAndGet();
        return playerService.linkStart(
                sequence,
                new PlayerCommand.Register(
                        "player-" + sequence,
                        "MALE"
                )
        ).id();
    }

    private Long createTitle(String suffix) {
        long sequence = SEQUENCE.incrementAndGet();
        return titleService.create(new TitleCommand.Create(
                "TITLE_230_" + suffix + "_" + sequence,
                "Title " + suffix + " " + sequence,
                "OTHER",
                "Issue 230"
        )).titleId();
    }

    private String playerName(Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT name FROM player WHERE id = ?",
                String.class,
                playerId
        );
    }

    private Long playerTitleId(Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT title_id FROM player WHERE id = ?",
                Long.class,
                playerId
        );
    }

    private boolean hasTitle(Long playerId, Long titleId) {
        return Objects.equals(1, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM player_titles
                WHERE player_id = ? AND title_id = ?
                """,
                Integer.class,
                playerId,
                titleId
        ));
    }

    private record Fixture(Long playerId, Long titleA, Long titleB) {
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run();
    }
}
