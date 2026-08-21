package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.ListingStatus;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.repository.ListingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ListingReader {

    private final ListingRepository repository;

    public Listing getForUpdate(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new DomainException(EconomyError.LISTING_NOT_FOUND));
    }

    public Listing get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new DomainException(EconomyError.LISTING_NOT_FOUND));
    }

    public List<Listing> listOpen() {
        return repository.findByStatus(ListingStatus.OPEN);
    }

    public List<Listing> listBySeller(Long sellerId) {
        return repository.findBySellerPlayerId(sellerId);
    }

}
