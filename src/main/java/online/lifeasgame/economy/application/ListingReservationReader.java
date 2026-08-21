package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.ListingReservation;
import online.lifeasgame.economy.domain.repository.ListingReservationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ListingReservationReader {

    private final ListingReservationRepository repository;

    public Optional<ListingReservation> findActiveForUpdate(Long listingId) {
        return repository.findActiveByListingIdForUpdate(listingId);
    }

    public List<Long> findActiveListingIdsExpiringBefore(Instant cutoff) {
        return repository.findActiveListingIdsExpiringBefore(cutoff);
    }

    public List<ListingReservation> listActiveByBuyer(Long buyerPlayerId) {
        return repository.findActiveByBuyerPlayerId(buyerPlayerId);
    }
}
