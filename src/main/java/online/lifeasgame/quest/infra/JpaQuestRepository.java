package online.lifeasgame.quest.infra;

import online.lifeasgame.quest.domain.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaQuestRepository extends JpaRepository<Quest, Long> {
    Optional<Quest> findByCode(String code);
}
