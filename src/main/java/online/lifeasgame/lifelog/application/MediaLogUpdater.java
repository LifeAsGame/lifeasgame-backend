package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.domain.EpisodeProgress;
import online.lifeasgame.lifelog.domain.MediaCategory;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.MediaTags;
import online.lifeasgame.lifelog.domain.Title;
import online.lifeasgame.lifelog.domain.WatchStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class MediaLogUpdater {

    private final MediaLogReader mediaLogReader;

    public MediaLog update(Long playerId, Long mediaId, MediaLogCommand.Update command) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);

        MediaCategory category = command.category() != null
                ? MediaCategory.parse(command.category())
                : mediaLog.getCategory();
        Title title = Title.of(
                command.title() != null ? command.title() : mediaLog.getTitle().value(),
                command.originalTitle() != null ? command.originalTitle() : mediaLog.getTitle().original()
        );
        EpisodeProgress progress = EpisodeProgress.of(
                command.currentEpisode() != null
                        ? command.currentEpisode()
                        : mediaLog.getProgress().current(),
                command.totalEpisode() != null
                        ? command.totalEpisode()
                        : mediaLog.getProgress().total()
        );
        WatchStatus status = command.status() != null
                ? WatchStatus.parse(command.status())
                : mediaLog.getStatus();
        MediaTags tags = command.tags() != null
                ? MediaTags.of(command.tags())
                : mediaLog.getMediaTags();

        mediaLog.update(category, title, progress, status, tags);
        return mediaLog;
    }
}
