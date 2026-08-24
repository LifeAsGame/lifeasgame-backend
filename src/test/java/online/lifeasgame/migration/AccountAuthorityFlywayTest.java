package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("V29 account authority migration")
class AccountAuthorityFlywayTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            "mysql:8.0.39"
    )
            .withDatabaseName("lifeasgame_account_authority")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Test
    @DisplayName("existing/new account는 USER이고 USER/ADMIN 외 값은 거절한다")
    void migratesExistingAndNewAccountsSafely() throws Exception {
        Flyway throughV28 = flyway(MigrationVersion.fromVersion("28"));
        throughV28.migrate();
        insertUser(30001L, "existing@example.com");

        Flyway latest = flyway(null);
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(latest.info().current().getVersion().getVersion())
                .isEqualTo("29");
        assertThat(authority(30001L)).isEqualTo("USER");

        insertUser(30002L, "new@example.com");
        assertThat(authority(30002L)).isEqualTo("USER");
        assertThatThrownBy(() -> updateAuthority(30002L, "GAMEPLAY_ROLE"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("ck_user_account_authority");
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword()
                )
                .locations("classpath:db/migration")
                .baselineOnMigrate(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private void insertUser(long id, String email) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (
                        id, email, password_hash, nickname, status,
                        created_at, updated_at
                    ) VALUES (
                        %d, '%s', 'hashed-password', 'user%d', 'ACTIVE',
                        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                    )
                    """.formatted(id, email, id));
        }
    }

    private String authority(long id) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT account_authority FROM users WHERE id = %d
                     """.formatted(id))) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private void updateAuthority(long id, String authority) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE users SET account_authority = '%s' WHERE id = %d
                    """.formatted(authority, id));
        }
    }
}
