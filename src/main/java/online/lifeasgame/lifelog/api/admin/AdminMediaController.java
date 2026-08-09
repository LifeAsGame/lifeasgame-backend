package online.lifeasgame.lifelog.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.admin.mapper.AdminMediaWebMapper;
import online.lifeasgame.lifelog.api.admin.request.AdminMediaRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminMediaResponse;
import online.lifeasgame.lifelog.api.admin.spec.AdminMediaSpecV1;
import online.lifeasgame.lifelog.application.MediaLogService;
import online.lifeasgame.lifelog.application.MediaLogQueryService;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminMediaController implements AdminMediaSpecV1 {

    private final MediaLogService mediaLogService;
    private final MediaLogQueryService mediaLogQueryService;

    @Override
    @GetMapping("/{playerId}/media/recent")
    public ResponseEntity<ApiResponse<List<AdminMediaResponse.Info>>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<MediaLogResult.Info> results = mediaLogQueryService.recent(playerId, limit);
        return ApiResponses.ok(AdminMediaWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{playerId}/media/search")
    public ResponseEntity<ApiResponse<List<AdminMediaResponse.Info>>> search(
            @PathVariable Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<MediaLogResult.Info> results = mediaLogQueryService.search(
                playerId,
                AdminMediaWebMapper.toSearchQuery(category, status, titleLike, page, size)
        );

        return ApiResponses.ok(AdminMediaWebMapper.toInfos(results));
    }

    @PostMapping("/{playerId}/media")
    @Override
    public ResponseEntity<ApiResponse<AdminMediaResponse.Created>> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminMediaRequest.Create request
    ) {
        MediaLogResult.Created result = mediaLogService.create(playerId, AdminMediaWebMapper.toCreateCommand(request));
        return ApiResponses.ok(AdminMediaWebMapper.toCreated(result));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/rate")
    public ResponseEntity<ApiResponse<AdminMediaResponse.Info>> rate(
            @PathVariable Long playerId,
            @PathVariable Long mediaId,
            @Valid @RequestBody AdminMediaRequest.Rate request
    ) {
        MediaLogResult.Info result = mediaLogService.rate(playerId, mediaId, AdminMediaWebMapper.toRateCommand(request));
        return ApiResponses.ok(AdminMediaWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/advance")
    public ResponseEntity<ApiResponse<AdminMediaResponse.Info>> advance(
            @PathVariable Long playerId,
            @PathVariable Long mediaId,
            @Valid @RequestBody AdminMediaRequest.Advance request
    ) {
        MediaLogResult.Info result = mediaLogService.advance(playerId, mediaId, AdminMediaWebMapper.toAdvanceCommand(request));
        return ApiResponses.ok(AdminMediaWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/status")
    public ResponseEntity<ApiResponse<AdminMediaResponse.Info>> markStatus(
            @PathVariable Long playerId,
            @PathVariable Long mediaId,
            @Valid @RequestBody AdminMediaRequest.MarkStatus request
    ) {
        MediaLogResult.Info result = mediaLogService.markStatus(
                playerId,
                mediaId,
                AdminMediaWebMapper.toMarkStatusCommand(request)
        );

        return ApiResponses.ok(AdminMediaWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{playerId}/media/{mediaId}/rewatch")
    public ResponseEntity<ApiResponse<AdminMediaResponse.Info>> rewatch(
            @PathVariable Long playerId,
            @PathVariable Long mediaId
    ) {
        MediaLogResult.Info result = mediaLogService.rewatch(playerId, mediaId);
        return ApiResponses.ok(AdminMediaWebMapper.toInfo(result));
    }

    @Override
    @GetMapping("/{playerId}/media/{mediaId}")
    public ResponseEntity<ApiResponse<AdminMediaResponse.Info>> get(
            @PathVariable Long playerId,
            @PathVariable Long mediaId
    ) {
        MediaLogResult.Info result = mediaLogQueryService.getMedia(playerId, mediaId);
        return ApiResponses.ok(AdminMediaWebMapper.toInfo(result));
    }

    @Override
    @DeleteMapping("/{playerId}/media/{mediaId}")
    public ResponseEntity<ApiResponse<AdminMediaResponse.Deleted>> delete(
            @PathVariable Long playerId,
            @PathVariable Long mediaId
    ) {
        MediaLogResult.Deleted result = mediaLogService.delete(playerId, mediaId);
        return ApiResponses.deleted(AdminMediaWebMapper.toDeleted(result.mediaId()));
    }
}
