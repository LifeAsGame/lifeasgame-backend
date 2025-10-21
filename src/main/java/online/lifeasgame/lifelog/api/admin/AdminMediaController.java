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

    // Player-scoped
    @PostMapping("/{playerId}/media")
    @Override
    public ResponseEntity<AdminMediaResponse.Created> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminMediaRequest.Create request
    ) {
        MediaLogResult.Created created = mediaLogService.create(playerId, AdminMediaWebMapper.toCommand(request));
        return ResponseEntity.ok(AdminMediaWebMapper.toResponse(created));
    }

    @Override
    @GetMapping("/{playerId}/media/recent")
    public ResponseEntity<List<AdminMediaResponse.Info>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<MediaLogResult.Info> infos = mediaLogService.recent(playerId, limit);
        return ResponseEntity.ok(AdminMediaWebMapper.toResponseList(infos));
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
        List<MediaLogResult.Info> infos = mediaLogService.search(
                playerId,
                AdminMediaWebMapper.toCommand(
                        category,
                        status,
                        titleLike,
                        page,
                        size
                )
        );
        return ResponseEntity.ok(AdminMediaWebMapper.toResponseList(infos));
    }

    // MediaId-scoped
    @Override
    @PostMapping("/{playerId}/media/{mediaId}/rate")
    public ResponseEntity<AdminMediaResponse.Info> rate(
            @PathVariable Long playerId,
            @PathVariable Long mediaId,
            @Valid @RequestBody AdminMediaRequest.Rate request
    ) {
        MediaLogResult.Info info = mediaLogService.rate(playerId, mediaId, AdminMediaWebMapper.toCommand(request));
        return ResponseEntity.ok(AdminMediaWebMapper.toResponse(info));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/advance")
    public ResponseEntity<AdminMediaResponse.Info> advance(
            @PathVariable Long playerId,
            @PathVariable Long mediaId,
            @Valid @RequestBody AdminMediaRequest.Advance request
    ) {
        MediaLogResult.Info info = mediaLogService.advance(playerId, mediaId, AdminMediaWebMapper.toCommand(request));
        return ResponseEntity.ok(AdminMediaWebMapper.toResponse(info));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/status")
    public ResponseEntity<AdminMediaResponse.Info> markStatus(
            @PathVariable Long playerId,
            @PathVariable Long mediaId,
            @Valid @RequestBody AdminMediaRequest.MarkStatus request
    ) {
        MediaLogResult.Info info = mediaLogService.markStatus(playerId, mediaId, AdminMediaWebMapper.toCommand(request));
        return ResponseEntity.ok(AdminMediaWebMapper.toResponse(info));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/rewatch")
    public ResponseEntity<AdminMediaResponse.Info> rewatch(
            @PathVariable Long playerId,
            @PathVariable Long mediaId
    ) {
        MediaLogResult.Info info = mediaLogService.rewatch(playerId, mediaId);
        return ResponseEntity.ok(AdminMediaWebMapper.toResponse(info));
    }
}
