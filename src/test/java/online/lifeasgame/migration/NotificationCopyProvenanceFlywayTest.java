package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.UncategorizedSQLException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("Notification copy V34 → V35 MySQL migration")
class NotificationCopyProvenanceFlywayTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39");

    @Test
    @DisplayName("기존 본문은 그대로 두고 metadata는 null이며 이전 writer와 호환되고 부분 metadata는 거부한다")
    void preservesLegacyWithoutInventingProvenance() {
        var dataSource = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("34").load().migrate();
        insertLegacy(jdbc, "before-migration");
        var before = jdbc.queryForMap("SELECT title, body, source_event_id, occurred_at, read_at FROM player_notifications");
        var flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("36");
        assertThat(jdbc.queryForMap("SELECT title, body, source_event_id, occurred_at, read_at FROM player_notifications"))
                .isEqualTo(before);
        assertThat(jdbc.queryForMap("SELECT title_copy_id, title_copy_version, body_copy_id, body_copy_version, copy_locale FROM player_notifications"))
                .allSatisfy((key, value) -> assertThat(value).isNull());
        insertLegacy(jdbc, "old-writer-after-migration");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM player_notifications WHERE title_copy_id IS NULL", Long.class)).isEqualTo(2);
        assertThatThrownBy(() -> jdbc.update("UPDATE player_notifications SET title_copy_version = 1"))
                .isInstanceOfSatisfying(UncategorizedSQLException.class, error -> {
                    assertThat(error.getSQLException().getErrorCode()).isEqualTo(3819);
                    assertThat(error.getMessage()).contains("ck_notification_copy_provenance");
                });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM player_notifications WHERE title_copy_version IS NULL", Long.class)).isEqualTo(2);
    }

    private void insertLegacy(JdbcTemplate jdbc, String event) {
        jdbc.update("""
                INSERT INTO player_notifications (player_id, type, title, body, source_event_id,
                  occurred_at, read_at, created_at, updated_at)
                VALUES (1, 'SYSTEM_NOTICE', '기존 제목', '기존 본문', ?, NOW(6), NULL, NOW(6), NOW(6))
                """, event);
    }
}
