package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import online.lifeasgame.lifelog.domain.*;
import online.lifeasgame.lifelog.domain.event.MediaLogAdvanced;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MediaLogService {

    private final MediaLogReader mediaLogReader;
    private final MediaLogWriter mediaLogWriter;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public MediaLogResult.Created create(Long playerId, MediaLogCommand.Create command) {
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

        return new MediaLogResult.Created(saved.getId());
    }

    @Transactional
    public MediaLogResult.Info rate(Long playerId, Long mediaId, MediaLogCommand.Rate command) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);
        mediaLog.rate(command.score());
        return MediaLogResult.Info.from(mediaLog);
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
    public MediaLogResult.Info markStatus(Long playerId, Long mediaId, MediaLogCommand.MarkStatus command) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);
        mediaLog.markStatus(WatchStatus.parse(command.status()));
        return MediaLogResult.Info.from(mediaLog);
    }

    @Transactional
    public MediaLogResult.Info rewatch(Long playerId, Long mediaId) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);
        mediaLog.rewatch();
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
    public MediaLogResult.Deleted delete(Long playerId, Long mediaId) {
        mediaLogWriter.delete(playerId, mediaId);
        return new MediaLogResult.Deleted(mediaId);
    }

    public MediaLogResult.Info getMedia(Long playerId, Long mediaId) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);
        return MediaLogResult.Info.from(mediaLog);
    }
}
