package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.application.query.ExerciseLogQueryRepository;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.repository.ExerciseLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ExerciseLogReader {

    private final ExerciseLogRepository exerciseLogRepository;
    private final ExerciseLogQueryRepository exerciseLogQueryRepository;

    public ExerciseLog get(Long id, Long playerId) {
        return exerciseLogRepository.findByIdAndPlayerId(id, playerId)
                .orElseThrow(() -> new IllegalArgumentException("EXERCISE_NOT_FOUND"));
    }

    public List<ExerciseLog> recent(Long playerId, int limit) {
        return exerciseLogQueryRepository.findByPlayer(playerId, limit);
    }

    public List<ExerciseLog> search(Long playerId, String category, LocalDate from, LocalDate to, int page, int size) {
        ExerciseCategory c = ExerciseCategory.parseNullable(category);
        return exerciseLogQueryRepository.search(playerId, c, from, to, page, size);
    }
}
