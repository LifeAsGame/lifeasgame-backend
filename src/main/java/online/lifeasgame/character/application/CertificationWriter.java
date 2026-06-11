package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.repository.CertificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class CertificationWriter {

    private final CertificationRepository repository;

    public Certification create(Certification certification) {
        return repository.save(certification);
    }

    public void delete(Long certificationId) {
        repository.delete(certificationId);
    }
}
