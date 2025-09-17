package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerCertification;
import online.lifeasgame.character.domain.repository.PlayerCertificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PlayerCertificationWriter {

    private final PlayerCertificationRepository repository;

    public PlayerCertification grantCertification(PlayerCertification playerCertification) {
        return repository.save(playerCertification);
    }
}
