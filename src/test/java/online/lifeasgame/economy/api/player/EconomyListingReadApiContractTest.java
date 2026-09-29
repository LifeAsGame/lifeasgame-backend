package online.lifeasgame.economy.api.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.economy.api.player.response.EconomyResponse;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.economy.application.EconomyFacade;
import online.lifeasgame.economy.application.ListingOpenService;
import online.lifeasgame.economy.application.ListingReader;
import online.lifeasgame.economy.application.ListingReservationReader;
import online.lifeasgame.economy.application.ListingReservationWriter;
import online.lifeasgame.economy.application.ListingWriter;
import online.lifeasgame.economy.application.MarketplaceService;
import online.lifeasgame.economy.application.ShopService;
import online.lifeasgame.economy.application.TopUpService;
import online.lifeasgame.economy.application.TradeReader;
import online.lifeasgame.economy.application.TradeWriter;
import online.lifeasgame.economy.application.WalletQueryService;
import online.lifeasgame.economy.application.WalletReader;
import online.lifeasgame.economy.application.WalletWriter;
import online.lifeasgame.economy.domain.Currency;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.ListingReservation;
import online.lifeasgame.economy.domain.ListingReservationState;
import online.lifeasgame.economy.domain.ListingStatus;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.repository.MarketplacePurchaseReceiptRepository;
import online.lifeasgame.inventory.application.internal.InventoryMarketAvailabilityApi;
import online.lifeasgame.inventory.application.internal.InventoryMarketTransferApi;
import online.lifeasgame.platform.security.jwt.JwtCurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EconomyController.class, properties = {
        "lifeasgame.web.cors.allowed-origins[0]=http://localhost:3000",
        "spring.test.mockmvc.print=NONE"
})
@ActiveProfiles("test")
@Import({SecurityConfig.class, WebMvcTestConfig.class, EconomyListingReadApiContractTest.PlayerConfig.class,
        EconomyFacade.class, MarketplaceService.class})
@DisplayName("매물 snapshot 수량 조회 HTTP 계약")
class EconomyListingReadApiContractTest {

