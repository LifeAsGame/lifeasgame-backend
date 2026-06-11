package online.lifeasgame.lifelog.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.mapper.PlayerMediaLogWebMapper;
import online.lifeasgame.lifelog.api.player.request.PlayerMediaLogRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerMediaLogResponse;
import online.lifeasgame.lifelog.api.player.spec.PlayerMediaLogSpecV1;
import online.lifeasgame.lifelog.application.MediaLogFacade;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/media")
public class PlayerMediaLogController implements PlayerMediaLogSpecV1 {

    private final MediaLogFacade mediaLogFacade;

    @Override
    @GetMapping("/recent")
    public ResponseEntity<List<PlayerMediaLogResponse.Info>> recent(
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<MediaLogResult.Info> infos = mediaLogFacade.recent(limit);
        return ResponseEntity.ok(PlayerMediaLogWebMapper.toInfos(infos));
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<List<PlayerMediaLogResponse.Info>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<MediaLogResult.Info> infos = mediaLogFacade.search(
                PlayerMediaLogWebMapper.toSearchCommand(category, status, titleLike, page, size)
        );

        return ResponseEntity.ok(PlayerMediaLogWebMapper.toInfos(infos));
    }

    @Override
    @PostMapping
    public ResponseEntity<PlayerMediaLogResponse.Created> create(
            @Valid @RequestBody PlayerMediaLogRequest.Create request
    ) {
        MediaLogResult.Created result = mediaLogFacade.create(PlayerMediaLogWebMapper.toCreateCommand(request));
        return ResponseEntity.ok(PlayerMediaLogWebMapper.toCreated(result));
    }

    @Override
    @PostMapping("/{mediaId}/rate")
    public ResponseEntity<PlayerMediaLogResponse.Info> rate(
            @PathVariable Long mediaId,
            @Valid @RequestBody PlayerMediaLogRequest.Rate request
    ) {
        MediaLogResult.Info result = mediaLogFacade.rate(mediaId, PlayerMediaLogWebMapper.toRateCommand(request));
        return ResponseEntity.ok(PlayerMediaLogWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{mediaId}/advance")
    public ResponseEntity<PlayerMediaLogResponse.Info> advance(
            @PathVariable Long mediaId,
            @Valid @RequestBody PlayerMediaLogRequest.Advance request
    ) {
        MediaLogResult.Info result = mediaLogFacade.advance(mediaId, PlayerMediaLogWebMapper.toAdvanceCommand(request));
        return ResponseEntity.ok(PlayerMediaLogWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{mediaId}/status")
    public ResponseEntity<PlayerMediaLogResponse.Info> markStatus(
            @PathVariable Long mediaId,
            @Valid @RequestBody PlayerMediaLogRequest.MarkStatus request
    ) {
        MediaLogResult.Info result = mediaLogFacade.markStatus(mediaId, PlayerMediaLogWebMapper.toMarkStatusCommand(request));
        return ResponseEntity.ok(PlayerMediaLogWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{mediaId}/rewatch")
    public ResponseEntity<PlayerMediaLogResponse.Info> rewatch(@PathVariable Long mediaId) {
        MediaLogResult.Info result = mediaLogFacade.rewatch(mediaId);
        return ResponseEntity.ok(PlayerMediaLogWebMapper.toInfo(result));
    }

    @Override
    @PatchMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<PlayerMediaLogResponse.Info>> update(
            @PathVariable Long mediaId,
            @Valid @RequestBody PlayerMediaLogRequest.Update request
    ) {
        MediaLogResult.Info result= mediaLogFacade.update(mediaId, PlayerMediaLogWebMapper.toUpdateCommand(request));
        return ApiResponses.ok(PlayerMediaLogWebMapper.toInfo(result));
    }

    @Override
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<PlayerMediaLogResponse.Deleted>> delete(
            @PathVariable Long mediaId
    ) {
        MediaLogResult.Deleted result = mediaLogFacade.delete(mediaId);
        return ApiResponses.deleted(PlayerMediaLogWebMapper.toDeleted(result));
    }
}
