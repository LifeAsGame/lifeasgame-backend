package online.lifeasgame.inventory.api.admin;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.ItemQueryService;
import online.lifeasgame.inventory.application.ItemService;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.domain.error.ItemError;
import online.lifeasgame.platform.security.jwt.JwtProperties;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminItemController.class)
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class,
        AdminItemInspectionControllerTest.JwtTestConfig.class
})
@DisplayName("Admin Item inspection read contract")
class AdminItemInspectionControllerTest {

    private static final long USER_ID = 31201L;
    private static final long ADMIN_ID = 31202L;
    private static final String SECRET =
            "admin-item-read-test-secret-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserAuthApi userAuthApi;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private ItemQueryService itemQueryService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("canonical filters와 lower-bounded page를 전달하고 page metadata를 반환한다")
    void searchesWithBoundedPage() throws Exception {
        allowAdmin();
        given(itemQueryService.search(
                eq("Potion"),
                eq("CONSUMABLE"),
                eq("POTION"),
                eq("RARE"),
                any(Pageable.class)
        )).willReturn(new ItemResult.Page<>(
                List.of(summary()),
                0,
                1,
                3,
                3
        ));

        mockMvc.perform(get("/admin/v1/items")
                        .header("Authorization", bearer(ADMIN_ID))
                        .param("name", "Potion")
                        .param("category", "CONSUMABLE")
                        .param("type", "POTION")
                        .param("rarity", "RARE")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].id").value(3120))
                .andExpect(jsonPath("$.result.content[0].code").value("POTION_312"))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(1))
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.totalPages").value(3));

        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(itemQueryService).search(
                eq("Potion"),
                eq("CONSUMABLE"),
                eq("POTION"),
                eq("RARE"),
                pageable.capture()
        );
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("search size를 100으로 clamp한다")
    void clampsMaximumPageSize() throws Exception {
        allowAdmin();
        given(itemQueryService.search(
                eq(null), eq(null), eq(null), eq(null), any(Pageable.class)
        )).willReturn(new ItemResult.Page<>(List.of(), 0, 100, 0, 0));

        mockMvc.perform(get("/admin/v1/items")
                        .header("Authorization", bearer(ADMIN_ID))
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.size").value(100));

        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(itemQueryService).search(
                eq(null), eq(null), eq(null), eq(null), pageable.capture()
        );
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("exact requested Item detail과 canonical baseAttrs를 반환한다")
    void getsExactItem() throws Exception {
        allowAdmin();
        given(itemQueryService.getItem(3120L)).willReturn(detail(3120L));

        mockMvc.perform(get("/admin/v1/items/3120")
                        .header("Authorization", bearer(ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(3120))
                .andExpect(jsonPath("$.result.code").value("POTION_312"))
                .andExpect(jsonPath("$.result.name").value("Potion"))
                .andExpect(jsonPath("$.result.category").value("CONSUMABLE"))
                .andExpect(jsonPath("$.result.type").value("POTION"))
                .andExpect(jsonPath("$.result.rarity").value("RARE"))
                .andExpect(jsonPath("$.result.stackable").value(true))
                .andExpect(jsonPath("$.result.maxStack").value(20))
                .andExpect(jsonPath("$.result.maxDurability").value(100))
                .andExpect(jsonPath("$.result.baseAttrs.attack").value(7));

        verify(itemQueryService).getItem(3120L);
    }

    @Test
    @DisplayName("Item detail은 positive id만 허용한다")
    void rejectsInvalidItemId() throws Exception {
        allowAdmin();

        mockMvc.perform(get("/admin/v1/items/0")
                        .header("Authorization", bearer(ADMIN_ID)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemQueryService);
    }

    @Test
    @DisplayName("provider Item not-found를 404로 전파한다")
    void propagatesNotFound() throws Exception {
        allowAdmin();
        given(itemQueryService.getItem(404L))
                .willThrow(new DomainException(ItemError.ITEM_NOT_FOUND));

        mockMvc.perform(get("/admin/v1/items/404")
                        .header("Authorization", bearer(ADMIN_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("provider detail id가 요청 id와 다르면 fail closed한다")
    void rejectsIdentityDrift() throws Exception {
        allowAdmin();
        given(itemQueryService.getItem(3120L)).willReturn(detail(3121L));

        mockMvc.perform(get("/admin/v1/items/3120")
                        .header("Authorization", bearer(ADMIN_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("persisted USER authority는 새 read를 403으로 거부한다")
    void rejectsUser() throws Exception {
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false))
        );

        mockMvc.perform(get("/admin/v1/items")
                        .header("Authorization", bearer(USER_ID)))
                .andExpect(status().isForbidden());

        verify(itemQueryService, never()).search(
                any(), any(), any(), any(), any()
        );
    }

    private ItemResult.Summary summary() {
        return new ItemResult.Summary(
                3120L,
                "POTION_312",
                "Potion",
                "CONSUMABLE",
                "POTION",
                "RARE",
                true,
                20
        );
    }

    private ItemResult.Detail detail(Long id) {
        return new ItemResult.Detail(
                id,
                "POTION_312",
                "Potion",
                "CONSUMABLE",
                "POTION",
                "RARE",
                true,
                20,
                100,
                Map.of("attack", 7)
        );
    }

    private void allowAdmin() {
        given(userAuthApi.resolveAuthorization(ADMIN_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true))
        );
    }

    private String bearer(long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId, null);
    }

    private static JwtProvider provider() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpiryMs(3_600_000L);
        properties.setRefreshTokenExpiryMs(604_800_000L);
        return new JwtProvider(properties);
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtProvider jwtProvider() {
            return provider();
        }
    }
}
