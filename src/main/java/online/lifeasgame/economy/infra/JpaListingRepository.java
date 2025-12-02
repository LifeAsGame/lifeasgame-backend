package online.lifeasgame.economy.infra;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface JpaListingRepository extends JpaRepository<Listing, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT l FROM Listing l WHERE l.id = :id")
    Optional<Listing> findByIdForUpdate(Long id);

    List<Listing> findByStatusAndReservationExpiresAtBefore(ListingStatus status, Instant expiresAt);

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findBySellerPlayerId(Long sellerId);

    List<Listing> findByReservedBy(Long buyerId);
}
