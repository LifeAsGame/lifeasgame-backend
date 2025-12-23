package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.repository.PartyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PartyWriter {

    private final PartyRepository repository;

    public Party create(Party party) {
        return repository.save(party);
    }
}
