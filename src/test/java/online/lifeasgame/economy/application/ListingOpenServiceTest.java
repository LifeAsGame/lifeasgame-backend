package online.lifeasgame.economy.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.economy.api.player.request.EconomyRequest;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.domain.Currency;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.inventory.application.internal.InventoryMarketAvailabilityApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Listing 생성 권한")
class ListingOpenServiceTest {

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;
    @Mock
    private InventoryMarketAvailabilityApi inventoryApi;
    @Mock
    private ListingWriter listingWriter;
    @Mock
    private DomainEventPublisher eventPublisher;
    @InjectMocks
    private ListingOpenService service;

    @Nested
    @DisplayName("Current Player가 whole InventoryEntry를 등록하면")
    class OpenWholeEntry {

        @Test
        @DisplayName("클라이언트 item/quantity 권한 없이 서버 snapshot 전체 수량을 저장한다")
        void snapshotsServerOwnedEntry() {
            given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(10L);
            given(inventoryApi.listWholeEntry(10L, 20L)).willReturn(
                    new InventoryMarketAvailabilityApi.EntrySnapshot(
                            20L,
                            10L,
                            30L,
                            7,
                            "LISTED"
                    )
            );
            given(listingWriter.create(any())).willAnswer(invocation -> invocation.getArgument(0));

            service.open(new EconomyCommand.OpenListing(20L, 900L, "GEM"));

            assertThat(EconomyRequest.OpenListing.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .containsExactly("inventoryEntryId", "price", "currency");
            ArgumentCaptor<Listing> listing = ArgumentCaptor.forClass(Listing.class);
            verify(listingWriter).create(listing.capture());
            assertThat(listing.getValue().getSellerPlayerId()).isEqualTo(10L);
            assertThat(listing.getValue().getItemInstanceId()).isEqualTo(20L);
            assertThat(listing.getValue().getItemId()).isEqualTo(30L);
            assertThat(listing.getValue().getSaleQuantity()).isEqualTo(7);
            assertThat(listing.getValue().getPrice().amount()).isEqualTo(900L);
            assertThat(listing.getValue().getPrice().currency()).isEqualTo(Currency.GEM);
        }

        @Test
        @DisplayName("지원하지 않는 currency는 Inventory 점유 전에 stable 400으로 거절한다")
        void rejectsUnsupportedCurrency() {
            assertThatThrownBy(() -> service.open(
                    new EconomyCommand.OpenListing(20L, 900L, "GEMS")
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(EconomyError.INVALID_CURRENCY)
            );

            verifyNoInteractions(
                    currentPlayerAccessor,
                    inventoryApi,
                    listingWriter,
                    eventPublisher
            );
        }
    }
}
