package online.lifeasgame.economy.api.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.economy.application.EconomyFacade;
import online.lifeasgame.economy.application.ListingOpenService;
import online.lifeasgame.economy.application.MarketplaceService;
import online.lifeasgame.economy.application.ShopService;
import online.lifeasgame.economy.application.TopUpService;
import online.lifeasgame.economy.application.WalletReader;
import online.lifeasgame.economy.application.WalletWriter;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.port.PaymentGateway;
import online.lifeasgame.economy.domain.repository.WalletRepository;
import online.lifeasgame.economy.infra.TossPaymentGateway;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import online.lifeasgame.platform.security.jwt.JwtCurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtProperties;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EconomyController.class, properties = {
        "lifeasgame.web.cors.allowed-origins[0]=http://localhost:3000",
        "spring.test.mockmvc.print=NONE"
})
@ActiveProfiles("test")
@Import({SecurityConfig.class, WebMvcTestConfig.class, EconomyTopUpControllerTest.JwtTestConfig.class,
        EconomyFacade.class, TopUpService.class, TossPaymentGateway.class, WalletReader.class, WalletWriter.class})
@DisplayName("현재 플레이어 충전 HTTP 계약")
class EconomyTopUpControllerTest {

    private static final String PATH = "/api/v1/economy/top-up";
    private static final long USER_ID = 34401L;
    private static final long PLAYER_ID = 34402L;
    private static final Duration TTL = Duration.ofMinutes(10);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApplicationContext context;

    @MockitoSpyBean
    private TopUpService topUpService;
    @MockitoSpyBean
    private TossPaymentGateway gateway;

    @MockitoBean
    private UserAuthApi userAuthApi;
    @MockitoBean
    private WalletRepository repository;
    @MockitoBean
    private IdempotencyKeyStore idempotencyKeyStore;
    @MockitoBean
    private DomainEventPublisher publisher;
    @MockitoBean
    private MarketplaceService marketplaceService;
    @MockitoBean
    private ShopService shopService;
    @MockitoBean
    private ListingOpenService listingOpenService;
    @MockitoBean
    private AppErrorProperties appErrorProperties;
    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @ParameterizedTest
    @ValueSource(strings = {"GOLD", "GEM"})
    @DisplayName("JWT 플레이어의 미검증 요청은 부작용 없이 402 ProblemDetail로 응답한다")
    void rejectsAuthenticatedCharge(String currency) throws Exception {
        when(idempotencyKeyStore.acquire("top-up-A", TTL)).thenReturn(true);
        assertThat(context.getBeansOfType(PaymentGateway.class).values()).containsExactly(gateway);
        assertThat(context.getBeansOfType(CurrentPlayerAccessor.class).values())
                .singleElement().isExactlyInstanceOf(JwtCurrentPlayerAccessor.class);

        mockMvc.perform(authenticatedRequest(currency))
                .andExpect(status().isPaymentRequired())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.code").value("ECON-PAYMENT-REJECTED"))
                .andExpect(jsonPath("$.title").value("Payment gateway rejected"))
                .andExpect(jsonPath("$.path").value(PATH))
                .andExpect(jsonPath("$.instance", endsWith(PATH)))
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.isSuccess").doesNotExist());

        verify(topUpService).topUp(PLAYER_ID,
                new EconomyCommand.TopUp(100L, currency, "synthetic-payment", "synthetic-order", "top-up-A"));
        verifyNoInteractions(repository, publisher);
    }

    @Test
    @DisplayName("인증이 없으면 401이고 충전 처리에 진입하지 않는다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(request("GOLD"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(topUpService, gateway, idempotencyKeyStore, repository, publisher);
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "currency", "paymentKey", "orderId", "idempotencyKey"})
    @DisplayName("0 금액 또는 공백 필수 필드는 400이며 충전 처리에 진입하지 않는다")
    void validatesRequiredFields(String field) throws Exception {
        var body = objectMapper.readTree(body("GOLD"));
        if (field.equals("amount")) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) body).put(field, 0);
        } else {
            ((com.fasterxml.jackson.databind.node.ObjectNode) body).put(field, " ");
        }

        mockMvc.perform(authenticatedRequest("GOLD").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(topUpService, gateway, idempotencyKeyStore, repository, publisher);
    }

    @Test
    @DisplayName("거절 후 동일 키를 재사용하면 기존 409 중복 응답을 유지한다")
    void preservesDuplicateResponse() throws Exception {
        when(idempotencyKeyStore.acquire("top-up-A", TTL)).thenReturn(true, false);

        mockMvc.perform(authenticatedRequest("GOLD"))
                .andExpect(status().isPaymentRequired());
        mockMvc.perform(authenticatedRequest("GOLD"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ECON-DUPLICATE-REQUEST"));

        verify(gateway).confirmCharge("synthetic-payment", "synthetic-order", 100L,
                online.lifeasgame.economy.domain.Currency.GOLD);
        verifyNoInteractions(repository, publisher);
    }

    private MockHttpServletRequestBuilder authenticatedRequest(String currency) {
        when(userAuthApi.resolveAuthorization(USER_ID)).thenReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        return request(currency)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(USER_ID, PLAYER_ID));
    }

    private MockHttpServletRequestBuilder request(String currency) {
        return post(PATH).contentType(MediaType.APPLICATION_JSON).content(body(currency));
    }

    private String body(String currency) {
        return """
                {"amount":100,"currency":"%s","paymentKey":"synthetic-payment",
                 "orderId":"synthetic-order","idempotencyKey":"top-up-A"}
                """.formatted(currency);
    }

    @TestConfiguration
    static class JwtTestConfig {
        @Bean
        JwtProvider jwtProvider() {
            var properties = new JwtProperties();
            properties.setSecret("synthetic-top-up-test-secret-at-least-32-characters");
            properties.setAccessTokenExpiryMs(3_600_000L);
            return new JwtProvider(properties);
        }

        @Bean
        CurrentPlayerAccessor currentPlayerAccessor() {
            return new JwtCurrentPlayerAccessor();
        }
    }
}
