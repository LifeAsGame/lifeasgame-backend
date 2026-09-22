package online.lifeasgame.lifelog.application;


import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.repository.ExerciseLogRepository;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.repository.LifeLogRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class ExerciseLogWriter {

    private final ExerciseLogRepository repository;
    private final LifeLogRecordRepository recordRepository;

    public ExerciseLog create(ExerciseLog exerciseLog) {
        return repository.save(exerciseLog);
    }

    public void delete(Long playerId, Long exerciseId) {
        if (repository.deleteByIdAndPlayerId(exerciseId, playerId) > 0) {
            recordRepository.deleteBySourceAndPlayerId(
                    LifeLogSourceType.EXERCISE, exerciseId, playerId
            );
        }
    }
}
