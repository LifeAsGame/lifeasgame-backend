package online.lifeasgame.economy.application;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.ListingStatus;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.repository.ListingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ListingReader {

    private final ListingRepository listingRepository;

    public Listing get(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new DomainException(EconomyError.LISTING_NOT_FOUND));
    }

    public Listing getForUpdate(Long id) {
        return listingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new DomainException(EconomyError.LISTING_NOT_FOUND));
    }

    public List<Listing> listOpen() {
        return listingRepository.findByStatus(ListingStatus.OPEN);
    }

    public List<Listing> findReservedExpiringBefore(Instant cutoff) {
        return listingRepository.findByStatusAndReservationExpiresAtBefore(ListingStatus.RESERVED, cutoff);
    }

    public List<Listing> listBySeller(Long sellerId) {
        return listingRepository.findBySellerPlayerId(sellerId);
    }

    public List<Listing> listByReservedBy(Long buyerId) {
        return listingRepository.findByReservedBy(buyerId);
    }
}
