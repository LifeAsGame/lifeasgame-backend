package online.lifeasgame.lifelog.application;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
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
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LifeLogRecorded Application Service 발행")
class LifeLogRecordedApplicationServiceTest {

    private static final Long PLAYER_ID = 197L;
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T10:00:00Z");

    @Test
    @DisplayName("Collection direct legacy create는 FULL header와 non-ready Fact를 한 번 만든다")
    void recordsLegacyCollectionCreate() {
        CollectionLogReader reader = mock(CollectionLogReader.class);
        CollectionLogWriter writer = mock(CollectionLogWriter.class);
        LifeLogRecordRegistrar recordRegistrar =
                mock(LifeLogRecordRegistrar.class);
        RecordingPublisher publisher = new RecordingPublisher();
        CollectionLog saved = mock(CollectionLog.class);
        when(saved.getId()).thenReturn(101L);
        when(saved.getCategory()).thenReturn(CollectionCategory.BOOK);
        when(saved.getQuantity()).thenReturn(Quantity.of(2));
        when(writer.create(any(CollectionLog.class))).thenReturn(saved);
        LifeLogRecord canonical = record(
                1001L,
                LifeLogEntryMode.FULL,
                null
        );
        when(recordRegistrar.register(
                eq(PLAYER_ID),
                eq(LifeLogSourceType.COLLECTION),
                eq(101L),
                eq(LifeLogEntryMode.FULL),
                any(LifeLogRecordMetadataCommand.class)
        )).thenReturn(canonical);
        CollectionLogService service = new CollectionLogService(
                reader,
                writer,
                recordRegistrar,
                publisher,
                mock(CurrentPlayerAccessor.class)
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
        assertThat(result.lifeLogId()).isEqualTo(1001L);
        assertThat(publisher.events())
                .filteredOn(CollectionLogged.class::isInstance)
                .singleElement();
        assertRecorded(
                publisher.events(),
                1001L,
                LifeLogEntryMode.FULL,
                null
        );
        assertThat(publisher.events()).hasSize(2);
    }

    @Test
    @DisplayName("Exercise content-ready direct create는 FULL Definition Snapshot을 발행한다")
    void recordsContentReadyExerciseCreate() {
        ExerciseLogReader reader = mock(ExerciseLogReader.class);
        ExerciseLogWriter writer = mock(ExerciseLogWriter.class);
        LifeLogRecordRegistrar recordRegistrar =
                mock(LifeLogRecordRegistrar.class);
        RecordingPublisher publisher = new RecordingPublisher();
        ExerciseLog saved = mock(ExerciseLog.class);
        when(saved.getId()).thenReturn(102L);
        when(saved.getCategory()).thenReturn(ExerciseCategory.RUNNING);
        when(saved.getMetrics()).thenReturn(
                ExerciseMetrics.of(30, 5.0, 250)
        );
        when(writer.create(any(ExerciseLog.class))).thenReturn(saved);
        LifeLogRecord canonical = record(
                1002L,
                LifeLogEntryMode.FULL,
                LifeLogSubtype.ACTIVITY
        );
        when(recordRegistrar.register(
                eq(PLAYER_ID),
                eq(LifeLogSourceType.EXERCISE),
                eq(102L),
                eq(LifeLogEntryMode.FULL),
                any(LifeLogRecordMetadataCommand.class)
        )).thenReturn(canonical);
        ExerciseLogService service = new ExerciseLogService(
                reader,
                writer,
                recordRegistrar,
                publisher,
                mock(CurrentPlayerAccessor.class)
        );

        ExerciseResult.Created result = service.create(
                PLAYER_ID,
                new ExerciseCommand.Create(
                        "RUNNING",
                        30,
                        5.0,
                        250,
                        LocalDate.of(2026, 7, 24),
                        "Private exercise memo",
                        new LifeLogRecordMetadataCommand(
                                "ACTIVITY",
                                null
                        )
                )
        );

        assertThat(result.id()).isEqualTo(102L);
        assertThat(result.lifeLogId()).isEqualTo(1002L);
        assertThat(publisher.events())
                .filteredOn(ExerciseLogged.class::isInstance)
                .singleElement();
        assertRecorded(
                publisher.events(),
                1002L,
                LifeLogEntryMode.FULL,
                LifeLogSubtype.ACTIVITY
        );
        assertThat(publisher.events()).hasSize(2);
    }

    @Test
    @DisplayName("progress를 생략한 Media direct create도 0/1 Source와 Fact를 한 번 만든다")
    void recordsMediaCreateOnce() {
        MediaLogReader reader = mock(MediaLogReader.class);
        MediaLogWriter writer = mock(MediaLogWriter.class);
        LifeLogRecordRegistrar recordRegistrar =
                mock(LifeLogRecordRegistrar.class);
        RecordingPublisher publisher = new RecordingPublisher();
        MediaLog saved = mock(MediaLog.class);
        when(saved.getId()).thenReturn(103L);
        when(writer.create(any(MediaLog.class))).thenReturn(saved);
        LifeLogRecord canonical = record(
                1003L,
                LifeLogEntryMode.FULL,
                LifeLogSubtype.STUDY
        );
        when(recordRegistrar.register(
                eq(PLAYER_ID),
                eq(LifeLogSourceType.MEDIA),
                eq(103L),
                eq(LifeLogEntryMode.FULL),
                any(LifeLogRecordMetadataCommand.class)
        )).thenReturn(canonical);
        MediaLogService service = new MediaLogService(
                reader,
                mock(MediaLogUpdater.class),
                writer,
                recordRegistrar,
                publisher,
                mock(CurrentPlayerAccessor.class)
        );

        MediaLogResult.Created result = service.create(
                PLAYER_ID,
                new MediaLogCommand.Create(
                        "MOVIE",
                        "Private media title",
                        "Private original title",
                        null,
                        null,
                        "PLANNED",
                        Set.of("private-tag"),
                        new LifeLogRecordMetadataCommand("STUDY", null)
                )
        );

        assertThat(result.id()).isEqualTo(103L);
        assertThat(result.lifeLogId()).isEqualTo(1003L);
        verify(writer).create(argThat(media ->
                media.getProgress().current() == 0
                        && media.getProgress().total() == 1
        ));
        assertRecorded(
                publisher.events(),
                1003L,
                LifeLogEntryMode.FULL,
                LifeLogSubtype.STUDY
        );
        assertThat(publisher.events()).hasSize(1);
    }

    private void assertRecorded(
            List<DomainEvent> events,
            Long lifeLogId,
            LifeLogEntryMode entryMode,
            LifeLogSubtype subtype
    ) {
        assertThat(events)
                .filteredOn(LifeLogRecorded.class::isInstance)
                .singleElement()
                .satisfies(event -> {
                    LifeLogRecorded recorded = (LifeLogRecorded) event;
                    assertThat(recorded.eventId()).isNotBlank();
                    assertThat(recorded.eventType())
                            .isEqualTo("LifeLogRecorded");
                    assertThat(recorded.eventVersion()).isEqualTo(1);
                    assertThat(recorded.playerId()).isEqualTo(PLAYER_ID);
                    assertThat(recorded.lifeLogId())
                            .isEqualTo(lifeLogId);
                    assertThat(recorded.sourceDefinitionVersion())
                            .isEqualTo(1);
                    assertThat(recorded.subtype()).isEqualTo(subtype);
                    assertThat(recorded.entryMode())
                            .isEqualTo(entryMode);
                    assertThat(recorded.legacyLifeLogType()).isNull();
                    assertThat(recorded.primaryRoleId()).isNull();
                    assertThat(recorded.occurredAt())
                            .isEqualTo(OCCURRED_AT);
                    assertThat(recorded.isContentReady())
                            .isEqualTo(subtype != null);
                });
    }

    private LifeLogRecord record(
            Long id,
            LifeLogEntryMode entryMode,
            LifeLogSubtype subtype
    ) {
        LifeLogRecord record = mock(LifeLogRecord.class);
        when(record.getId()).thenReturn(id);
        when(record.getPlayerId()).thenReturn(PLAYER_ID);
        when(record.getSourceDefinitionVersion()).thenReturn(1);
        when(record.getEntryMode()).thenReturn(entryMode);
        when(record.getSubtype()).thenReturn(subtype);
        when(record.getPrimaryRoleId()).thenReturn(null);
        when(record.getOccurredAt()).thenReturn(OCCURRED_AT);
        return record;
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
