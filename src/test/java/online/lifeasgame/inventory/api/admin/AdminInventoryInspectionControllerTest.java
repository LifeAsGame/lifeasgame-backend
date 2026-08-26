package online.lifeasgame.inventory.api.admin;

import online.lifeasgame.inventory.application.AdminInventoryEntitlementService;
import online.lifeasgame.inventory.application.InventoryQueryService;
import online.lifeasgame.inventory.application.MailboxQueryService;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.platform.security.jwt.JwtProperties;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminInventoryController.class, AdminMailboxController.class})
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class,
        AdminInventoryInspectionControllerTest.JwtTestConfig.class
})
@DisplayName("Admin Inventory와 Mailbox inspection read contract")
class AdminInventoryInspectionControllerTest {

    private static final long PLAYER_ID = 312L;
    private static final long ADMIN_ID = 31202L;
    private static final String SECRET =
            "admin-inventory-read-test-secret-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserAuthApi userAuthApi;

    @MockitoBean
    private AdminInventoryEntitlementService entitlementService;

    @MockitoBean
    private InventoryQueryService inventoryQueryService;

    @MockitoBean
    private MailboxQueryService mailboxQueryService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @BeforeEach
    void setUp() {
        given(userAuthApi.resolveAuthorization(ADMIN_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true))
        );
    }

    @Test
    @DisplayName("path playerId의 Inventory를 bounded fields로 반환한다")
    void listsInventoryForPathPlayer() throws Exception {
        given(inventoryQueryService.list(PLAYER_ID)).willReturn(
                new InventoryResult.Entries(List.of(new InventoryResult.Entry(
                        9001L,
                        2,
                        3100L,
                        "Iron Sword",
                        "WEAPON",
                        "SWORD",
                        "RARE",
                        false,
                        1,
                        1,
                        true,
                        85,
                        Map.of("private", "raw")
                )))
        );

        mockMvc.perform(get("/admin/v1/players/312/inventory")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.playerId").value(PLAYER_ID))
                .andExpect(jsonPath("$.result.entries[0].itemInstanceId").value(9001))
                .andExpect(jsonPath("$.result.entries[0].slotIndex").value(2))
                .andExpect(jsonPath("$.result.entries[0].itemId").value(3100))
                .andExpect(jsonPath("$.result.entries[0].itemName").value("Iron Sword"))
                .andExpect(jsonPath("$.result.entries[0].category").value("WEAPON"))
                .andExpect(jsonPath("$.result.entries[0].type").value("SWORD"))
                .andExpect(jsonPath("$.result.entries[0].rarity").value("RARE"))
                .andExpect(jsonPath("$.result.entries[0].stackable").value(false))
                .andExpect(jsonPath("$.result.entries[0].maxStack").value(1))
                .andExpect(jsonPath("$.result.entries[0].quantity").value(1))
                .andExpect(jsonPath("$.result.entries[0].bound").value(true))
                .andExpect(jsonPath("$.result.entries[0].durability").value(85))
                .andExpect(jsonPath("$.result.entries[0].instanceAttrs").doesNotExist());

        verify(inventoryQueryService).list(PLAYER_ID);
        verify(inventoryQueryService, never()).list();
    }

    @Test
    @DisplayName("path playerId의 Mailbox를 bounded fields로 반환한다")
    void listsMailboxForPathPlayer() throws Exception {
        given(mailboxQueryService.list(PLAYER_ID)).willReturn(
                new MailboxResult.Entries(List.of(new MailboxResult.Entry(
                        8001L,
                        3,
                        3200L,
                        "Silver Ring",
                        "ACCESSORY",
                        "RING",
                        "EPIC",
                        false,
                        1,
                        1,
                        false,
                        null,
                        Map.of("private", "raw")
                )))
        );

        mockMvc.perform(get("/admin/v1/players/312/mailbox")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.playerId").value(PLAYER_ID))
                .andExpect(jsonPath("$.result.entries[0].mailId").value(8001))
                .andExpect(jsonPath("$.result.entries[0].slotIndex").value(3))
                .andExpect(jsonPath("$.result.entries[0].itemId").value(3200))
                .andExpect(jsonPath("$.result.entries[0].itemName").value("Silver Ring"))
                .andExpect(jsonPath("$.result.entries[0].category").value("ACCESSORY"))
                .andExpect(jsonPath("$.result.entries[0].type").value("RING"))
                .andExpect(jsonPath("$.result.entries[0].rarity").value("EPIC"))
                .andExpect(jsonPath("$.result.entries[0].stackable").value(false))
                .andExpect(jsonPath("$.result.entries[0].maxStack").value(1))
                .andExpect(jsonPath("$.result.entries[0].quantity").value(1))
                .andExpect(jsonPath("$.result.entries[0].bound").value(false))
                .andExpect(jsonPath("$.result.entries[0].durability").doesNotExist())
                .andExpect(jsonPath("$.result.entries[0].instanceAttrs").doesNotExist());

        verify(mailboxQueryService).list(PLAYER_ID);
        verify(mailboxQueryService, never()).list();
    }

    @Test
    @DisplayName("Inventory와 Mailbox path playerId는 positive 값만 허용한다")
    void rejectsInvalidPlayerId() throws Exception {
        mockMvc.perform(get("/admin/v1/players/0/inventory")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/admin/v1/players/-1/mailbox")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryQueryService, mailboxQueryService);
    }

    private String bearer() {
        return "Bearer " + jwtProvider.createAccessToken(ADMIN_ID, null);
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
