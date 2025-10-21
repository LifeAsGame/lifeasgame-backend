package online.lifeasgame.lifelog.application.model;

import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.domain.*;

import java.util.Set;

public final class MediaSpec {

    private MediaSpec() {
    }

    public record Create(
            Long playerId,
            MediaCategory category,
            Title title,
            EpisodeProgress progress,
            WatchStatus status,
            MediaTags mediaTags
    ) {
        public static Create from(Long playerId, MediaLogCommand.Create command) {
            return new Create(
                    playerId,
                    MediaCategory.parse(command.category()),
                    (command.originalTitle() == null || command.originalTitle().isBlank())
                            ? Title.of(command.title())
                            : Title.of(command.title(), command.originalTitle()),
                    EpisodeProgress.of(command.currentEpisode(), command.totalEpisode()),
                    WatchStatus.parse(command.status()),
                    MediaTags.of(command.tags() == null ? Set.of() : command.tags())
            );
        }
    }
}
