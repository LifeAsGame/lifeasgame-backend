package online.lifeasgame.social.infra;

import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyJpaRepository extends JpaRepository<Party, Long> {

    Optional<Party> findByIdAndPlayerId(Long id, Long playerId);

    @Query("SELECT p FROM Party p JOIN p.members m WHERE m.playerId = :playerId AND p.status = online.lifeasgame.social.domain.PartyStatus.ACTIVE ORDER BY p.id DESC")
    Page<Party> findMine(@Param("playerId") Long playerId, Pageable pageable);

    @Query("SELECT m FROM PartyMember m WHERE m.party.id = :partyId ORDER BY m.joinedAt, m.id")
    Page<online.lifeasgame.social.domain.PartyMember> findMembers(@Param("partyId") Long partyId, Pageable pageable);

    @Query("SELECT w FROM PartyWaitMember w WHERE w.party.id = :partyId AND w.type = online.lifeasgame.social.domain.PartyWaitType.JOIN_REQUEST AND w.status = online.lifeasgame.social.domain.PartyWaitStatus.PENDING ORDER BY w.id")
    Page<online.lifeasgame.social.domain.PartyWaitMember> findPendingRequests(@Param("partyId") Long partyId, Pageable pageable);

    @Query("SELECT w FROM PartyWaitMember w JOIN FETCH w.party p WHERE w.playerId = :playerId AND w.type = :type AND w.status = online.lifeasgame.social.domain.PartyWaitStatus.PENDING ORDER BY w.id DESC")
    Page<online.lifeasgame.social.domain.PartyWaitMember> findMyPending(@Param("playerId") Long playerId, @Param("type") online.lifeasgame.social.domain.PartyWaitType type, Pageable pageable);

    @Query(
        """
            SELECT p.id
            FROM Party p
            WHERE (:keyword IS NULL OR :keyword='' OR LOWER(p.name.value) LIKE LOWER(CONCAT('%',:keyword,'%') )
                OR LOWER(p.code.value) LIKE LOWER(CONCAT('%',:keyword,'%') ) )
                AND p.visibility = online.lifeasgame.social.domain.PartyVisibility.PUBLIC AND p.status = online.lifeasgame.social.domain.PartyStatus.ACTIVE
                AND (:visibility IS NULL OR p.visibility = :visibility)
            ORDER BY p.id DESC
        """
    )
    Page<Long> searchIds(
            @Param("keyword") String keyword,
            @Param("visibility") PartyVisibility visibility,
            Pageable pageable
    );

    @Query(
        """
            SELECT DISTINCT p
            FROM Party p
            LEFT JOIN FETCH p.tags t
            WHERE p.id IN :ids
        """
    )
    List<Party> fetchWithTagsByIds(@Param("ids") List<Long> ids);

    @Query(
        """
            SELECT DISTINCT p
            FROM Party p
            LEFT JOIN FETCH p.tags t
            WHERE p.id IN :ids
        """
    )
    List<Party> findRecentWithTags(@Param("ids") List<Long> ids);

    @Query("SELECT p.id FROM Party p WHERE p.visibility = online.lifeasgame.social.domain.PartyVisibility.PUBLIC AND p.status = online.lifeasgame.social.domain.PartyStatus.ACTIVE ORDER BY p.createdAt DESC, p.id DESC")
    List<Long> findRecent(Pageable pageable);
}
