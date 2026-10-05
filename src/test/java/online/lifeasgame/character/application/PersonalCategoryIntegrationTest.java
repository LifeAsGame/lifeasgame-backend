package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.PersonalCategory.Kind;
import online.lifeasgame.character.domain.error.PlayerHobbyError;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
    @Autowired private PrivateHobbyService privateHobbies;
    @Autowired private OfficialCertificationImporter importer;
    @Autowired private CharacterCatalogService catalog;
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
    @DisplayName("독립 취미는 카탈로그 없이 저장되고 소유자만 수정하며 분류 삭제 후에도 남는다")
    void privateHobbyOwnershipAndCategoryPreservation() {
        var category = service.create(Kind.HOBBY, "Personal notes");
        var created = privateHobbies.create("  나만의 취미  ", "memory", 25, "ACTIVE",
                LocalDate.of(2025, 3, 1), category.id());
        assertThat(created.catalogItemId()).isNull();
        assertThat(created.source()).isEqualTo("PRIVATE");
        assertThat(created.name()).isEqualTo("나만의 취미");
        assertThat(privateHobbies.list()).hasSize(1);
        assertThatThrownBy(() -> privateHobbies.create("나만의 취미", null, null, null, null, null))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(PlayerHobbyError.DUPLICATE_PRIVATE_HOBBY));
        assertThatThrownBy(() -> privateHobbies.get(301L))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(PlayerHobbyError.PLAYER_HOBBY_NOT_FOUND));
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(102L);
        assertThatThrownBy(() -> privateHobbies.get(created.ownedItemId()))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(PlayerHobbyError.PLAYER_HOBBY_NOT_FOUND));
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(101L);
        var changed = privateHobbies.update(created.ownedItemId(), "Other name", null, null, null,
                null, null, false);
        assertThat(changed.name()).isEqualTo("Other name");
        service.delete(Kind.HOBBY, category.id());
        assertThat(privateHobbies.get(created.ownedItemId()).personalCategoryId()).isNull();
        privateHobbies.delete(created.ownedItemId());
        assertThat(privateHobbies.list()).isEmpty();
    }

    @Test
    @DisplayName("공식 재수입은 이름이 바뀌어도 ID를 유지하고 불완전 입력은 기존 데이터를 건드리지 않는다")
    void importPreservesIdentityAndRejectsIncompleteSource() {
        var first = officialManifest("Old name");
        assertThat(importer.importManifest(first, false).applied()).isFalse();
        assertThat(importer.importManifest(first, true).created()).isEqualTo(1);
        Long id = jdbc.queryForObject("SELECT id FROM certification WHERE source_code='T001'", Long.class);
        assertThat(importer.importManifest(officialManifest("New name"), true).updated()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id FROM certification WHERE source_code='T001'", Long.class)).isEqualTo(id);
        assertThat(catalog.get(Kind.CERTIFICATION, id).name()).isEqualTo("New name");
        assertThatThrownBy(() -> importer.importManifest(new OfficialCertificationImporter.Manifest(
                false, 1, Instant.now(), "https://www.data.go.kr/data/15003003/openapi.do",
                first.pages(), first.items()), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> importer.importManifest(new OfficialCertificationImporter.Manifest(
                true, 1, Instant.now(), first.listingSourceUrl(),
                List.of(new OfficialCertificationImporter.PageEvidence(2, 1, 1, 1)), first.items()), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT name FROM certification WHERE id=?", String.class, id)).isEqualTo("New name");
    }

    @Test
    @DisplayName("내 분류는 소유한 시스템·공식 분류와 비어 있는 개인 분류만 포함한다")
    void ownedCategoriesUseServerOwnership() {
        var empty = service.create(Kind.CERTIFICATION, "Empty");
        assertThat(service.owned(Kind.CERTIFICATION)).extracting(PersonalCategoryService.Category::code)
                .contains("CLOUD").doesNotContain("SECURITY");
        importer.importManifest(officialManifest("Technical qualification"), true);
        Long officialId = jdbc.queryForObject("SELECT id FROM certification WHERE source_code='T001'", Long.class);
        jdbc.update("INSERT INTO player_certifications (player_id, certification_id, granted_at, created_at, updated_at) VALUES (101, ?, NOW(6), NOW(6), NOW(6))", officialId);
        assertThat(service.owned(Kind.CERTIFICATION)).extracting(PersonalCategoryService.Category::code)
                .contains("HRDK:21:211");
        assertThat(service.owned(Kind.CERTIFICATION)).extracting(PersonalCategoryService.Category::id)
                .contains(empty.id());
    }

    @Test
    @DisplayName("카탈로그 페이지는 보유 상태를 현재 소유자로 계산하고 비활성 정의의 소유 기록은 보존한다")
    void catalogReadAndDeactivation() {
        var page = catalog.search(Kind.HOBBY, "Hobby", "ARTS", null, 0, 10);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items().getFirst().owned()).isTrue();
        assertThat(page.items().getFirst().ownedItemId()).isNotNull();
        assertThat(page.items().getFirst().catalogItemId()).isEqualTo(301L);
        assertThat(page.items().getFirst().source()).isEqualTo("LEGACY");
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(102L);
        assertThat(catalog.get(Kind.HOBBY, 301L).owned()).isFalse();
        assertThat(catalog.get(Kind.HOBBY, 301L).ownedItemId()).isNull();
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(101L);
        jdbc.update("UPDATE hobbies SET active=0 WHERE id=301");
        assertThat(catalog.search(Kind.HOBBY, null, null, null, 0, 10).items()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM player_hobbies WHERE player_id=101 AND hobby_id=301", Long.class))
                .isEqualTo(1L);
    }

    private OfficialCertificationImporter.Manifest officialManifest(String name) {
        var entry = new OfficialCertificationImporter.Entry("T001", name, null, "21", "정보통신",
                "211", "정보기술", "한국산업인력공단", null, "BASIC",
                "https://www.data.go.kr/data/15003003/openapi.do");
        return new OfficialCertificationImporter.Manifest(true, 1, Instant.parse("2026-10-05T00:00:00Z"),
                "https://www.hrdkorea.or.kr/7/3/2",
                List.of(new OfficialCertificationImporter.PageEvidence(1, 1, 1, 1)), List.of(entry));
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
