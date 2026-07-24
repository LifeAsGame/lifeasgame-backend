package online.lifeasgame.quest.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.QuestSignalReceipt;
import online.lifeasgame.quest.domain.repository.QuestSignalReceiptRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuestSignalReceiptRepositoryAdapter
        implements QuestSignalReceiptRepository {

    private final JpaQuestSignalReceiptRepository jpaRepository;

    @Override
    public QuestSignalReceipt saveAndFlush(QuestSignalReceipt receipt) {
        return jpaRepository.saveAndFlush(receipt);
    }

    @Override
    public Optional<QuestSignalReceipt> findByIdentity(
            String questCode,
            Long playerId,
            String correlationId
    ) {
        return jpaRepository.findByQuestCodeAndPlayerIdAndCorrelationId(
                questCode,
                playerId,
                correlationId
        );
    }
}
