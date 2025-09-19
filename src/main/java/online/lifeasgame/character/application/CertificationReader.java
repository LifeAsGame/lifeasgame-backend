package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import online.lifeasgame.character.domain.error.CertificationError;
import online.lifeasgame.character.domain.repository.CertificationRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class CertificationReader {

    private final CertificationRepository repository;

    public List<Certification> getCertifications(List<CertificationCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return repository.findAll();
        }
        return repository.findByCategoryIn(categories);
    }

    public Certification getCertification(Long CertificationId) {
        return repository.findById(CertificationId)
                .orElseThrow(() -> new DomainException(CertificationError.CERTIFICATION_NOT_FOUND));
    }
}
