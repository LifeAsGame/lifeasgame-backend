package online.lifeasgame.user.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.platform.outbox.application.OutboxClaim;
import online.lifeasgame.platform.outbox.application.OutboxClaimService;
import online.lifeasgame.platform.outbox.application.OutboxDispatchAttempt;
import online.lifeasgame.user.application.command.UserSettingCommand;
import online.lifeasgame.user.application.internal.UserAuthApi;
import online.lifeasgame.user.application.result.UserSettingResult;
import online.lifeasgame.user.domain.UserSetting;
import online.lifeasgame.user.domain.error.UserSettingError;
import online.lifeasgame.user.domain.event.UserRegistered;
import online.lifeasgame.user.domain.repository.UserSettingRepository;
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
import org.springframework.test.util.AopTestUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("UserSetting 초기화의 MySQL 트랜잭션과 멱등성")
class UserSettingInitializationIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_user_settings")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired private UserAuthApi userAuthApi;
    @Autowired private UserSettingService settings;
    @Autowired private OutboxClaimService claims;
    @Autowired private OutboxDispatchAttempt dispatch;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private CurrentUserAccessor currentUser;
    @MockitoSpyBean private DomainEventPublisher events;
    @MockitoSpyBean private UserSettingRepository settingRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM user_settings");
        jdbc.update("DELETE FROM users");
    }

    @Nested
    @DisplayName("일반 가입과 OAuth 신규 가입에서")
    class Registration {

        @ParameterizedTest(name = "OAuth = {0}")
        @ValueSource(booleans = {false, true})
        @DisplayName("Outbox 전달 전 첫 본인 설정 조회에 기본 설정이 보인다")
        void readsImmediately(boolean oauth) {
            Long userId = register(oauth);
            when(currentUser.currentUserIdOrThrow()).thenReturn(userId);

            UserSettingResult.Settings result = settings.getSettings();

            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.volume()).isEqualTo(50);
            assertThat(result.uiLayoutJson()).isNull();
            assertThat(result.flagsJson()).isNull();
            assertThat(result.updatedAt()).isNotNull();
            assertThat(settingCount(userId)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "SELECT status FROM outbox_events WHERE event_type = 'user.registered.v1'",
                    String.class)).isEqualTo("PENDING");
        }

        @ParameterizedTest(name = "OAuth = {0}")
        @ValueSource(booleans = {false, true})
        @DisplayName("Outbox 저장 후 가입이 실패하면 사용자·설정·이벤트가 함께 롤백된다")
        void rollsBackAfterAppend(boolean oauth) {
            doAnswer(invocation -> {
                invocation.callRealMethod();
                UserRegistered event = invocation.getArgument(0);
                assertThat(settingCount(event.userId())).isEqualTo(1);
                assertThat(count("outbox_events")).isEqualTo(1);
                throw new IllegalStateException("registration failed after append");
            }).when((DomainEventPublisher) AopTestUtils.getUltimateTargetObject(events))
                    .publish(any(UserRegistered.class));

            assertThatThrownBy(() -> register(oauth))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("registration failed after append");

            assertRegistrationRolledBack();
        }

        @ParameterizedTest(name = "OAuth = {0}")
        @ValueSource(booleans = {false, true})
        @DisplayName("설정 초기화 실패도 가입 트랜잭션을 롤백한다")
        void rollsBackOnInitializationFailure(boolean oauth) {
            doAnswer(invocation -> {
                invocation.callRealMethod();
                throw new IllegalStateException("settings initialization failed");
            }).when(settingRepository).insertIfAbsent(any(UserSetting.class));

            assertThatThrownBy(() -> register(oauth))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("settings initialization failed");

            assertRegistrationRolledBack();
        }
    }

    @Nested
    @DisplayName("이벤트 재전달과 반복 초기화에서")
    class Replay {

        @Test
        @DisplayName("같은 Outbox 이벤트를 재전달해도 변경한 설정과 수정 시각을 보존한다")
        void preservesPreferencesOnRedelivery() {
            Long userId = register(false);
            customize(userId);
            UserSettingResult.Settings before = settings.getSettings(userId);
            OutboxClaim event = claims.claimBatch().getFirst();
            assertThat(event.eventType()).isEqualTo("user.registered.v1");

            dispatch.dispatch(event);
            dispatch.dispatch(event);
            settings.ensureDefaultIfMissing(userId);

            assertThat(settings.getSettings(userId)).isEqualTo(before);
            assertThat(settingCount(userId)).isEqualTo(1);
        }

        @Test
        @DisplayName("누락된 기존 설정은 이벤트 재전달로 한 번만 생성된다")
        void repairsMissingSettingsOnDelivery() {
            Long userId = register(false);
            jdbc.update("DELETE FROM user_settings WHERE user_id = ?", userId);
            OutboxClaim event = claims.claimBatch().getFirst();

            dispatch.dispatch(event);
            UserSettingResult.Settings first = settings.getSettings(userId);
            dispatch.dispatch(event);

            assertThat(first.volume()).isEqualTo(50);
            assertThat(settings.getSettings(userId)).isEqualTo(first);
            assertThat(settingCount(userId)).isEqualTo(1);
        }

        @Test
        @DisplayName("기존 OAuth 사용자의 재로그인은 설정과 Outbox를 변경하지 않는다")
        void preservesExistingOAuthUser() {
            Long userId = register(true);
            customize(userId);
            UserSettingResult.Settings before = settings.getSettings(userId);

            assertThat(register(true)).isEqualTo(userId);

            assertThat(settings.getSettings(userId)).isEqualTo(before);
            assertThat(count("users")).isEqualTo(1);
            assertThat(count("outbox_events")).isEqualTo(1);
        }

        @Test
        @DisplayName("누락된 설정을 조회해도 DB나 Outbox를 쓰지 않는다")
        void keepsReadPathReadOnly() {
            Long userId = register(false);
            jdbc.update("DELETE FROM user_settings WHERE user_id = ?", userId);
            when(currentUser.currentUserIdOrThrow()).thenReturn(userId);

            assertThatThrownBy(() -> settings.getSettings())
                    .isInstanceOfSatisfying(DomainException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(UserSettingError.USER_SETTING_NOT_FOUND));

            assertThat(settingCount(userId)).isZero();
            assertThat(count("outbox_events")).isEqualTo(1);
        }
    }

    @ParameterizedTest(name = "Existing settings = {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("두 트랜잭션이 함께 초기화해도 한 행만 존재하며 기존 값을 보존한다")
    void initializesConcurrently(boolean existing) throws Exception {
        Long userId = register(false);
        UserSettingResult.Settings before;
        if (existing) {
            customize(userId);
            before = settings.getSettings(userId);
        } else {
            jdbc.update("DELETE FROM user_settings WHERE user_id = ?", userId);
            before = null;
        }
        CountDownLatch bothInTransaction = new CountDownLatch(2);
        doAnswer(invocation -> {
            bothInTransaction.countDown();
            assertThat(bothInTransaction.await(10, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(settingRepository).insertIfAbsent(any(UserSetting.class));

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> settings.ensureDefaultIfMissing(userId));
            var second = executor.submit(() -> settings.ensureDefaultIfMissing(userId));
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(userId);
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo(userId);
        }

        assertThat(settingCount(userId)).isEqualTo(1);
        if (existing) {
            assertThat(settings.getSettings(userId)).isEqualTo(before);
        } else {
            assertThat(settings.getSettings(userId).volume()).isEqualTo(50);
        }
    }

    private Long register(boolean oauth) {
        return oauth
                ? userAuthApi.findOrRegisterByGoogle("oauth@example.com", "OAuthUser")
                : userAuthApi.register("regular@example.com", "password123", "RegularUser");
    }

    private void customize(Long userId) {
        settings.updateSettings(userId, new UserSettingCommand.UpdateSettings(
                17, "{\"theme\":\"dark\"}", "{\"music\":false}"));
    }

    private void assertRegistrationRolledBack() {
        assertThat(count("users")).isZero();
        assertThat(count("user_settings")).isZero();
        assertThat(count("outbox_events")).isZero();
    }

    private int settingCount(Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_settings WHERE user_id = ?",
                Integer.class, userId);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
