package online.lifeasgame.inventory.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.ItemLookupApi;
import online.lifeasgame.inventory.domain.error.ItemError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("ItemLookupApi integration")
class ItemLookupIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_item_lookup")
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
    private ItemLookupApi itemLookupApi;

    @Test
    @DisplayName("V12 Seed를 stable code로 조회해 양수 ID와 정확한 code를 반환한다")
    void getsSeedByStableCode() {
        ItemLookupApi.ItemReference reference =
                itemLookupApi.getByCode("  IT_FIRST_STEP_FRAGMENT  ");

        assertThat(reference.id()).isPositive();
        assertThat(reference.code()).isEqualTo("IT_FIRST_STEP_FRAGMENT");
    }

    @Test
    @DisplayName("알 수 없는 stable code는 ItemError로 실패한다")
    void rejectsUnknownStableCode() {
        assertThatThrownBy(() -> itemLookupApi.getByCode("IT_UNKNOWN"))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ItemError.ITEM_CODE_NOT_FOUND)
                );
    }
}
