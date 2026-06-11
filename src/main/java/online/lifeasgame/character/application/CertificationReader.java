package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import online.lifeasgame.character.domain.error.CertificationError;
import online.lifeasgame.character.domain.repository.CertificationRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class CertificationReader {

    private final CertificationRepository repository;

    public List<Certification> getByCategories(List<CertificationCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return repository.findAll();
        }

        return repository.findByCategoryIn(categories);
    }

    public Certification getByIdOrThrow(Long certificationId) {
        return repository.findById(certificationId)
                .orElseThrow(() -> new DomainException(CertificationError.CERTIFICATION_NOT_FOUND));
    }
}
