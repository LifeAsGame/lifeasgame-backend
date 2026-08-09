package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.query.MediaLogQuery;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import online.lifeasgame.lifelog.domain.MediaLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaLogQueryService {

    private final MediaLogReader mediaLogReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public List<MediaLogResult.Info> recent(int limit) {
        return recent(currentPlayerAccessor.currentPlayerIdOrThrow(), limit);
    }

    public List<MediaLogResult.Info> recent(Long playerId, int limit) {
        return mediaLogReader.recent(playerId, limit).stream()
                .map(MediaLogResult.Info::from)
                .toList();
    }

    public List<MediaLogResult.Info> search(MediaLogQuery.Search query) {
        return search(currentPlayerAccessor.currentPlayerIdOrThrow(), query);
    }

    public List<MediaLogResult.Info> search(Long playerId, MediaLogQuery.Search query) {
        return mediaLogReader.search(
                        playerId,
                        query.category(),
                        query.status(),
                        query.titleLike(),
                        query.page(),
                        query.size()
                ).stream()
                .map(MediaLogResult.Info::from)
                .toList();
    }

    public MediaLogResult.Info getMedia(Long mediaId) {
        return getMedia(currentPlayerAccessor.currentPlayerIdOrThrow(), mediaId);
    }

    public MediaLogResult.Info getMedia(Long playerId, Long mediaId) {
        MediaLog mediaLog = mediaLogReader.getByPlayerIdAndIdOrThrow(playerId, mediaId);
        return MediaLogResult.Info.from(mediaLog);
    }
}
