package online.lifeasgame.economy.domain.repository;

import online.lifeasgame.economy.domain.ListingReservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ListingReservationRepository {

    ListingReservation save(ListingReservation reservation);

    Optional<ListingReservation> findActiveByListingIdForUpdate(Long listingId);

    List<Long> findActiveListingIdsExpiringBefore(Instant cutoff);

    List<ListingReservation> findActiveByBuyerPlayerId(Long buyerPlayerId);
}
