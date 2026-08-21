package online.lifeasgame.economy.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.domain.Currency;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.ListingReservation;
import online.lifeasgame.economy.domain.ListingReservationState;
import online.lifeasgame.economy.domain.ListingStatus;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.Wallet;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.inventory.application.internal.InventoryMarketAvailabilityApi;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingReservation 생명주기")
class MarketplaceReservationLifecycleTest {

    private static final long LISTING_ID = 296L;
    private static final long SELLER_ID = 10L;
    private static final long BUYER_ID = 20L;
    private static final long ENTRY_ID = 30L;

    @Mock
    private ListingReader listingReader;
    @Mock
    private ListingWriter listingWriter;
    @Mock
    private ListingReservationReader reservationReader;
    @Mock
    private ListingReservationWriter reservationWriter;
    @Mock
    private TradeWriter tradeWriter;
    @Mock
    private TradeReader tradeReader;
    @Mock
    private WalletWriter walletWriter;
    @Mock
    private WalletReader walletReader;
    @Mock
    private InventoryMarketAvailabilityApi inventoryApi;
    @Mock
    private IdempotencyKeyStore idempotencyKeyStore;
    @Mock
    private DomainEventPublisher eventPublisher;
    @InjectMocks
    private MarketplaceService service;

    @Nested
    @DisplayName("구매자가 OPEN Listing을 예약하면")
    class Reserve {

        @Test
        @DisplayName("Listing은 OPEN으로 두고 별도 ACTIVE 예약과 Inventory 예약을 만든다")
        void createsDistinctReservation() {
            Listing listing = listing();
            Wallet wallet = fundedWallet();
            given(listingReader.getForUpdate(LISTING_ID)).willReturn(listing);
            given(reservationReader.findActiveForUpdate(LISTING_ID)).willReturn(Optional.empty());
            given(walletReader.getByOwnerIdForUpdate(BUYER_ID)).willReturn(Optional.of(wallet));
            given(reservationWriter.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            service.reserve(BUYER_ID, new EconomyCommand.ReserveListing(LISTING_ID, 60));

            ArgumentCaptor<ListingReservation> reservation = ArgumentCaptor.forClass(ListingReservation.class);
            verify(reservationWriter).save(reservation.capture());
            assertThat(reservation.getValue().getState()).isEqualTo(ListingReservationState.ACTIVE);
            assertThat(reservation.getValue().getListingId()).isEqualTo(LISTING_ID);
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.OPEN);
            assertThat(listing.getReservedBy()).isNull();
            verify(inventoryApi).reserveForTrade(SELLER_ID, ENTRY_ID);
        }
    }

    @Nested
    @DisplayName("구매자가 Listing 구매를 완료하면")
    class Purchase {

        @Test
        @DisplayName("canonical ACTIVE 예약을 소비하고 Inventory transfer를 시작한 뒤 Listing을 판매 완료한다")
        void purchasesCanonicalReservation() {
            Listing listing = listing();
            Wallet buyerWallet = fundedWallet();
            Wallet sellerWallet = Wallet.open(SELLER_ID);
            Instant now = Instant.now();
            String holdId = buyerWallet.placeHold(listing.getPrice(), "test", now, 60);
            ListingReservation reservation = ListingReservation.active(
                    LISTING_ID,
                    BUYER_ID,
                    holdId,
                    now,
                    60
            );
            given(idempotencyKeyStore.acquire(any(), any())).willReturn(true);
            given(listingReader.getForUpdate(LISTING_ID)).willReturn(listing);
            given(reservationReader.findActiveForUpdate(LISTING_ID)).willReturn(Optional.of(reservation));
            given(walletReader.getByOwnerIdForUpdate(BUYER_ID)).willReturn(Optional.of(buyerWallet));
            given(walletReader.getByOwnerIdForUpdate(SELLER_ID)).willReturn(Optional.of(sellerWallet));
            given(tradeWriter.create(any())).willAnswer(invocation -> invocation.getArgument(0));

            service.purchase(BUYER_ID, new EconomyCommand.PurchaseListing(
                    LISTING_ID,
                    reservation.getReservationToken(),
                    "purchase-296"
            ));

            assertThat(reservation.getState()).isEqualTo(ListingReservationState.CONSUMED);
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.SOLD);
            verify(inventoryApi).beginTransfer(SELLER_ID, ENTRY_ID);
            verify(listingWriter).create(listing);
            verify(tradeWriter).create(any());
        }

