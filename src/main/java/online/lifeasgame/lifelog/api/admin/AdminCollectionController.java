package online.lifeasgame.lifelog.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.api.admin.mapper.AdminCollectionWebMapper;
import online.lifeasgame.lifelog.api.admin.request.AdminCollectionRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminCollectionResponse;
import online.lifeasgame.lifelog.api.admin.spec.AdminCollectionSpecV1;
import online.lifeasgame.lifelog.application.CollectionLogService;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminCollectionController implements AdminCollectionSpecV1 {

    private final CollectionLogService collectionLogService;

    @GetMapping("/{playerId}/collections/recent")
    @Override
    public ResponseEntity<List<AdminCollectionResponse.Info>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        List<CollectionResult.Info> results = collectionLogService.recent(playerId, limit);
        return ResponseEntity.ok(AdminCollectionWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{playerId}/collections/search")
    public ResponseEntity<List<AdminCollectionResponse.Info>> search(
            @PathVariable Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) @Max(100) int size
    ) {
        List<CollectionResult.Info> results = collectionLogService.search(
                playerId,
                AdminCollectionWebMapper.toSearchCommand(category, titleLike, page, size)
        );

        return ResponseEntity.ok(AdminCollectionWebMapper.toInfos(results));
    }

    @PostMapping("/{playerId}/collections")
    @Override
    public ResponseEntity<AdminCollectionResponse.Created> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminCollectionRequest.Create request
    ) {
        CollectionResult.Created result = collectionLogService.create(
                playerId,
                AdminCollectionWebMapper.toCreateCommand(request)
        );

        return ResponseEntity.ok(AdminCollectionWebMapper.toCreated(result));
    }

    @PostMapping("/{playerId}/collections/{collectionId}")
    @Override
    public ResponseEntity<AdminCollectionResponse.Info> update(
            @PathVariable Long playerId,
            @PathVariable Long collectionId,
            @Valid @RequestBody AdminCollectionRequest.Update request
    ) {
        CollectionResult.Info result = collectionLogService.update(
                playerId,
                collectionId,
                AdminCollectionWebMapper.toUpdateCommand(request)
        );

        return ResponseEntity.ok(AdminCollectionWebMapper.toInfo(result));
    }
}
