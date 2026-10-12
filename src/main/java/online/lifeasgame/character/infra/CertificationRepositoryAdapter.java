package online.lifeasgame.character.infra;


import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import online.lifeasgame.character.domain.repository.CertificationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CertificationRepositoryAdapter implements CertificationRepository {

    private final JpaCertificationRepository jpaRepository;

    @Override
    public Certification save(Certification Certification) {
        return jpaRepository.save(Certification);
    }

    @Override
    public List<Certification> findAll() {
        return jpaRepository.findByActiveTrue();
    }

    @Override
    public List<Certification> findByCategoryIn(List<CertificationCategory> CertificationCategories) {
        return jpaRepository.findByCategoryInAndActiveTrue(CertificationCategories);
    }

    @Override
    public Optional<Certification> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Certification> findByProviderAndSourceCode(String provider, String sourceCode) {
        return jpaRepository.findByProviderAndSourceCode(provider, sourceCode);
    }

    @Override
    public void delete(Long certificationId) {
        jpaRepository.findById(certificationId).ifPresent(Certification::deactivate);
    }
}
