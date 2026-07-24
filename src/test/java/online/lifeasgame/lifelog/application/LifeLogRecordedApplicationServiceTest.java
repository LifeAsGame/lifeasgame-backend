package online.lifeasgame.lifelog.application;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import online.lifeasgame.lifelog.domain.CollectionCategory;
import online.lifeasgame.lifelog.domain.CollectionLog;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.ExerciseMetrics;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.Quantity;
import online.lifeasgame.lifelog.domain.event.CollectionLogged;
import online.lifeasgame.lifelog.domain.event.ExerciseLogged;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LifeLogRecorded Application Service 발행")
class LifeLogRecordedApplicationServiceTest {

    private static final Long PLAYER_ID = 197L;
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T10:00:00Z");
    private static final Clock CLOCK =
            Clock.fixed(OCCURRED_AT, ZoneOffset.UTC);

    @Test
    @DisplayName("Collection create는 저장 후 subtype Event와 Fact를 각각 한 번 발행한다")
    void recordsCollectionCreate() {
        CollectionLogReader reader = mock(CollectionLogReader.class);
        CollectionLogWriter writer = mock(CollectionLogWriter.class);
        RecordingPublisher publisher = new RecordingPublisher();
        CollectionLog saved = mock(CollectionLog.class);
        when(saved.getId()).thenReturn(101L);
        when(saved.getCategory()).thenReturn(CollectionCategory.BOOK);
        when(saved.getQuantity()).thenReturn(Quantity.of(2));
        when(writer.create(any(CollectionLog.class))).thenReturn(saved);
        CollectionLogService service = new CollectionLogService(
                reader,
                writer,
                publisher,
                CLOCK
        );

        CollectionResult.Created result = service.create(
                PLAYER_ID,
                new CollectionCommand.Create(
                        "BOOK",
                        "Private collection title",
                        "Private original title",
                        2,
                        "Private condition",
                        "Private source",
                        Set.of("private-tag")
                )
        );

        verify(writer).create(any(CollectionLog.class));
        assertThat(result.id()).isEqualTo(101L);
        assertThat(publisher.events())
                .filteredOn(CollectionLogged.class::isInstance)
                .singleElement()
                .satisfies(event -> {
                    CollectionLogged logged = (CollectionLogged) event;
                    assertThat(logged.playerId()).isEqualTo(PLAYER_ID);
                    assertThat(logged.collectionLogId()).isEqualTo(101L);
                    assertThat(logged.occurredAt()).isEqualTo(OCCURRED_AT);
                });
        assertRecorded(
                publisher.events(),
                101L,
                LifeLogType.COLLECTION
        );
        assertThat(publisher.events()).hasSize(2);
    }

    @Test
    @DisplayName("Exercise create는 저장 후 subtype Event와 Fact를 각각 한 번 발행한다")
    void recordsExerciseCreate() {
        ExerciseLogReader reader = mock(ExerciseLogReader.class);
        ExerciseLogWriter writer = mock(ExerciseLogWriter.class);
        RecordingPublisher publisher = new RecordingPublisher();
        ExerciseLog saved = mock(ExerciseLog.class);
        when(saved.getId()).thenReturn(102L);
        when(saved.getCategory()).thenReturn(ExerciseCategory.RUNNING);
        when(saved.getMetrics()).thenReturn(ExerciseMetrics.of(30, 5.0, 250));
        when(writer.create(any(ExerciseLog.class))).thenReturn(saved);
        ExerciseLogService service = new ExerciseLogService(
                reader,
                writer,
                publisher,
                CLOCK
        );

        ExerciseResult.Created result = service.create(
                PLAYER_ID,
                new ExerciseCommand.Create(
                        "RUNNING",
                        30,
                        5.0,
                        250,
                        LocalDate.of(2026, 7, 24),
                        "Private exercise memo"
                )
        );

        verify(writer).create(any(ExerciseLog.class));
        assertThat(result.id()).isEqualTo(102L);
        assertThat(publisher.events())
                .filteredOn(ExerciseLogged.class::isInstance)
                .singleElement()
                .satisfies(event -> {
                    ExerciseLogged logged = (ExerciseLogged) event;
                    assertThat(logged.playerId()).isEqualTo(PLAYER_ID);
                    assertThat(logged.exerciseLogId()).isEqualTo(102L);
                    assertThat(logged.occurredAt()).isEqualTo(OCCURRED_AT);
                });
        assertRecorded(
                publisher.events(),
                102L,
                LifeLogType.EXERCISE
        );
        assertThat(publisher.events()).hasSize(2);
    }

    @Test
    @DisplayName("Media create는 저장 후 Fact를 정확히 한 번 발행한다")
    void recordsMediaCreate() {
        MediaLogReader reader = mock(MediaLogReader.class);
        MediaLogWriter writer = mock(MediaLogWriter.class);
        RecordingPublisher publisher = new RecordingPublisher();
        MediaLog saved = mock(MediaLog.class);
        when(saved.getId()).thenReturn(103L);
        when(writer.create(any(MediaLog.class))).thenReturn(saved);
        MediaLogService service = new MediaLogService(
                reader,
                writer,
                publisher,
                CLOCK
        );

        MediaLogResult.Created result = service.create(
                PLAYER_ID,
                new MediaLogCommand.Create(
                        "MOVIE",
                        "Private media title",
                        "Private original title",
                        0,
                        1,
                        "PLANNED",
                        Set.of("private-tag")
                )
        );

        verify(writer).create(any(MediaLog.class));
        assertThat(result.id()).isEqualTo(103L);
        assertRecorded(publisher.events(), 103L, LifeLogType.MEDIA);
        assertThat(publisher.events()).hasSize(1);
    }

    private void assertRecorded(
            List<DomainEvent> events,
            Long lifeLogId,
            LifeLogType type
    ) {
        assertThat(events)
                .filteredOn(LifeLogRecorded.class::isInstance)
                .singleElement()
                .satisfies(event -> {
                    LifeLogRecorded recorded = (LifeLogRecorded) event;
                    assertThat(recorded.eventId()).isNotBlank();
                    assertThat(recorded.eventVersion()).isEqualTo(1);
                    assertThat(recorded.playerId()).isEqualTo(PLAYER_ID);
                    assertThat(recorded.lifeLogId()).isEqualTo(lifeLogId);
                    assertThat(recorded.lifeLogType()).isEqualTo(type);
                    assertThat(recorded.primaryRoleId()).isNull();
                    assertThat(recorded.occurredAt())
                            .isEqualTo(OCCURRED_AT);
                });
    }

    private static class RecordingPublisher implements DomainEventPublisher {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }

        List<DomainEvent> events() {
            return List.copyOf(events);
        }
    }
}
