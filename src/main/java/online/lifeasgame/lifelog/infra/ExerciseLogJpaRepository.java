package online.lifeasgame.lifelog.infra;

import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExerciseLogJpaRepository extends JpaRepository<ExerciseLog, Long> {

    List<ExerciseLog> findByPlayerIdOrderByIdDesc(Long playerId, Pageable pageable);

    Optional<ExerciseLog> findByIdAndPlayerId(Long id, Long playerId);

    @Query(
            """
                        SELECT e FROM ExerciseLog e
                        WHERE e.playerId = :playerId
                          AND (:category IS NULL OR e.category = :category)
                          AND (:from IS NULL OR e.exercisedOn >= :from)
                          AND (:to IS NULL OR e.exercisedOn <= :to)
                    """
    )
    Page<ExerciseLog> search(
            @Param("playerId") Long playerId,
            @Param("category") ExerciseCategory category,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );
}
