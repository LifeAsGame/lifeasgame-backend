package online.lifeasgame.character.application;

import online.lifeasgame.auth.application.AuthService;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentUserAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Atomic Player onboarding MySQL integration")
class PlayerOnboardingIntegrationTest {

    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_player_onboarding")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    private static final AtomicLong USER_SEQUENCE =
            new AtomicLong(331_000L);

    @Container
    private static final MySQLContainer<?> CONTAINER = MYSQL;

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
    private PlayerFacade playerFacade;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserAccessor currentUserAccessor;

    @MockitoBean
    private AuthService authTokenApi;

    @MockitoSpyBean
    private PlayerEquipmentRepository playerEquipmentRepository;

    private final AtomicReference<Long> currentUserId =
            new AtomicReference<>();

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM player_equipment");
        jdbc.update("DELETE FROM inventory_entries");
        jdbc.update("DELETE FROM mailbox_entries");
        jdbc.update("DELETE FROM player_inventory");
        jdbc.update("DELETE FROM player_mailbox");
        jdbc.update("DELETE FROM player_titles");
        jdbc.update("DELETE FROM player_achievements");
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM player");
        jdbc.update("""
                DELETE FROM equipment_slots
                WHERE definition_version <> '1.0.0'
                """);

        currentUserId.set(USER_SEQUENCE.incrementAndGet());
        given(currentUserAccessor.currentUserIdOrThrow())
                .willAnswer(invocation -> currentUserId.get());
        given(authTokenApi.issueToken(anyLong(), anyLong()))
                .willAnswer(invocation -> token(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ));
    }

    @Nested
    @DisplayName("LAG-EQSA 1.0.0 migration catalog")
    class Catalog {

        @Test
        @DisplayName("17개 definition과 P0/P1/P2 lifecycle 및 9개 eager 계약을 보존한다")
        void preservesApprovedAuthority() {
            assertThat(count("""
                    SELECT COUNT(*) FROM equipment_slots
                    WHERE definition_version = '1.0.0'
                    """)).isEqualTo(17);
            assertThat(count("""
                    SELECT COUNT(*) FROM equipment_slots
                    WHERE definition_version = '1.0.0'
                      AND release_tier = 'P0'
                      AND enabled = b'1'
                      AND lifecycle_status = 'ACTIVE'
                    """)).isEqualTo(10);
            assertThat(count("""
                    SELECT COUNT(*) FROM equipment_slots
                    WHERE definition_version = '1.0.0'
                      AND release_tier = 'P1'
                      AND enabled = b'0'
                      AND lifecycle_status = 'GATED'
                    """)).isEqualTo(5);
            assertThat(count("""
                    SELECT COUNT(*) FROM equipment_slots
                    WHERE definition_version = '1.0.0'
                      AND release_tier = 'P2'
                      AND enabled = b'0'
                      AND lifecycle_status = 'GATED'
                    """)).isEqualTo(2);
            assertThat(requiredCatalogCodes()).containsExactlyElementsOf(
                    PlayerEquipmentProvisioningPolicy.REQUIRED_CODES
            );
            assertThat(count("""
                    SELECT COUNT(*) FROM equipment_slots
                    WHERE definition_version = '1.0.0'
                      AND (category IS NOT NULL OR role IS NOT NULL)
                    """)).isZero();
            assertThat(jdbc.queryForMap("""
                    SELECT logical_category, enabled, lifecycle_status,
                           eager_on_link_start
                    FROM equipment_slots
                    WHERE code = 'TITLE'
                      AND definition_version = '1.0.0'
                    """))
                    .containsEntry("logical_category", "IDENTITY")
                    .containsEntry("lifecycle_status", "ACTIVE");
            assertThat(count("""
                    SELECT COUNT(*) FROM equipment_slots
                    WHERE code = 'TITLE'
                      AND definition_version = '1.0.0'
                      AND enabled = b'1'
                      AND eager_on_link_start = b'0'
                    """)).isEqualTo(1);
            assertThat(uniqueIndexColumns()).containsExactly(
                    "code",
                    "definition_version"
            );
        }
    }

    @Nested
    @DisplayName("새 Player를 등록할 때")
    class NewPlayer {

        @Test
        @DisplayName("Player와 정확한 9개 empty row가 commit된 뒤 token을 발급한다")
        void commitsCanonicalStateBeforeIssuingToken() {
            long itemsBefore = count("SELECT COUNT(*) FROM items");
            given(authTokenApi.issueToken(anyLong(), anyLong()))
                    .willAnswer(invocation -> {
                        Long userId = invocation.getArgument(0);
                        Long playerId = invocation.getArgument(1);
                        assertThat(playerCount(userId)).isEqualTo(1);
                        assertThat(equipmentCount(playerId)).isEqualTo(9);
                        return token(userId, playerId);
                    });

            PlayerResult.CreatedWithToken result = register(
                    "온보딩 플레이어",
                    "FEMALE"
            );

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            assertThat(requiredPlayerCodes(result.id()))
                    .containsExactlyElementsOf(
                            PlayerEquipmentProvisioningPolicy.REQUIRED_CODES
                    );
            assertThat(count("""
                    SELECT COUNT(*) FROM player_equipment
                    WHERE player_id = %d
                      AND (item_inst_id IS NOT NULL OR equipped_at IS NOT NULL)
                    """.formatted(result.id()))).isZero();
            assertThat(count("""
                    SELECT COUNT(*)
                    FROM player_equipment equipment
                    JOIN equipment_slots slot ON slot.id = equipment.slot_id
                    WHERE equipment.player_id = %d
                      AND (slot.code = 'TITLE'
                           OR slot.lifecycle_status <> 'ACTIVE'
                           OR slot.enabled = b'0')
                    """.formatted(result.id()))).isZero();
            assertNoStarterGrant(result.id(), itemsBefore);
            assertThat(jdbc.queryForObject(
                    "SELECT title_id FROM player WHERE id = ?",
                    Long.class,
                    result.id()
            )).isNull();
        }

        @Test
        @DisplayName("required-row 실패는 Player까지 rollback하고 같은 user retry를 허용한다")
        void rollsBackPlayerAndAllowsRetry() {
            RuntimeException failure = new RuntimeException(
                    "forced equipment provisioning failure"
            );
            doAnswer(invocation -> {
                invocation.callRealMethod();
                throw failure;
            })
                    .doCallRealMethod()
                    .when(playerEquipmentRepository)
                    .saveAllAndFlush(anyList());

            assertThatThrownBy(() -> register("retry-player", "MALE"))
                    .isSameAs(failure);

            assertThat(playerCount(currentUserId.get())).isZero();
            assertThat(count("SELECT COUNT(*) FROM player_equipment"))
                    .isZero();
            assertThat(count("SELECT COUNT(*) FROM outbox_events")).isZero();
            verifyNoInteractions(authTokenApi);

            PlayerResult.CreatedWithToken retried = register(
                    "retry-player",
                    "MALE"
            );

            assertThat(playerCount(currentUserId.get())).isEqualTo(1);
            assertThat(equipmentCount(retried.id())).isEqualTo(9);
            verify(authTokenApi).issueToken(currentUserId.get(), retried.id());
        }
    }

    @Nested
    @DisplayName("기존 Player가 다시 등록할 때")
    class ExistingPlayer {

        @Test
        @DisplayName("token 실패 후 replay는 원래 identity와 9개 row를 보존하고 token만 재발급한다")
        void replaysCompletedOnboardingWithoutMutation() {
            RuntimeException tokenFailure = new RuntimeException(
                    "forced token failure"
            );
            given(authTokenApi.issueToken(anyLong(), anyLong()))
                    .willThrow(tokenFailure)
                    .willAnswer(invocation -> token(
                            invocation.getArgument(0),
                            invocation.getArgument(1)
                    ));

            assertThatThrownBy(() -> register("original-name", "MALE"))
                    .isSameAs(tokenFailure);
            Long playerId = playerId(currentUserId.get());

            PlayerResult.CreatedWithToken replayed = register(
                    "ignored-name",
                    "FEMALE"
            );

            assertThat(replayed.id()).isEqualTo(playerId);
            assertThat(jdbc.queryForObject(
                    "SELECT name FROM player WHERE id = ?",
                    String.class,
                    playerId
            )).isEqualTo("original-name");
            assertThat(jdbc.queryForObject(
                    "SELECT gender FROM player WHERE id = ?",
                    String.class,
                    playerId
            )).isEqualTo("male");
            assertThat(equipmentCount(playerId)).isEqualTo(9);
            assertThat(count("SELECT COUNT(*) FROM outbox_events"))
                    .isEqualTo(1);
            verify(authTokenApi, times(2))
                    .issueToken(currentUserId.get(), playerId);
        }

        @ParameterizedTest(name = "existing required rows = {0}")
        @ValueSource(ints = {0, 3})
        @DisplayName("zero/subset legacy state에는 missing row만 보정하고 gated extra를 보존한다")
        void reconcilesMissingRowsAndPreservesGatedExtra(int existingRows) {
            Long playerId = insertPlayer(
                    currentUserId.get(),
                    "legacy-player",
                    "male"
            );
            for (String code : PlayerEquipmentProvisioningPolicy
                    .REQUIRED_CODES.subList(0, existingRows)) {
                insertEquipment(playerId, slotId(code, "1.0.0"));
            }
            insertEquipment(playerId, slotId("FACE", "1.0.0"));

            PlayerResult.CreatedWithToken result = register(
                    "ignored",
                    "FEMALE"
            );

            assertThat(result.id()).isEqualTo(playerId);
            assertThat(requiredPlayerCodes(playerId))
                    .containsExactlyElementsOf(
                            PlayerEquipmentProvisioningPolicy.REQUIRED_CODES
                    );
            assertThat(equipmentCount(playerId)).isEqualTo(10);
            assertThat(count("""
                    SELECT COUNT(*)
                    FROM player_equipment equipment
                    JOIN equipment_slots slot ON slot.id = equipment.slot_id
                    WHERE equipment.player_id = %d
                      AND slot.code = 'FACE'
                    """.formatted(playerId))).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 required code의 conflicting version rows는 보존하고 fail closed한다")
        void rejectsConflictingLegacyState() {
            Long playerId = insertPlayer(
                    currentUserId.get(),
                    "conflicting-player",
                    "male"
            );
            Long first = insertConflictingHeadDefinition("0.8.0");
            Long second = insertConflictingHeadDefinition("0.9.0");
            insertEquipment(playerId, first);
            insertEquipment(playerId, second);

            assertThatThrownBy(() -> register("ignored", "FEMALE"))
                    .isInstanceOfSatisfying(
                            DomainException.class,
                            exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo(PlayerEquipmentError
                                            .PLAYER_EQUIPMENT_ONBOARDING_CONFLICT)
                    );

            assertThat(equipmentCount(playerId)).isEqualTo(2);
            assertThat(requiredPlayerCodes(playerId)).isEmpty();
            verifyNoInteractions(authTokenApi);
        }
    }

    @Test
    @DisplayName("동일 user concurrent registration은 한 Player와 정확한 9개 row로 수렴한다")
    void convergesConcurrentDuplicateRegistration() throws Exception {
        List<Throwable> outcomes = race(
                () -> register("concurrent-player", "MALE"),
                () -> register("concurrent-player", "MALE")
        );

        long successes = outcomes.stream().filter(Objects::isNull).count();
        assertThat(successes).isBetween(1L, 2L);
        outcomes.stream()
                .filter(Objects::nonNull)
                .forEach(failure -> assertThat(failure)
                        .isInstanceOfSatisfying(
                                DomainException.class,
                                exception -> assertThat(exception.getErrorCode())
                                        .isEqualTo(
                                                PlayerError.PLAYER_ALREADY_EXISTS
                                        )
                        ));
        Long playerId = playerId(currentUserId.get());
        assertThat(playerCount(currentUserId.get())).isEqualTo(1);
        assertThat(equipmentCount(playerId)).isEqualTo(9);
        assertThat(count("SELECT COUNT(*) FROM outbox_events")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM inventory_entries")).isZero();
        assertThat(count("SELECT COUNT(*) FROM mailbox_entries")).isZero();
    }

    private PlayerResult.CreatedWithToken register(
            String name,
            String gender
    ) {
        return playerFacade.linkStart(new PlayerCommand.Register(name, gender));
    }

    private AuthResult.TokenPair token(Long userId, Long playerId) {
        return new AuthResult.TokenPair(
                "access-token",
                "refresh-token",
                userId,
                playerId
        );
    }

    private void assertNoStarterGrant(Long playerId, long itemsBefore) {
        assertThat(count("SELECT COUNT(*) FROM items")).isEqualTo(itemsBefore);
        for (String table : List.of(
                "player_inventory",
                "inventory_entries",
                "player_mailbox",
                "mailbox_entries",
                "player_titles",
                "player_achievements"
        )) {
            assertThat(count("SELECT COUNT(*) FROM " + table
                    + " WHERE player_id = " + playerId)).isZero();
        }
    }

    private long playerCount(Long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM player WHERE user_id = ?",
                Long.class,
                userId
        );
    }

    private Long playerId(Long userId) {
        return jdbc.queryForObject(
                "SELECT id FROM player WHERE user_id = ?",
                Long.class,
                userId
        );
    }

    private long equipmentCount(Long playerId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM player_equipment WHERE player_id = ?",
                Long.class,
                playerId
        );
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    private List<String> requiredCatalogCodes() {
        return jdbc.queryForList("""
                SELECT code
                FROM equipment_slots
                WHERE definition_version = '1.0.0'
                  AND eager_on_link_start = b'1'
                ORDER BY sort_order
                """, String.class);
    }

    private List<String> requiredPlayerCodes(Long playerId) {
        return jdbc.queryForList("""
                SELECT slot.code
                FROM player_equipment equipment
                JOIN equipment_slots slot ON slot.id = equipment.slot_id
                WHERE equipment.player_id = ?
                  AND slot.definition_version = '1.0.0'
                  AND slot.eager_on_link_start = b'1'
                ORDER BY slot.sort_order
                """, String.class, playerId);
    }

    private List<String> uniqueIndexColumns() {
        return jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'equipment_slots'
                  AND index_name = 'uq_equipment_slot_code_version'
                  AND non_unique = 0
                ORDER BY seq_in_index
                """, String.class);
    }

    private Long insertPlayer(Long userId, String name, String gender) {
        jdbc.update("""
                INSERT INTO player (
                    user_id, name, gender, level, exp,
                    hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat,
                    extra_stats, status_effects, version,
                    created_at, updated_at
                ) VALUES (
                    ?, ?, ?, 1, 0,
                    100, 100, 50, 50,
                    1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, userId, name, gender);
        return playerId(userId);
    }

    private Long slotId(String code, String version) {
        return jdbc.queryForObject("""
                SELECT id FROM equipment_slots
                WHERE code = ? AND definition_version = ?
                """, Long.class, code, version);
    }

    private void insertEquipment(Long playerId, Long slotId) {
        jdbc.update("""
                INSERT INTO player_equipment (
                    created_at, equipped_at, item_inst_id,
                    player_id, slot_id, updated_at
                ) VALUES (
                    CURRENT_TIMESTAMP(6), NULL, NULL,
                    ?, ?, CURRENT_TIMESTAMP(6)
                )
                """, playerId, slotId);
    }

    private Long insertConflictingHeadDefinition(String version) {
        jdbc.update("""
                INSERT INTO equipment_slots (
                    created_at, updated_at, code, name, category, role,
                    definition_version, logical_category, semantic_role,
                    release_tier, sort_order, enabled, lifecycle_status,
                    introduced_activation_wave, source_revision, approved_by,
                    eager_on_link_start
                ) VALUES (
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
                    'HEAD', 'legacy head', 'HEAD', 'SINGLE', ?, 'AVATAR',
                    'conflicting historical definition', 'P0', 10,
                    b'0', 'GATED', 'LEGACY_TEST', 'LEGACY_TEST',
                    'LEGACY_TEST', b'0'
                )
                """, version);
        return slotId("HEAD", version);
    }

    private List<Throwable> race(Runnable first, Runnable second)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();
        try {
            for (Runnable action : List.of(first, second)) {
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
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .isTrue();
        }
    }
}
