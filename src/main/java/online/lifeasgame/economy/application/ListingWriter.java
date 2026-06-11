package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.repository.ListingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ListingWriter {

    private final ListingRepository repository;

    public Listing create(Listing listing) {
        return repository.save(listing);
    }
}
