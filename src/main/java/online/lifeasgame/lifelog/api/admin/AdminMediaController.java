package online.lifeasgame.lifelog.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.api.admin.mapper.AdminMediaWebMapper;
import online.lifeasgame.lifelog.api.admin.request.AdminMediaRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminMediaResponse;
import online.lifeasgame.lifelog.api.admin.spec.AdminMediaSpecV1;
import online.lifeasgame.lifelog.application.MediaLogService;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminMediaController implements AdminMediaSpecV1 {

    private final MediaLogService mediaLogService;

    @Override
    @GetMapping("/{playerId}/media/recent")
    public ResponseEntity<List<AdminMediaResponse.Info>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<MediaLogResult.Info> results = mediaLogService.recent(playerId, limit);
        return ResponseEntity.ok(AdminMediaWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{playerId}/media/search")
    public ResponseEntity<List<AdminMediaResponse.Info>> search(
            @PathVariable Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<MediaLogResult.Info> results = mediaLogService.search(
                playerId,
                AdminMediaWebMapper.toSearchCommand(category, status, titleLike, page, size)
        );

        return ResponseEntity.ok(AdminMediaWebMapper.toInfos(results));
    }

    @PostMapping("/{playerId}/media")
    @Override
    public ResponseEntity<AdminMediaResponse.Created> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminMediaRequest.Create request
    ) {
        MediaLogResult.Created result = mediaLogService.create(playerId, AdminMediaWebMapper.toCreateCommand(request));
        return ResponseEntity.ok(AdminMediaWebMapper.toCreated(result));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/rate")
    public ResponseEntity<AdminMediaResponse.Info> rate(
            @PathVariable Long playerId,
            @PathVariable Long mediaId,
            @Valid @RequestBody AdminMediaRequest.Rate request
    ) {
        MediaLogResult.Info result = mediaLogService.rate(playerId, mediaId, AdminMediaWebMapper.toRateCommand(request));
        return ResponseEntity.ok(AdminMediaWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/advance")
    public ResponseEntity<AdminMediaResponse.Info> advance(
            @PathVariable Long playerId,
            @PathVariable Long mediaId,
            @Valid @RequestBody AdminMediaRequest.Advance request
    ) {
        MediaLogResult.Info result = mediaLogService.advance(playerId, mediaId, AdminMediaWebMapper.toAdvanceCommand(request));
        return ResponseEntity.ok(AdminMediaWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/status")
    public ResponseEntity<AdminMediaResponse.Info> markStatus(
            @PathVariable Long playerId,
            @PathVariable Long mediaId,
            @Valid @RequestBody AdminMediaRequest.MarkStatus request
    ) {
        MediaLogResult.Info result = mediaLogService.markStatus(
                playerId,
                mediaId,
                AdminMediaWebMapper.toMarkStatusCommand(request)
        );

        return ResponseEntity.ok(AdminMediaWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/rewatch")
    public ResponseEntity<AdminMediaResponse.Info> rewatch(
            @PathVariable Long playerId,
            @PathVariable Long mediaId
    ) {
        MediaLogResult.Info result = mediaLogService.rewatch(playerId, mediaId);
        return ResponseEntity.ok(AdminMediaWebMapper.toInfo(result));
    }
}
