package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.application.model.CollectionSpec;
import online.lifeasgame.lifelog.domain.CollectionLog;
import online.lifeasgame.lifelog.domain.repository.CollectionLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CollectionLogWriter {

    private final CollectionLogRepository repository;

    public CollectionLog create(CollectionSpec.Create spec) {
        return repository.save(CollectionLog.create(
                spec.playerId(), spec.category(), spec.title(), spec.quantity(),
                spec.conditionNote(), spec.acquiredFrom(), spec.tags()
        ));
    }

    // 변경감지
    public void changeQuantity(CollectionLog log, Integer value) {
        log.changeQuantity(value);
    }

    public void changeCondition(CollectionLog log, String note) {
        log.changeCondition(note);
    }

    public void changeAcquiredFrom(CollectionLog log, String from) {
        log.changeAcquiredFrom(from);
    }
}
