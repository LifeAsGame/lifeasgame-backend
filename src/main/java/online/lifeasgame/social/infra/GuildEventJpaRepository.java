package online.lifeasgame.social.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.social.domain.GuildEvent;
import online.lifeasgame.social.domain.repository.GuildEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
