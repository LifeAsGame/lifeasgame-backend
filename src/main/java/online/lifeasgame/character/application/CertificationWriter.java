package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.error.CertificationError;
import online.lifeasgame.character.domain.repository.CertificationRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CertificationWriter {

    private final CertificationRepository CertificationRepository;

    public Certification create(Certification Certification) {
        return CertificationRepository.save(Certification);
    }
}
