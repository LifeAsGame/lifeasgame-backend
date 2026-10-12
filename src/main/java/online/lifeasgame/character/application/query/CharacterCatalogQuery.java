package online.lifeasgame.character.application.query;

import java.util.List;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CharacterCatalogQuery {
    record Group(String majorCode, String majorName, String minorCode, String minorName) {}

    Page<Certification> certifications(String query, String majorCode, String minorCode, Pageable page);
    Page<Hobby> hobbies(String query, HobbyCategory category, Pageable page);
    Certification certification(Long id);
    Hobby hobby(Long id);
    List<Group> officialGroups();
    List<Group> ownedOfficialGroups(Long playerId);
    List<CertificationCategory> ownedLegacyCertificationCategories(Long playerId);
    List<HobbyCategory> ownedHobbyCategories(Long playerId);
    Long ownedCertificationId(Long playerId, Long certificationId);
    Long ownedHobbyId(Long playerId, Long hobbyId);
}
