package online.lifeasgame.lifelog.application;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.ExerciseMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("Exercise partial update")
class ExerciseLogServiceTest {

    private static final Long PLAYER_ID = 270L;
    private static final Long EXERCISE_ID = 2701L;
    private static final LocalDate EXERCISED_ON =
            LocalDate.of(2026, 8, 14);

    @Mock
    private ExerciseLogReader exerciseLogReader;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    private ExerciseLogService service;
    private ExerciseLog exerciseLog;

    @BeforeEach
    void setUp() {
        service = new ExerciseLogService(
                exerciseLogReader,
                mock(ExerciseLogWriter.class),
                mock(LifeLogRecordRegistrar.class),
                mock(DomainEventPublisher.class),
                currentPlayerAccessor
        );
        exerciseLog = ExerciseLog.create(
                PLAYER_ID,
                ExerciseCategory.RUNNING,
                ExerciseMetrics.of(30, 5.0, 250),
                EXERCISED_ON,
                "기존 메모"
        );
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willReturn(PLAYER_ID);
        given(exerciseLogReader.getByIdAndPlayerIdOrThrow(
                EXERCISE_ID,
                PLAYER_ID
        )).willReturn(exerciseLog);
    }

    @Test
    @DisplayName("category와 duration만 바꾸고 생략한 metrics/date/memo는 보존한다")
    void appliesSuppliedFieldsAndPreservesOmittedFields() {
        ExerciseResult.Info result = service.update(
                EXERCISE_ID,
                new ExerciseCommand.Update(
                        "CYCLING",
                        45,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertThat(result.category()).isEqualTo("CYCLING");
        assertThat(result.durationMinutes()).isEqualTo(45);
        assertThat(result.distanceKm()).isEqualTo(5.0);
        assertThat(result.calories()).isEqualTo(250);
        assertThat(result.exercisedOn()).isEqualTo(EXERCISED_ON);
        assertThat(result.memo()).isEqualTo("기존 메모");
    }

    @Test
    @DisplayName("null memo는 보존하고 blank memo는 기존 정규화로 지운다")
    void preservesNullMemoAndClearsBlankMemo() {
        ExerciseResult.Info preserved = service.update(
                EXERCISE_ID,
                new ExerciseCommand.Update(
                        null, null, null, null, null, null
                )
        );
        ExerciseResult.Info cleared = service.update(
                EXERCISE_ID,
                new ExerciseCommand.Update(
                        null, null, null, null, null, "   "
                )
        );

        assertThat(preserved.memo()).isEqualTo("기존 메모");
        assertThat(cleared.memo()).isNull();
    }

    @Test
    @DisplayName("supplied invalid duration은 기존 domain invariant로 거부한다")
    void rejectsInvalidSuppliedDuration() {
        assertThatThrownBy(() -> service.update(
                EXERCISE_ID,
                new ExerciseCommand.Update(
                        null, 0, null, null, null, null
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("durationMinutes >= 1");
    }
}
