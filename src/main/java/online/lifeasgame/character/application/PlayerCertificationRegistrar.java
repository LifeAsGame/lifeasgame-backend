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
class PlayerCertificationRegistrar {

    private final PlayerCertificationRepository repository;

    public PlayerCertification register(PlayerCertification playerCertification) {
        return repository.save(playerCertification);
    }
}
