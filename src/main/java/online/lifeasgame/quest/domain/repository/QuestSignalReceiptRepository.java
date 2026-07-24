package online.lifeasgame.quest.domain.repository;

import online.lifeasgame.quest.domain.QuestSignalReceipt;

import java.util.Optional;

public interface QuestSignalReceiptRepository {

    QuestSignalReceipt saveAndFlush(QuestSignalReceipt receipt);

    Optional<QuestSignalReceipt> findByIdentity(
            String questCode,
            Long playerId,
            String correlationId
    );
}