    private static final long PLAYER_ID = 37801L;
    private static final long LISTING_ID = 37802L;
    private static final long ITEM_ID = 37803L;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private ListingReader listingReader;
    @MockitoBean
    private ListingWriter listingWriter;
    @MockitoBean
    private ListingReservationReader reservationReader;
    @MockitoBean
    private ListingReservationWriter reservationWriter;
    @MockitoBean
    private TradeReader tradeReader;
    @MockitoBean
    private TradeWriter tradeWriter;
    @MockitoBean
    private WalletReader walletReader;
    @MockitoBean
    private WalletWriter walletWriter;
    @MockitoBean
    private InventoryMarketAvailabilityApi inventoryAvailability;
    @MockitoBean
    private InventoryMarketTransferApi inventoryTransfer;
    @MockitoBean
    private MarketplacePurchaseReceiptRepository receipts;
    @MockitoBean
    private DomainEventPublisher events;
    @MockitoBean
    private ListingOpenService listingOpenService;
    @MockitoBean
    private ShopService shopService;
    @MockitoBean
    private TopUpService topUpService;
    @MockitoBean
    private WalletQueryService walletQueryService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private AppErrorProperties appErrorProperties;
    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @ParameterizedTest(name = "{0}: saleQuantity={1}")
    @CsvSource(value = {
            "listings, 1", "listings, 7", "listings, null",
            "listings/me, 1", "listings/me, 7", "listings/me, null",
            "listings/reservations, 1", "listings/reservations, 7", "listings/reservations, null"
    }, nullValues = "null")
    @DisplayName("실제 조회 경로는 저장된 수량 또는 명시적 null을 부작용 없이 전달한다")
    void exposesSnapshot(String path, Integer quantity) throws Exception {
        Listing listing = listing(quantity, path.equals("listings/me") ? PLAYER_ID : 999L);
        ListingReservation reservation = ListingReservation.active(
                LISTING_ID, PLAYER_ID, "synthetic-hold", Instant.parse("2026-09-29T00:00:00Z"), 60);
        boolean reservationPath = path.endsWith("reservations");
        switch (path) {
            case "listings" -> when(listingReader.listOpen()).thenReturn(List.of(listing));
            case "listings/me" -> when(listingReader.listBySeller(PLAYER_ID)).thenReturn(List.of(listing));
            case "listings/reservations" -> {
                when(reservationReader.listActiveByBuyer(PLAYER_ID)).thenReturn(List.of(reservation));
                when(listingReader.get(LISTING_ID)).thenReturn(listing);
            }
            default -> throw new AssertionError(path);
        }
        if (!reservationPath) {
            when(reservationReader.findActiveListingIds(List.of(LISTING_ID))).thenReturn(Set.of());
        }
        String array = reservationPath ? "reservations" : "listings";
        var response = mvc.perform(authenticatedGet(path).param("playerId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.result." + array + ".length()").value(1))
                .andExpect(jsonPath("$.result." + array + "[0].itemId").value(ITEM_ID))
                .andExpect(jsonPath("$.result." + array + "[0].price").value(40))
                .andExpect(jsonPath("$.result." + array + "[0].currency").value("GOLD"))
                .andReturn().getResponse();
        var row = objectMapper.readTree(response.getContentAsString()).path("result").path(array).get(0);
        assertThat(row.has("saleQuantity")).as("saleQuantity must also be present when unknown").isTrue();
        if (quantity == null) {
            assertThat(row.get("saleQuantity").isNull()).isTrue();
        } else {
            assertThat(row.get("saleQuantity").intValue()).isEqualTo(quantity);
        }
        if (reservationPath) {
            verify(reservationReader).listActiveByBuyer(PLAYER_ID);
            assertThat(row.path("listingId").longValue()).isEqualTo(LISTING_ID);
            assertThat(Instant.parse(row.path("expiresAt").textValue())).isEqualTo(reservation.getExpiresAt());
        } else {
            if (path.endsWith("me")) verify(listingReader).listBySeller(PLAYER_ID);
            assertThat(row.path("id").longValue()).isEqualTo(LISTING_ID);
            assertThat(row.path("status").textValue()).isEqualTo("OPEN");
        }
        assertThat(listing.getSaleQuantity()).isEqualTo(quantity);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.OPEN);
        assertThat(reservation.getState()).isEqualTo(ListingReservationState.ACTIVE);
        verifyNoInteractions(listingWriter, reservationWriter, tradeWriter, walletReader, walletWriter,
                inventoryAvailability, inventoryTransfer, receipts, events);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "RESERVED", "SOLD", "CANCELED"})
    @DisplayName("내 매물의 상태가 바뀌어도 등록 시 수량을 유지한다")
    void preservesQuantityAcrossStates(String state) throws Exception {
        Listing listing = listing(7, PLAYER_ID);
        if (state.equals("SOLD")) listing.sellTo(999L);
        if (state.equals("CANCELED")) listing.cancel(PLAYER_ID);
        when(listingReader.listBySeller(PLAYER_ID)).thenReturn(List.of(listing));
        when(reservationReader.findActiveListingIds(
                listing.getStatus() == ListingStatus.OPEN ? List.of(LISTING_ID) : List.of()))
                .thenReturn(state.equals("RESERVED") ? Set.of(LISTING_ID) : Set.of());

        mvc.perform(authenticatedGet("listings/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.listings[0].status").value(state))
                .andExpect(jsonPath("$.result.listings[0].saleQuantity").value(7));
        verifyNoInteractions(listingWriter, reservationWriter, tradeWriter, walletWriter,
                inventoryAvailability, inventoryTransfer, receipts, events);
    }

    @ParameterizedTest
    @ValueSource(strings = {"listings", "listings/me", "listings/reservations"})
    @DisplayName("인증 없이는 401이며 매물·예약을 조회하지 않는다")
    void requiresAuthentication(String path) throws Exception {
        mvc.perform(get("/api/v1/economy/" + path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(listingReader, reservationReader);
    }

    @ParameterizedTest
    @MethodSource("quantityResponses")
    @DisplayName("OpenAPI는 항상 포함되는 nullable int32 수량과 미확인 의미를 문서화한다")
    void documentsQuantity(Class<?> responseType) {
        io.swagger.v3.oas.models.media.Schema<?> schema = io.swagger.v3.core.converter.ModelConverters.getInstance()
                .readAll(responseType).get(responseType.getSimpleName());
        var quantity = schema.getProperties().get("saleQuantity");
        assertThat(quantity.getType()).isEqualTo("integer");
        assertThat(quantity.getFormat()).isEqualTo("int32");
        assertThat(quantity.getNullable()).isTrue();
        assertThat(quantity.getMinimum()).isEqualByComparingTo("1");
        assertThat(quantity.getDescription()).contains("snapshot", "null", "미확인");
        assertThat(schema.getRequired()).contains("saleQuantity");
    }

    static List<Class<?>> quantityResponses() {
        return List.of(EconomyResponse.ListingSummary.class, EconomyResponse.ListingReservation.class);
    }

    private Listing listing(Integer quantity, long sellerId) {
        Listing listing = Listing.open(sellerId, 37804L, ITEM_ID,
                quantity == null ? 1 : quantity, Money.of(40, Currency.GOLD));
        ReflectionTestUtils.setField(listing, "id", LISTING_ID);
        if (quantity == null) ReflectionTestUtils.setField(listing, "saleQuantity", null);
        return listing;
    }

    private MockHttpServletRequestBuilder authenticatedGet(String path) {
        return get("/api/v1/economy/" + path).with(authentication(
                new UsernamePasswordAuthenticationToken(new JwtPrincipal(37800L, PLAYER_ID), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))));
    }

    @TestConfiguration
    static class PlayerConfig {
        @Bean
        CurrentPlayerAccessor currentPlayerAccessor() {
            return new JwtCurrentPlayerAccessor();
        }
    }
}
