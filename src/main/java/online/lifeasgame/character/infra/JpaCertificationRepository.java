package online.lifeasgame.character.infra;

import java.util.Collection;
import java.util.List;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByCategoryIn(Collection<CertificationCategory> categories);
}
