package online.lifeasgame.lifelog.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.admin.mapper.AdminCollectionWebMapper;
import online.lifeasgame.lifelog.api.admin.request.AdminCollectionRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminCollectionResponse;
import online.lifeasgame.lifelog.api.admin.spec.AdminCollectionSpecV1;
import online.lifeasgame.lifelog.application.CollectionLogService;
import online.lifeasgame.lifelog.application.CollectionLogQueryService;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminCollectionController implements AdminCollectionSpecV1 {

    private final CollectionLogService collectionLogService;
    private final CollectionLogQueryService collectionLogQueryService;

    @Override
    @GetMapping("/{playerId}/collections/recent")
    public ResponseEntity<ApiResponse<List<AdminCollectionResponse.Info>>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        List<CollectionResult.Info> results = collectionLogQueryService.recent(playerId, limit);
        return ApiResponses.ok(AdminCollectionWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{playerId}/collections/search")
    public ResponseEntity<ApiResponse<List<AdminCollectionResponse.Info>>> search(
            @PathVariable Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) @Max(100) int size
    ) {
        List<CollectionResult.Info> results = collectionLogQueryService.search(
                playerId,
                AdminCollectionWebMapper.toSearchQuery(category, titleLike, page, size)
        );

        return ApiResponses.ok(AdminCollectionWebMapper.toInfos(results));
    }

    @Override
    @PostMapping("/{playerId}/collections")
    public ResponseEntity<ApiResponse<AdminCollectionResponse.Created>> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminCollectionRequest.Create request
    ) {
        CollectionResult.Created result = collectionLogService.create(
                playerId,
                AdminCollectionWebMapper.toCreateCommand(request)
        );

        return ApiResponses.ok(AdminCollectionWebMapper.toCreated(result));
    }

    @Override
    @PostMapping("/{playerId}/collections/{collectionId}")
    public ResponseEntity<ApiResponse<AdminCollectionResponse.Info>> update(
            @PathVariable Long playerId,
            @PathVariable Long collectionId,
            @Valid @RequestBody AdminCollectionRequest.Update request
    ) {
        CollectionResult.Info result = collectionLogService.update(
                playerId,
                collectionId,
                AdminCollectionWebMapper.toUpdateCommand(request)
        );

        return ApiResponses.ok(AdminCollectionWebMapper.toInfo(result));
    }

    @Override
    @GetMapping("/{playerId}/collections/{collectionId}")
    public ResponseEntity<ApiResponse<AdminCollectionResponse.Info>> get(
            @PathVariable Long playerId,
            @PathVariable Long collectionId
    ) {
        CollectionResult.Info result = collectionLogQueryService.getCollection(playerId, collectionId);
        return ApiResponses.ok(AdminCollectionWebMapper.toInfo(result));
    }

    @Override
    @DeleteMapping("/{playerId}/collections/{collectionId}")
    public ResponseEntity<ApiResponse<AdminCollectionResponse.Deleted>> delete(
            @PathVariable Long playerId,
            @PathVariable Long collectionId
    ) {
        CollectionResult.Deleted result = collectionLogService.delete(playerId, collectionId);
        return ApiResponses.deleted(AdminCollectionWebMapper.toDeleted(result));
    }
}
