package online.lifeasgame.economy.infra;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import online.lifeasgame.economy.domain.ListingReservation;
import online.lifeasgame.economy.domain.ListingReservationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JpaListingReservationRepository extends JpaRepository<ListingReservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("""
            SELECT reservation
            FROM ListingReservation reservation
            WHERE reservation.listingId = :listingId
              AND reservation.state = :state
            """)
    Optional<ListingReservation> findByListingIdAndStateForUpdate(
            @Param("listingId") Long listingId,
            @Param("state") ListingReservationState state
    );

    @Query("""
            SELECT reservation.listingId
            FROM ListingReservation reservation
            WHERE reservation.state = :state
              AND reservation.expiresAt < :cutoff
            ORDER BY reservation.expiresAt, reservation.id
            """)
    List<Long> findListingIdsByStateAndExpiresAtBefore(
            @Param("state") ListingReservationState state,
            @Param("cutoff") Instant cutoff
    );

    List<ListingReservation> findByBuyerPlayerIdAndStateOrderByIdDesc(
            Long buyerPlayerId,
            ListingReservationState state
    );
}
