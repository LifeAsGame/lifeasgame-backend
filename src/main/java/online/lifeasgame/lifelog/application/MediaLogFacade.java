package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MediaLogFacade {

    private final MediaLogService mediaLogService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public MediaLogResult.Created create(MediaLogCommand.Create command) {
        Long playerId = getPlayer();
        return mediaLogService.create(playerId, command);
    }

    public MediaLogResult.Info rate(Long mediaId, MediaLogCommand.Rate command) {
        Long playerId = getPlayer();
        return mediaLogService.rate(playerId, mediaId, command);
    }

    public MediaLogResult.Info advance(Long mediaId, MediaLogCommand.Advance command) {
        Long playerId = getPlayer();
        return mediaLogService.advance(playerId, mediaId, command);
    }

    public MediaLogResult.Info markStatus(Long mediaId, MediaLogCommand.MarkStatus command) {
        Long playerId = getPlayer();
        return mediaLogService.markStatus(playerId, mediaId, command);
    }

    public MediaLogResult.Info rewatch(Long mediaId) {
        Long playerId = getPlayer();
        return mediaLogService.rewatch(playerId, mediaId);
    }

    public List<MediaLogResult.Info> search(MediaLogCommand.Search command) {
        Long playerId = getPlayer();
        return mediaLogService.search(playerId, command);
    }

    public List<MediaLogResult.Info> recent( Integer limit) {
        Long playerId = getPlayer();
        return mediaLogService.recent(playerId, limit);
    }

    private Long getPlayer() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }

    public MediaLogResult.Info update(Long mediaId, MediaLogCommand.Update command) {
        Long playerId = getPlayer();
        return mediaLogService.update(playerId, mediaId, command);
    }

    public MediaLogResult.Deleted delete(Long mediaId) {
        Long playerId = getPlayer();
        return mediaLogService.delete(playerId, mediaId);
    }
}
