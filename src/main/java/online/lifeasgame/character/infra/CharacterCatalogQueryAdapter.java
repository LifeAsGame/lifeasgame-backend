package online.lifeasgame.character.infra;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.CharacterCatalogQuery;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import online.lifeasgame.character.domain.error.CertificationError;
import online.lifeasgame.character.domain.error.HobbyError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CharacterCatalogQueryAdapter implements CharacterCatalogQuery {
    private final JpaCertificationRepository certifications;
    private final JpaHobbyRepository hobbies;
    private final JpaPlayerCertificationRepository playerCertifications;
    private final JpaPlayerHobbyRepository playerHobbies;

    @Override
    public Page<Certification> certifications(String query, String majorCode, String minorCode, Pageable page) {
        return certifications.search(query, majorCode, minorCode, page);
    }

    @Override
    public Page<Hobby> hobbies(String query, HobbyCategory category, Pageable page) {
        return hobbies.search(query, category, page);
    }

    @Override
    public Certification certification(Long id) {
        return certifications.findById(id).filter(Certification::isActive)
                .orElseThrow(() -> new DomainException(CertificationError.CERTIFICATION_NOT_FOUND));
    }

    @Override
    public Hobby hobby(Long id) {
        return hobbies.findById(id).filter(Hobby::isActive)
                .orElseThrow(() -> new DomainException(HobbyError.HOBBY_NOT_FOUND));
    }

    @Override
    public List<Group> officialGroups() {
        return certifications.officialGroups().stream().map(CharacterCatalogQueryAdapter::group).toList();
    }

    @Override
    public List<Group> ownedOfficialGroups(Long playerId) {
        return certifications.ownedOfficialGroups(playerId).stream().map(CharacterCatalogQueryAdapter::group).toList();
    }

    @Override
    public List<CertificationCategory> ownedLegacyCertificationCategories(Long playerId) {
        return certifications.ownedLegacyCategories(playerId);
    }

    @Override
    public List<HobbyCategory> ownedHobbyCategories(Long playerId) {
        return playerHobbies.ownedCategories(playerId);
    }

    @Override
    public Long ownedCertificationId(Long playerId, Long certificationId) {
        return playerCertifications.findByPlayerIdAndCertificationId(playerId, certificationId)
                .map(online.lifeasgame.character.domain.PlayerCertification::getId).orElse(null);
    }

    @Override
    public Long ownedHobbyId(Long playerId, Long hobbyId) {
        return playerHobbies.findByPlayerIdAndHobbyId(playerId, hobbyId)
                .map(online.lifeasgame.character.domain.PlayerHobby::getId).orElse(null);
    }

    private static Group group(Object[] fields) {
        return new Group((String) fields[0], (String) fields[1], (String) fields[2], (String) fields[3]);
    }
}
