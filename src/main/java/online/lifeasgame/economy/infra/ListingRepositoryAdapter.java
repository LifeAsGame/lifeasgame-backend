package online.lifeasgame.economy.infra;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.ListingStatus;
import online.lifeasgame.economy.domain.repository.ListingRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ListingRepositoryAdapter implements ListingRepository {

    private final JpaListingRepository jpaListingRepository;

    @Override
    public Listing save(Listing listing) {
        return jpaListingRepository.save(listing);
    }

    @Override
    public Optional<Listing> findById(Long id) {
        return jpaListingRepository.findById(id);
    }

    @Override
    public Optional<Listing> findByIdForUpdate(Long id) {
        return jpaListingRepository.findByIdForUpdate(id);
    }

    @Override
    public List<Listing> findByStatus(ListingStatus status) {
        return jpaListingRepository.findByStatus(status);
    }

    @Override
    public List<Listing> findBySellerPlayerId(Long sellerId) {
        return jpaListingRepository.findBySellerPlayerId(sellerId);
    }

}
