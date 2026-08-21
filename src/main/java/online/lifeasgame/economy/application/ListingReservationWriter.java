package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.ListingReservation;
import online.lifeasgame.economy.domain.repository.ListingReservationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ListingReservationWriter {

    private final ListingReservationRepository repository;

    public ListingReservation save(ListingReservation reservation) {
        return repository.save(reservation);
    }
}
