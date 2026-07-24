package online.lifeasgame.quest.infra;

import online.lifeasgame.quest.domain.QuestSignalReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaQuestSignalReceiptRepository
        extends JpaRepository<QuestSignalReceipt, Long> {

    Optional<QuestSignalReceipt> findByQuestCodeAndPlayerIdAndCorrelationId(
            String questCode,
            Long playerId,
            String correlationId
    );
}
