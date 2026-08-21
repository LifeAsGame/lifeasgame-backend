package online.lifeasgame.economy.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.ListingReservation;
import online.lifeasgame.economy.domain.ListingReservationState;
import online.lifeasgame.economy.domain.repository.ListingReservationRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ListingReservationRepositoryAdapter implements ListingReservationRepository {

    private final JpaListingReservationRepository jpaRepository;

    @Override
    public ListingReservation save(ListingReservation reservation) {
        return jpaRepository.save(reservation);
    }

    @Override
    public Optional<ListingReservation> findActiveByListingIdForUpdate(Long listingId) {
        return jpaRepository.findByListingIdAndStateForUpdate(
                listingId,
                ListingReservationState.ACTIVE
        );
    }

    @Override
    public List<Long> findActiveListingIdsExpiringBefore(Instant cutoff) {
        return jpaRepository.findListingIdsByStateAndExpiresAtBefore(
                ListingReservationState.ACTIVE,
                cutoff
        );
    }

    @Override
    public List<ListingReservation> findActiveByBuyerPlayerId(Long buyerPlayerId) {
        return jpaRepository.findByBuyerPlayerIdAndStateOrderByIdDesc(
                buyerPlayerId,
                ListingReservationState.ACTIVE
        );
    }
}