        @Test
        @DisplayName("canonical OPEN Listing에 ACTIVE 예약이 없으면 아무 상태도 변경하지 않고 거절한다")
        void rejectsCanonicalDirectPurchase() {
            Listing listing = listing();
            given(idempotencyKeyStore.acquire(any(), any())).willReturn(true);
            given(listingReader.getForUpdate(LISTING_ID)).willReturn(listing);
            given(reservationReader.findActiveForUpdate(LISTING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.purchase(
                    BUYER_ID,
                    new EconomyCommand.PurchaseListing(
                            LISTING_ID,
                            null,
                            "direct-296"
                    )
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(EconomyError.LISTING_NOT_AVAILABLE)
            );

            assertThat(listing.getStatus()).isEqualTo(ListingStatus.OPEN);
            verifyNoInteractions(
                    walletReader,
                    walletWriter,
                    listingWriter,
                    tradeWriter,
                    inventoryApi
            );
        }

        @Test
        @DisplayName("snapshot이 없는 legacy OPEN Listing만 기존 direct purchase를 유지한다")
        void preservesLegacyDirectPurchase() {
            Listing listing = listing();
            ReflectionTestUtils.setField(listing, "saleQuantity", null);
            Wallet buyerWallet = fundedWallet();
            Wallet sellerWallet = Wallet.open(SELLER_ID);
            given(idempotencyKeyStore.acquire(any(), any())).willReturn(true);
            given(listingReader.getForUpdate(LISTING_ID)).willReturn(listing);
            given(reservationReader.findActiveForUpdate(LISTING_ID)).willReturn(Optional.empty());
            given(walletReader.getByOwnerIdForUpdate(BUYER_ID)).willReturn(Optional.of(buyerWallet));
            given(walletReader.getByOwnerIdForUpdate(SELLER_ID)).willReturn(Optional.of(sellerWallet));
            given(tradeWriter.create(any())).willAnswer(invocation -> invocation.getArgument(0));

            service.purchase(BUYER_ID, new EconomyCommand.PurchaseListing(
                    LISTING_ID,
                    null,
                    "legacy-296"
            ));

            assertThat(listing.getStatus()).isEqualTo(ListingStatus.SOLD);
            verifyNoInteractions(inventoryApi);
            verify(tradeWriter).create(any());
        }
    }

    @Nested
    @DisplayName("ACTIVE 예약이 만료되면")
    class Expire {

        @Test
        @DisplayName("hold와 Inventory 예약만 해제하고 Listing은 OPEN으로 유지한다")
        void releasesReservationOnly() {
            Listing listing = listing();
            Wallet wallet = fundedWallet();
            Instant createdAt = Instant.now().minusSeconds(30);
            String holdId = wallet.placeHold(Money.of(40L, Currency.GOLD), "test", createdAt, 1);
            ListingReservation reservation = ListingReservation.active(
                    LISTING_ID,
                    BUYER_ID,
                    holdId,
                    createdAt,
                    1
            );
            given(reservationReader.findActiveListingIdsExpiringBefore(any())).willReturn(List.of(LISTING_ID));
            given(listingReader.getForUpdate(LISTING_ID)).willReturn(listing);
            given(reservationReader.findActiveForUpdate(LISTING_ID)).willReturn(Optional.of(reservation));
            given(walletReader.getByOwnerIdForUpdate(BUYER_ID)).willReturn(Optional.of(wallet));

            service.expireReservations();

            assertThat(reservation.getState()).isEqualTo(ListingReservationState.EXPIRED);
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.OPEN);
            assertThat(wallet.getBalance(Currency.GOLD).available()).isEqualTo(100L);
            verify(inventoryApi).releaseTradeReservation(SELLER_ID, ENTRY_ID);
            verify(reservationWriter).save(reservation);
        }
    }

    @Nested
    @DisplayName("판매자가 Listing을 취소하면")
    class Cancel {

        @Test
        @DisplayName("ACTIVE 예약이 있으면 상태와 Inventory와 Wallet을 변경하지 않는다")
        void rejectsActiveReservation() {
            Listing listing = listing();
            ListingReservation reservation = ListingReservation.active(
                    LISTING_ID,
                    BUYER_ID,
                    "hold-296",
                    Instant.now(),
                    60
            );
            given(listingReader.getForUpdate(LISTING_ID)).willReturn(listing);
            given(reservationReader.findActiveForUpdate(LISTING_ID)).willReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.cancel(
                    SELLER_ID,
                    new EconomyCommand.CancelListing(LISTING_ID)
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(EconomyError.LISTING_ACTIVE_RESERVATION)
            );

            assertThat(listing.getStatus()).isEqualTo(ListingStatus.OPEN);
            verify(inventoryApi, never()).releaseListing(any(), any());
            verifyNoInteractions(walletReader, walletWriter);
        }
    }

    private Listing listing() {
        Listing listing = Listing.open(
                SELLER_ID,
                ENTRY_ID,
                40L,
                3,
                Money.of(40L, Currency.GOLD)
        );
        ReflectionTestUtils.setField(listing, "id", LISTING_ID);
        return listing;
    }

    private Wallet fundedWallet() {
        Wallet wallet = Wallet.open(BUYER_ID);
        wallet.deposit(Money.of(100L, Currency.GOLD));
        return wallet;
    }
}
