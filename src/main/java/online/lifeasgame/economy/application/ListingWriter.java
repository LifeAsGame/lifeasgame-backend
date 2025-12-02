package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.repository.ListingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ListingWriter {

    private final ListingRepository listingRepository;

    public Listing create(Long sellerId, Long itemInstanceId, Long itemId, Money price) {
        return listingRepository.save(Listing.open(sellerId, itemInstanceId, itemId, price));
    }

    public Listing save(Listing listing) {
        return listingRepository.save(listing);
    }
}
