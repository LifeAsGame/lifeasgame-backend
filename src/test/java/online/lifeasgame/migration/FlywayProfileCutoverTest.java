package online.lifeasgame.migration;

import online.lifeasgame.LifeasgameApplication;
import org.flywaydb.core.Flyway;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "app.outbox.enabled=false"
})
@ActiveProfiles("local")
@DisplayName("Flyway profile cutover")
class FlywayProfileCutoverTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_profile_cutover")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Container
    private static final MySQLContainer<?> FLYWAY_DISABLED_MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_flyway_disabled")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("공통과 배포 Profile 설정을 해석할 때")
    class ResolveProfileProperties {

        @Test
        @DisplayName("공통 기본값은 Flyway 비활성, Hibernate none, 자동 baseline 금지다")
        void keepsConservativeCommonDefaults() throws IOException {
            ConfigurableEnvironment environment = environment(null, Map.of());

            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("spring.flyway.locations"))
                    .isEqualTo("classpath:db/migration");
            assertThat(environment.getProperty(
                    "spring.flyway.baseline-on-migrate", Boolean.class
            )).isFalse();
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("local은 Flyway 활성과 Hibernate validate를 사용한다")
        void enablesFlywayForLocal() throws IOException {
            ConfigurableEnvironment environment = environment("local", Map.of());

            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
            assertThat(environment.getProperty("spring.flyway.locations"))
                    .isEqualTo("classpath:db/migration");
            assertThat(environment.getProperty(
                    "spring.flyway.baseline-on-migrate", Boolean.class
            )).isFalse();
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
        }

        @Test
        @DisplayName("prod는 기본 비활성이며 FLYWAY_ENABLED로만 활성화된다")
        void gatesProductionFlywayWithEnvironmentVariable() throws IOException {
            ConfigurableEnvironment disabled = environment("prod", Map.of());
            ConfigurableEnvironment enabled = environment(
                    "prod", Map.of("FLYWAY_ENABLED", "true")
            );

            assertThat(disabled.getProperty("spring.flyway.enabled", Boolean.class)).isFalse();
            assertThat(enabled.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
            assertThat(enabled.getProperty("spring.flyway.locations"))
                    .isEqualTo("classpath:db/migration");
            assertThat(enabled.getProperty(
                    "spring.flyway.baseline-on-migrate", Boolean.class
            )).isFalse();
            assertThat(enabled.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
        }

        @Test
        @DisplayName("기존 datasource 환경변수 계약을 유지한다")
        void keepsDatasourceEnvironmentContract() throws IOException {
            ConfigurableEnvironment environment = environment(null, Map.of(
                    "DB_URL", "jdbc:mysql://localhost:3306/lifeasgame",
                    "DB_USERNAME", "lifeasgame",
                    "DB_PASSWORD", "secret"
            ));

            assertThat(environment.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:mysql://localhost:3306/lifeasgame");
            assertThat(environment.getProperty("spring.datasource.username"))
                    .isEqualTo("lifeasgame");
            assertThat(environment.getProperty("spring.datasource.password"))
                    .isEqualTo("secret");
        }
    }

    @Nested
    @DisplayName("빈 MySQL을 local Profile로 기동할 때")
    class StartLocalWithCleanDatabase {

        @Test
        @DisplayName("V1부터 V33까지 적용한 뒤 Hibernate validate로 Context가 기동한다")
        void migratesThenValidates() {
            ConfigurableEnvironment environment =
                    (ConfigurableEnvironment) applicationContext.getEnvironment();

            assertThat(applicationContext).isNotNull();
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
            assertThat(environment.getProperty(
                    "spring.flyway.baseline-on-migrate", Boolean.class
            )).isFalse();
            assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("33");
            assertThat(appliedMigrationCount()).isEqualTo(33);
        }
    }

    @Nested
    @DisplayName("Flyway가 비활성인 prod-equivalent 빈 DB를 기동할 때")
    class StartProductionWithFlywayDisabled {

        @Test
        @DisplayName("migration과 Hibernate update 없이 validate 실패로 기동을 중단한다")
        void failsValidationWithoutCreatingSchema() throws Exception {
            String jdbcUrl = FLYWAY_DISABLED_MYSQL.getJdbcUrl();

            Throwable thrown = catchThrowable(() -> new SpringApplicationBuilder(
                    LifeasgameApplication.class
            )
                    .web(WebApplicationType.NONE)
                    .registerShutdownHook(false)
                    .run(
                            "--spring.profiles.active=prod",
                            "--FLYWAY_ENABLED=false",
                            "--LIFEASGAME_WEB_ALLOWED_ORIGINS=https://app.example.com",
                            "--spring.datasource.url=" + jdbcUrl,
                            "--spring.datasource.username=" + FLYWAY_DISABLED_MYSQL.getUsername(),
                            "--spring.datasource.password=" + FLYWAY_DISABLED_MYSQL.getPassword(),
                            "--spring.datasource.driver-class-name="
                                    + FLYWAY_DISABLED_MYSQL.getDriverClassName(),
                            "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
                            "--lifeasgame.jwt.secret=test-secret-key-must-be-at-least-32-characters-long",
                            "--spring.main.banner-mode=off"
                    ));

            assertThat(rootCause(thrown))
                    .isInstanceOf(SchemaManagementException.class)
                    .hasMessageContaining("Schema-validation: missing table");
            assertThat(tableCount(jdbcUrl, "flyway_schema_history")).isZero();
            assertThat(schemaTableCount()).isZero();
        }
    }

    private int appliedMigrationCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                """, Integer.class);
    }

    private ConfigurableEnvironment environment(
            String profile,
            Map<String, Object> overrides
    ) throws IOException {
        MockEnvironment environment = new MockEnvironment();
        addYamlFirst(environment, "application.yml", "common");
        if (profile != null) {
            addYamlFirst(environment, "application-" + profile + ".yml", profile);
        }
        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("overrides", overrides)
            );
        }
        return environment;
    }

    private void addYamlFirst(
            ConfigurableEnvironment environment,
            String path,
            String name
    ) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (PropertySource<?> source : loader.load(name, new ClassPathResource(path))) {
            environment.getPropertySources().addFirst(source);
        }
    }

    private int tableCount(String jdbcUrl, String tableName) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl,
                FLYWAY_DISABLED_MYSQL.getUsername(),
                FLYWAY_DISABLED_MYSQL.getPassword()
        ); var statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int schemaTableCount() throws Exception {
        try (Connection connection = FLYWAY_DISABLED_MYSQL.createConnection("");
             var statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM information_schema.tables
                     WHERE table_schema = ?
                     """)) {
            statement.setString(1, FLYWAY_DISABLED_MYSQL.getDatabaseName());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private Throwable rootCause(Throwable throwable) {
        assertThat(throwable).isNotNull();
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
