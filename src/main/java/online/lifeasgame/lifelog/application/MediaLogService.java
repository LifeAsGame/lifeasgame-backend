package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.model.MediaSpec;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.WatchStatus;
import online.lifeasgame.lifelog.domain.event.MediaLogAdvanced;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class MediaLogService {

    private final MediaLogReader mediaLogReader;
    private final MediaLogWriter mediaLogWriter;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public MediaLogResult.Created create(Long playerId, MediaLogCommand.Create command) {
        MediaLog saved = mediaLogWriter.create(
                MediaSpec.Create.from(playerId, command)
        );
        return MediaLogResult.Created.of(saved.getId());
    }

    @Transactional
    public MediaLogResult.Info rate(Long playerId, Long mediaId, MediaLogCommand.Rate command) {
        MediaLog mediaLog = mediaLogReader.getMediaLog(playerId, mediaId);
        mediaLogWriter.rate(mediaLog, command.score());
        return MediaLogResult.Info.from(mediaLog);
    }

    @Transactional
    public MediaLogResult.Info advance(Long playerId, Long mediaId, MediaLogCommand.Advance command) {
        MediaLog mediaLog = mediaLogReader.getMediaLog(playerId, mediaId);

        int step = command.step() == null ? 1 : command.step();
        mediaLogWriter.advance(mediaLog, step);

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
    public MediaLogResult.Info markStatus(Long playerId, Long mediaId, MediaLogCommand.MarkStatus command) {
        MediaLog mediaLog = mediaLogReader.getMediaLog(playerId, mediaId);
        mediaLogWriter.markStatus(mediaLog, WatchStatus.parse(command.status()));
        return MediaLogResult.Info.from(mediaLog);
    }

    @Transactional
    public MediaLogResult.Info rewatch(Long playerId, Long mediaId) {
        MediaLog mediaLog = mediaLogReader.getMediaLog(playerId, mediaId);
        mediaLogWriter.rewatch(mediaLog);
        return MediaLogResult.Info.from(mediaLog);
    }

    public List<MediaLogResult.Info> recent(Long playerId, int limit) {
        return mediaLogReader.recent(playerId, limit).stream()
                .map(MediaLogResult.Info::from)
                .toList();
    }

    public List<MediaLogResult.Info> search(Long playerId, MediaLogCommand.Search command) {
        return mediaLogReader.search(
                        playerId,
                        command.category(),
                        command.status(),
                        command.titleLike(),
                        command.page(),
                        command.size()
                ).stream()
                .map(MediaLogResult.Info::from)
                .toList();
    }
}
