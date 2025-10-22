package online.lifeasgame.lifelog.application;


import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.application.model.ExerciseSpec;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.repository.ExerciseLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ExerciseLogWriter {

    private final ExerciseLogRepository repository;

    public ExerciseLog create(ExerciseSpec.Create spec) {
        return repository.save(ExerciseLog.create(
                spec.playerId(),
                spec.category(),
                spec.metrics(),
                spec.exercisedOn(),
                spec.memo()
        ));
    }

    // 변경감지
    public void update(ExerciseLog log, ExerciseSpec.Create specLike) {
        log.changeMetrics(specLike.metrics());
        log.changeExercisedOn(specLike.exercisedOn());
        log.changeMemo(specLike.memo());
    }
}
