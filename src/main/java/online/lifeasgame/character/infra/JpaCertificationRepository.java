package online.lifeasgame.character.infra;

import java.util.Collection;
import java.util.List;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface JpaCertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByCategoryIn(Collection<CertificationCategory> categories);

    List<Certification> findByActiveTrue();

    List<Certification> findByCategoryInAndActiveTrue(Collection<CertificationCategory> categories);

    Optional<Certification> findByProviderAndSourceCode(String provider, String sourceCode);

    @Query("select c from Certification c where c.active = true and (:query is null or lower(c.name) like lower(concat('%', :query, '%'))) and (:major is null or (:major = 'LEGACY' and c.provider is null) or c.majorCode = :major) and (:minor is null or c.minorCode = :minor)")
    Page<Certification> search(@Param("query") String query, @Param("major") String major,
                               @Param("minor") String minor, Pageable page);

    @Query("select distinct c.majorCode, c.majorName, c.minorCode, c.minorName from Certification c where c.provider = 'HRDK' and c.active = true order by c.majorCode, c.minorCode")
    List<Object[]> officialGroups();

    @Query("select distinct c.majorCode, c.majorName, c.minorCode, c.minorName from PlayerCertification pc join Certification c on c.id = pc.certificationId where pc.playerId = :playerId and c.provider = 'HRDK' order by c.majorCode, c.minorCode")
    List<Object[]> ownedOfficialGroups(@Param("playerId") Long playerId);

    @Query("select distinct c.category from PlayerCertification pc join Certification c on c.id = pc.certificationId where pc.playerId = :playerId and c.provider is null")
    List<CertificationCategory> ownedLegacyCategories(@Param("playerId") Long playerId);
}
