package online.lifeasgame.lifelog.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.repository.ExerciseLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExerciseLogRepositoryAdapter implements ExerciseLogRepository {

    private final ExerciseLogJpaRepository jpa;

    @Override
    public ExerciseLog save(ExerciseLog log) {
        return jpa.save(log);
    }

    @Override
    public Optional<ExerciseLog> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<ExerciseLog> findByIdAndPlayerId(Long id, Long playerId) {
        return jpa.findByIdAndPlayerId(id, playerId);
    }

    @Override
    public List<ExerciseLog> findByPlayerId(Long playerId, int limit) {
        return jpa.findByPlayerIdOrderByIdDesc(playerId, PageRequest.of(0, limit));
    }

    @Override
    public List<ExerciseLog> search(
            Long playerId,
            ExerciseCategory category,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
        Page<ExerciseLog> pr = jpa.search(
                playerId,
                category,
                from,
                to,
                PageRequest.of(page, size, Sort.by("exercisedOn").descending().and(Sort.by("id").descending()))
        );
        return pr.getContent();
    }
}
