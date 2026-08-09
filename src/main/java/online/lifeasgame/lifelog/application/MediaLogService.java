package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.core.support.IdGenerator;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import online.lifeasgame.lifelog.domain.*;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.MediaLogAdvanced;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MediaLogService {

    private final MediaLogReader mediaLogReader;
    private final MediaLogWriter mediaLogWriter;
    private final LifeLogRecordRegistrar lifeLogRecordRegistrar;
    private final DomainEventPublisher domainEventPublisher;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public MediaLogResult.Created create(MediaLogCommand.Create command) {
        return create(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public MediaLogResult.Created create(Long playerId, MediaLogCommand.Create command) {
        return create(
                playerId,
                command,
                LifeLogEntryMode.FULL,
                command.lifeLogMetadata()
        );
    }

    @Transactional
    public MediaLogResult.Created createQuick(
            Long playerId,
            MediaLogCommand.Create command,
            LifeLogRecordMetadataCommand metadata
    ) {
        return create(playerId, command, LifeLogEntryMode.QUICK, metadata);
    }

    private MediaLogResult.Created create(
            Long playerId,
            MediaLogCommand.Create command,
            LifeLogEntryMode entryMode,
            LifeLogRecordMetadataCommand metadata
    ) {
        MediaLog saved = mediaLogWriter.create(
                MediaLog.create(
                        playerId,
                        MediaCategory.parse(command.category()),
                        Title.of(command.title(), command.originalTitle()),
                        EpisodeProgress.of(command.currentEpisode(), command.totalEpisode()),
                        WatchStatus.parse(command.status()),
                        MediaTags.of(command.tags())
                )
        );

        LifeLogRecord record = lifeLogRecordRegistrar.register(
                playerId,
                LifeLogSourceType.MEDIA,
                saved.getId(),
                entryMode,
                metadata
        );
        Instant recordedAt = record.getOccurredAt();
        domainEventPublisher.publish(
                LifeLogRecorded.from(
                        IdGenerator.newEventId(),
                        record
                )
        );

        return new MediaLogResult.Created(
                saved.getId(),
                record.getId(),
                recordedAt
        );
    }

    @Transactional
    public MediaLogResult.Info rate(Long mediaId, MediaLogCommand.Rate command) {
        return rate(currentPlayerAccessor.currentPlayerIdOrThrow(), mediaId, command);
    }

    @Transactional
    public MediaLogResult.Info rate(Long playerId, Long mediaId, MediaLogCommand.Rate command) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);
        mediaLog.rate(command.score());
        return MediaLogResult.Info.from(mediaLog);
    }

    @Transactional
    public MediaLogResult.Info advance(Long mediaId, MediaLogCommand.Advance command) {
        return advance(currentPlayerAccessor.currentPlayerIdOrThrow(), mediaId, command);
    }

    @Transactional
    public MediaLogResult.Info advance(Long playerId, Long mediaId, MediaLogCommand.Advance command) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);

        int step = command.step() == null ? 1 : command.step();
        mediaLog.advanceEpisode(step);

        domainEventPublisher.publish(
                MediaLogAdvanced.of(
                        playerId,
                        mediaLog.getId(),
                        step,
                        mediaLog.getProgress().current(),
                        mediaLog.getProgress().total()
                )
        );

        return MediaLogResult.Info.from(mediaLog);
    }

    @Transactional
    public MediaLogResult.Info markStatus(Long mediaId, MediaLogCommand.MarkStatus command) {
        return markStatus(currentPlayerAccessor.currentPlayerIdOrThrow(), mediaId, command);
    }

    @Transactional
    public MediaLogResult.Info markStatus(Long playerId, Long mediaId, MediaLogCommand.MarkStatus command) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);
        mediaLog.markStatus(WatchStatus.parse(command.status()));
        return MediaLogResult.Info.from(mediaLog);
    }

    @Transactional
    public MediaLogResult.Info rewatch(Long mediaId) {
        return rewatch(currentPlayerAccessor.currentPlayerIdOrThrow(), mediaId);
    }

    @Transactional
    public MediaLogResult.Info rewatch(Long playerId, Long mediaId) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);
        mediaLog.rewatch();
        return MediaLogResult.Info.from(mediaLog);
    }

    @Transactional
    public MediaLogResult.Info update(Long mediaId, MediaLogCommand.Update command) {
        return update(currentPlayerAccessor.currentPlayerIdOrThrow(), mediaId, command);
    }

    @Transactional
    public MediaLogResult.Info update(Long playerId, Long mediaId, MediaLogCommand.Update command) {
        MediaCategory mediaCategory = MediaCategory.parse(command.category());
        Title title = Title.of(command.title(), command.originalTitle());
        EpisodeProgress episodeProgress = EpisodeProgress.of(command.currentEpisode(), command.totalEpisode());
        WatchStatus watchStatus = WatchStatus.parse(command.status());
        MediaTags tags = MediaTags.of(command.tags());

        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);

        mediaLog.update(
                mediaCategory,
                title,
                episodeProgress,
                watchStatus,
                tags
        );

        return MediaLogResult.Info.from(mediaLog);
    }

    @Transactional
    public MediaLogResult.Deleted delete(Long mediaId) {
        return delete(currentPlayerAccessor.currentPlayerIdOrThrow(), mediaId);
    }

    @Transactional
    public MediaLogResult.Deleted delete(Long playerId, Long mediaId) {
        mediaLogWriter.delete(playerId, mediaId);
        return new MediaLogResult.Deleted(mediaId);
    }
}
