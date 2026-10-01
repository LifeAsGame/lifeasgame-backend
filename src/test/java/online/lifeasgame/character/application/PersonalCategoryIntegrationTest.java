package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.PersonalCategory.Kind;
import online.lifeasgame.character.domain.error.PersonalCategoryError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("개인 분류의 MySQL 저장과 소유 경계")
class PersonalCategoryIntegrationTest {
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39");
    static { MYSQL.start(); }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private PersonalCategoryService service;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private CurrentPlayerAccessor currentPlayer;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM player_certifications");
        jdbc.update("DELETE FROM player_hobbies");
        jdbc.update("DELETE FROM personal_categories");
        jdbc.update("DELETE FROM certification");
        jdbc.update("DELETE FROM hobbies");
        jdbc.update("DELETE FROM player");
        for (long id : new long[]{101, 102}) {
            jdbc.update("""
                    INSERT INTO player (id, user_id, name, level, exp, hp_cur, hp_cap, mp_cur, mp_cap,
                        str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat, extra_stats,
                        status_effects, version, created_at, updated_at)
                    VALUES (?, ?, 'tester', 1, 0, 100, 100, 50, 50, 1, 1, 1, 1, 1, 1,
                        JSON_OBJECT(), '[]', 0, NOW(6), NOW(6))
                    """, id, id);
        }
        jdbc.update("INSERT INTO certification (id, name, issuer, category, created_at, updated_at) VALUES (201, 'Cert', 'Issuer', 'CLOUD', NOW(6), NOW(6))");
        jdbc.update("INSERT INTO hobbies (id, name, category, created_at, updated_at) VALUES (301, 'Hobby', 'ARTS', NOW(6), NOW(6))");
        jdbc.update("""
                INSERT INTO player_certifications (player_id, certification_id, granted_at, created_at, updated_at)
                VALUES (101, 201, NOW(6), NOW(6), NOW(6)), (102, 201, NOW(6), NOW(6), NOW(6))
                """);
        jdbc.update("""
                INSERT INTO player_hobbies (player_id, hobby_id, custom_name, proficiency, status, xp)
                VALUES (101, 301, 'Hobby', 0, 'ACTIVE', 0)
                """);
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(101L);
    }

    @Test
    @DisplayName("기본 분류와 빈 개인 분류를 조회하고, 이름 정규화 중복을 DB에서도 거부한다")
    void listsAndDeduplicates() {
        assertThat(service.list(Kind.CERTIFICATION)).hasSize(11);
        assertThat(service.list(Kind.HOBBY)).hasSize(18);
        var created = service.create(Kind.CERTIFICATION, "  Cloud notes  ");
        assertThat(service.list(Kind.CERTIFICATION)).hasSize(12);
        assertThat(service.certificationItems(created.id(), null)).isEmpty();
        assertThatThrownBy(() -> service.create(Kind.CERTIFICATION, "cloud notes"))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(PersonalCategoryError.DUPLICATE_NAME));
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO personal_categories (owner_player_id, kind, name, normalized_name)
                VALUES (101, 'CERTIFICATION', 'duplicate', 'cloud notes')
                """)).isInstanceOf(DuplicateKeyException.class);
        long other = service.create(Kind.CERTIFICATION, "Other notes").id();
        assertThatThrownBy(() -> service.rename(Kind.CERTIFICATION, other, "CLOUD NOTES"))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(PersonalCategoryError.DUPLICATE_NAME));
        assertThat(service.create(Kind.HOBBY, "cloud notes").name()).isEqualTo("cloud notes");
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(102L);
        assertThat(service.create(Kind.CERTIFICATION, "cloud notes").name()).isEqualTo("cloud notes");
    }

    @Test
    @DisplayName("소유 항목만 이동하고 삭제 시 분류 연결만 해제하며 기존 시스템 분류는 보존한다")
    void assignsAndUnlinks() {
        long first = service.create(Kind.CERTIFICATION, "First").id();
        long second = service.create(Kind.CERTIFICATION, "Second").id();
        assertThat(service.assign(Kind.CERTIFICATION, 201L, first).personalCategoryId()).isEqualTo(first);
        assertThat(service.certificationItems(first, null)).hasSize(1);
        assertThat(service.certificationItems(null, "CLOUD")).hasSize(1);
        assertThat(service.rename(Kind.CERTIFICATION, first, "Renamed").name()).isEqualTo("Renamed");
        service.assign(Kind.CERTIFICATION, 201L, second);
        assertThat(service.certificationItems(first, null)).isEmpty();
        assertThat(service.certificationItems(second, null)).hasSize(1);
        service.delete(Kind.CERTIFICATION, second);
        assertThat(jdbc.queryForObject("SELECT personal_category_id FROM player_certifications WHERE player_id=101", Long.class)).isNull();
        assertThat(service.certificationItems(null, "CLOUD")).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM certification WHERE id=201", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> service.assign(Kind.CERTIFICATION, 201L, second))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(PersonalCategoryError.NOT_FOUND));
        service.assign(Kind.CERTIFICATION, 201L, first);
        service.assign(Kind.CERTIFICATION, 201L, null);
        assertThat(service.certificationItems(first, null)).isEmpty();
    }

    @Test
    @DisplayName("다른 소유자와 종류의 분류 및 소유 항목 접근을 거부한다")
    void isolatesOwnerAndKind() {
        long category = service.create(Kind.CERTIFICATION, "Mine").id();
        long hobbyCategory = service.create(Kind.HOBBY, "Mine").id();
        service.assign(Kind.HOBBY, 301L, hobbyCategory);
        assertThat(service.hobbyItems(hobbyCategory, null)).hasSize(1);
        assertThatThrownBy(() -> service.assign(Kind.HOBBY, 301L, category))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(PersonalCategoryError.NOT_FOUND));
        assertThatThrownBy(() -> service.assign(Kind.CERTIFICATION, 999L, category))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(PersonalCategoryError.OWNED_ITEM_NOT_FOUND));
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(102L);
        assertThatThrownBy(() -> service.rename(Kind.CERTIFICATION, category, "Stolen"))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(PersonalCategoryError.NOT_FOUND));
        assertThatThrownBy(() -> service.certificationItems(category, null)).isInstanceOf(DomainException.class);
        assertThat(service.list(Kind.CERTIFICATION)).hasSize(11);
    }

    @Test
    @DisplayName("분류 삭제와 배정이 경쟁해도 삭제된 분류를 참조하지 않는다")
    void deleteRacesAssignment() throws Exception {
        long category = service.create(Kind.CERTIFICATION, "Racing").id();
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var assignment = workers.submit(() -> {
                start.await();
                try { service.assign(Kind.CERTIFICATION, 201L, category); }
                catch (DomainException error) {
                    assertThat(error.getErrorCode()).isEqualTo(PersonalCategoryError.NOT_FOUND);
                }
                return null;
            });
            var deletion = workers.submit(() -> {
                start.await();
                service.delete(Kind.CERTIFICATION, category);
                return null;
            });
            start.countDown();
            assignment.get(10, TimeUnit.SECONDS);
            deletion.get(10, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT personal_category_id FROM player_certifications WHERE player_id=101", Long.class)).isNull();
    }
}
