package online.lifeasgame.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import online.lifeasgame.LifeasgameApplication;
import online.lifeasgame.user.application.command.UserCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = LifeasgameApplication.class)
@ActiveProfiles({"test", "migration-test"})
@Testcontainers
@DisplayName("계정 변경 응답의 저장 시각")
class UserChangeTimestampIntegrationTest {

    private static final long USER_ID = 940001L;
    private static final Instant ORIGINAL = Instant.parse("2000-01-01T00:00:00Z");

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("user_change_timestamp")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired UserService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM users WHERE id = ?", USER_ID);
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, nickname, status,
                    account_authority, created_at, updated_at)
                VALUES (?, 'change-time@example.com', 'unused', 'Before', 'ACTIVE',
                    'USER', ?, ?)
                """, USER_ID, LocalDateTime.ofInstant(ORIGINAL, ZoneOffset.UTC),
                LocalDateTime.ofInstant(ORIGINAL, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("닉네임 변경 응답은 변경 전후 값과 커밋된 감사 시각을 반환한다")
    void nicknameChangedAtMatchesStoredUpdate() {
        var result = service.changeNickname(USER_ID, "After");

        assertThat(result.oldNickname()).isEqualTo("Before");
        assertThat(result.newNickname()).isEqualTo("After");
        assertThat(result.changedAt()).isAfter(ORIGINAL);
        assertThat(result.changedAt().truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(storedUpdatedAt());
    }

    @Test
    @DisplayName("상태 변경 응답은 변경 전후 값과 커밋된 감사 시각을 반환한다")
    void statusChangedAtMatchesStoredUpdate() {
        var result = service.changeStatus(USER_ID,
                new UserCommand.ChangeStatus("BANNED", "moderation"));

        assertThat(result.fromStatus()).isEqualTo("ACTIVE");
        assertThat(result.toStatus()).isEqualTo("BANNED");
        assertThat(result.changedAt()).isAfter(ORIGINAL);
        assertThat(result.changedAt().truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(storedUpdatedAt());
    }

    @Test
    @DisplayName("같은 상태 명령은 기존 저장 시각을 유지한다")
    void sameStatusPreservesTimestamp() {
        var result = service.changeStatus(USER_ID,
                new UserCommand.ChangeStatus("ACTIVE", "same state"));

        assertThat(result.fromStatus()).isEqualTo("ACTIVE");
        assertThat(result.toStatus()).isEqualTo("ACTIVE");
        assertThat(result.changedAt()).isEqualTo(ORIGINAL);
        assertThat(storedUpdatedAt()).isEqualTo(ORIGINAL);
    }

    private Instant storedUpdatedAt() {
        return jdbc.queryForObject("SELECT updated_at FROM users WHERE id = ?",
                (rs, row) -> rs.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                USER_ID);
    }
}
