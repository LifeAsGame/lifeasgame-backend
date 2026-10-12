package online.lifeasgame.social.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.social.domain.GuildEvent;
import online.lifeasgame.social.domain.GuildEventRsvp;
import online.lifeasgame.social.domain.repository.GuildEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuildEventJpaRepository extends JpaRepository<GuildEvent, Long>, GuildEventRepository {
    @Override
    Optional<GuildEvent> findByIdAndGuildId(Long id, Long guildId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM GuildEvent e WHERE e.id = :id AND e.guildId = :guildId")
    Optional<GuildEvent> findForUpdate(@Param("id") Long id, @Param("guildId") Long guildId);

    @Override
    Page<GuildEvent> findByGuildIdOrderByIdDesc(Long guildId, Pageable page);

    @Override
    @Query(value = """
            SELECT r FROM GuildEventRsvp r JOIN GuildMember m
                ON m.guild.id = :guildId AND m.playerId = r.playerId
            WHERE r.event.id = :eventId AND r.active = true ORDER BY r.id
            """, countQuery = """
            SELECT COUNT(r) FROM GuildEventRsvp r JOIN GuildMember m
                ON m.guild.id = :guildId AND m.playerId = r.playerId
            WHERE r.event.id = :eventId AND r.active = true
            """)
    Page<GuildEventRsvp> findCurrentParticipants(@Param("guildId") Long guildId,
                                                  @Param("eventId") Long eventId, Pageable page);

    @Override
    @Query("""
            SELECT r.event.id AS eventId, COUNT(r) AS participantCount,
                   SUM(CASE WHEN r.playerId = :actor THEN 1L ELSE 0L END) AS myRsvp
            FROM GuildEventRsvp r JOIN GuildMember m
                ON m.guild.id = r.event.guildId AND m.playerId = r.playerId
            WHERE r.active = true AND r.event.id IN :eventIds GROUP BY r.event.id
            """)
    List<EventTotals> totals(@Param("eventIds") List<Long> eventIds, @Param("actor") Long actor);
}
