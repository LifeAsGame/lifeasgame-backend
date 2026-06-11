package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
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
class ExerciseLogReader {

    private final ExerciseLogRepository repository;

    public ExerciseLog getByIdAndPlayerIdOrThrow(Long id, Long playerId) {
        return repository.findByIdAndPlayerId(id, playerId)
                .orElseThrow(() -> new IllegalArgumentException("EXERCISE_NOT_FOUND"));
    }

    public List<ExerciseLog> recent(Long playerId, int limit) {
        return repository.findByPlayerId(playerId, limit);
    }

    public List<ExerciseLog> search(
            Long playerId,
            String category,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
        ExerciseCategory exerciseCategory = ExerciseCategory.parseNullable(category);
        return repository.search(playerId, exerciseCategory, from, to, page, size);
    }
}
