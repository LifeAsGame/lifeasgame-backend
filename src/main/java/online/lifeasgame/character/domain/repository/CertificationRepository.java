package online.lifeasgame.character.domain.repository;

import java.util.List;
import java.util.Optional;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;

public interface CertificationRepository {
    Certification save(Certification certification);

    List<Certification> findAll();

    List<Certification> findByCategoryIn(List<CertificationCategory> categories);

    Optional<Certification> findById(Long id);
}
