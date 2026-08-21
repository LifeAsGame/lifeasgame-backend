package online.lifeasgame.economy.domain.repository;

import java.util.List;
import java.util.Optional;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.ListingStatus;

public interface ListingRepository {
    Listing save(Listing listing);
    Optional<Listing> findById(Long id);
    Optional<Listing> findByIdForUpdate(Long id);
    List<Listing> findByStatus(ListingStatus status);
    List<Listing> findBySellerPlayerId(Long sellerId);
    List<Listing> findByReservedBy(Long buyerId);
}
