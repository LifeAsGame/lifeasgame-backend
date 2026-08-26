package online.lifeasgame.inventory.api.admin;

import online.lifeasgame.inventory.api.admin.request.AdminInventoryRequest;
import online.lifeasgame.inventory.api.admin.request.AdminMailboxRequest;
import online.lifeasgame.inventory.application.AdminInventoryEntitlementService;
import online.lifeasgame.inventory.application.InventoryQueryService;
import online.lifeasgame.inventory.application.MailboxQueryService;
import online.lifeasgame.inventory.application.command.AdminInventoryEntitlementCommand;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminInventoryController.class, AdminMailboxController.class})
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class,
        AdminInventoryEntitlementControllerTest.JwtTestConfig.class
})
@DisplayName("Admin Inventory entitlement controller contract")
class AdminInventoryEntitlementControllerTest {

    private static final long PLAYER_ID = 310L;
    private static final long USER_ID = 31001L;
    private static final long ADMIN_ID = 31002L;
    private static final String SECRET =
            "admin-entitlement-test-secret-at-least-32-characters";

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
        given(entitlementService.addToInventory(any())).willReturn(
                new InventoryResult.Slots(List.of(0))
        );
        given(entitlementService.deliverToMailbox(any())).willReturn(
                new MailboxResult.Slot(1)
        );
    }

    @Test
    @DisplayName("두 body는 arbitrary instanceAttrs 없이 entitlement와 reason만 노출한다")
    void exposesBoundedBodies() {
        assertThat(componentNames(AdminInventoryRequest.Add.class))
                .containsExactly("itemId", "quantity", "bound", "reason");
        assertThat(componentNames(AdminMailboxRequest.Deliver.class))
                .containsExactly("itemId", "quantity", "bound", "reason");
    }

    @Test
    @DisplayName("ADMIN 요청은 두 entitlement command에 header metadata를 전달한다")
    void allowsAdminEntitlements() throws Exception {
        allowAdmin();

        mockMvc.perform(inventoryRequest(ADMIN_ID)
                        .header("Idempotency-Key", "inventory-310")
                        .header("X-Correlation-Id", "request-inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.slots[0]").value(0));
        mockMvc.perform(mailboxRequest(ADMIN_ID)
                        .header("Idempotency-Key", "mailbox-310")
                        .header("X-Correlation-Id", "request-mailbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.slot").value(1));

        var inventory = org.mockito.ArgumentCaptor.forClass(
                AdminInventoryEntitlementCommand.AddToInventory.class
        );
        var mailbox = org.mockito.ArgumentCaptor.forClass(
                AdminInventoryEntitlementCommand.DeliverToMailbox.class
        );
        verify(entitlementService).addToInventory(inventory.capture());
        verify(entitlementService).deliverToMailbox(mailbox.capture());
        assertThat(inventory.getValue()).satisfies(command -> {
            assertThat(command.playerId()).isEqualTo(PLAYER_ID);
            assertThat(command.itemId()).isEqualTo(3100L);
            assertThat(command.quantity()).isEqualTo(2);
            assertThat(command.bound()).isTrue();
            assertThat(command.reason()).isEqualTo("CASE-310-INVENTORY");
            assertThat(command.idempotencyKey()).isEqualTo("inventory-310");
            assertThat(command.correlationId()).isEqualTo("request-inventory");
        });
        assertThat(mailbox.getValue()).satisfies(command -> {
            assertThat(command.playerId()).isEqualTo(PLAYER_ID);
            assertThat(command.itemId()).isEqualTo(3100L);
            assertThat(command.quantity()).isEqualTo(1);
            assertThat(command.bound()).isTrue();
            assertThat(command.reason()).isEqualTo("CASE-310-MAILBOX");
            assertThat(command.idempotencyKey()).isEqualTo("mailbox-310");
            assertThat(command.correlationId()).isEqualTo("request-mailbox");
        });
    }

    @Test
    @DisplayName("두 command 모두 Idempotency-Key가 없으면 400이다")
    void requiresIdempotencyKey() throws Exception {
        allowAdmin();

        mockMvc.perform(inventoryRequest(ADMIN_ID))
                .andExpect(status().isBadRequest());
        mockMvc.perform(mailboxRequest(ADMIN_ID))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(entitlementService);
    }

    @Test
    @DisplayName("spec-owned playerId constraint는 0과 음수를 400으로 거부한다")
    void rejectsInvalidPlayerId() throws Exception {
        allowAdmin();

        mockMvc.perform(post("/admin/v1/players/0/inventory/add")
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "inventory-310")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2, "CASE-310-INVENTORY")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/admin/v1/players/-1/mailbox/deliver")
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "mailbox-310")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1, "CASE-310-MAILBOX")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(entitlementService);
    }

    @Test
    @DisplayName("두 command 모두 unsafe reason과 header를 400으로 거부한다")
    void rejectsUnsafeMetadata() throws Exception {
        allowAdmin();

        mockMvc.perform(inventoryRequest(ADMIN_ID)
                        .header("Idempotency-Key", "unsafe key"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(inventoryPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "inventory-310")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2, "CASE-310\u202Eprivate")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(mailboxPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "mailbox-format")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1, "CASE-310\u202Eprivate")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(mailboxPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "mailbox-310")
                        .header("X-Correlation-Id", "unsafe correlation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1, "CASE-310-MAILBOX")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(entitlementService);
    }

    @Test
    @DisplayName("spec-owned body cascade는 invalid itemId quantity와 reason을 400으로 거부한다")
    void rejectsInvalidBody() throws Exception {
        allowAdmin();

        mockMvc.perform(post(inventoryPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "invalid-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(0L, 2, "CASE-310-INVENTORY")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(mailboxPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "invalid-quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(3100L, 0, "CASE-310-MAILBOX")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(inventoryPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "invalid-reason")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(3100L, 2, " ")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(entitlementService);
    }

    @Test
    @DisplayName("두 endpoint는 legacy non-null instanceAttrs를 400으로 거부한다")
    void rejectsLegacyInstanceAttrs() throws Exception {
        allowAdmin();

        mockMvc.perform(post(inventoryPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "inventory-legacy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(legacyBody(2, "CASE-310-INVENTORY")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(mailboxPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "mailbox-legacy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(legacyBody(1, "CASE-310-MAILBOX")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(entitlementService);
    }

    @Test
    @DisplayName("persisted USER authority는 entitlement를 403으로 거부한다")
    void rejectsUser() throws Exception {
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false))
        );

        mockMvc.perform(inventoryRequest(USER_ID)
                        .header("Idempotency-Key", "inventory-user"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(entitlementService);
    }

    @Test
    @DisplayName("/api/v1/admin alias는 존재하지 않는다")
    void hasNoAlias() throws Exception {
        allowAdmin();

        mockMvc.perform(post(
                        "/api/v1/admin/players/310/inventory/add"
                )
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "inventory-310")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2, "CASE-310")))
                .andExpect(status().isNotFound());
    }

    private List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName())
                .toList();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    inventoryRequest(long userId) {
        return post(inventoryPath())
                .header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(2, "CASE-310-INVENTORY"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    mailboxRequest(long userId) {
        return post(mailboxPath())
                .header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(1, "CASE-310-MAILBOX"));
    }

    private String inventoryPath() {
        return "/admin/v1/players/310/inventory/add";
    }

    private String mailboxPath() {
        return "/admin/v1/players/310/mailbox/deliver";
    }

    private String body(int quantity, String reason) {
        return body(3100L, quantity, reason);
    }

    private String body(long itemId, int quantity, String reason) {
        return """
                {
                  "itemId": %d,
                  "quantity": %d,
                  "bound": true,
                  "reason": "%s"
                }
                """.formatted(itemId, quantity, reason);
    }

    private String legacyBody(int quantity, String reason) {
        return """
                {
                  "itemId": 3100,
                  "quantity": %d,
                  "bound": true,
                  "reason": "%s",
                  "instanceAttrs": {"legacy": "value"}
                }
                """.formatted(quantity, reason);
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
