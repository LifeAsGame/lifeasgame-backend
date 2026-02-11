package online.lifeasgame.lifelog.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.mapper.PlayerCollectionWebMapper;
import online.lifeasgame.lifelog.api.player.request.PlayerCollectionRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerCollectionResponse;
import online.lifeasgame.lifelog.api.player.spec.PlayerCollectionSpecV1;
import online.lifeasgame.lifelog.application.CollectionLogFacade;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/collections")
public class PlayerCollectionController implements PlayerCollectionSpecV1 {

    private final CollectionLogFacade collectionLogFacade;

    @Override
    @GetMapping("/recent")
    public ResponseEntity<List<PlayerCollectionResponse.Info>> recent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        List<CollectionResult.Info> results = collectionLogFacade.recent(limit);
        return ResponseEntity.ok(PlayerCollectionWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<List<PlayerCollectionResponse.Info>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        List<CollectionResult.Info> results = collectionLogFacade.search(
                PlayerCollectionWebMapper.toSearchCommand(category, titleLike, page, size)
        );

        return ResponseEntity.ok(PlayerCollectionWebMapper.toInfos(results));
    }

    @Override
    @PostMapping
    public ResponseEntity<PlayerCollectionResponse.Created> create(
            @Valid @RequestBody PlayerCollectionRequest.Create request
    ) {
        CollectionResult.Created result = collectionLogFacade.create(PlayerCollectionWebMapper.toCreateCommand(request));
        return ResponseEntity.ok(PlayerCollectionWebMapper.toCreated(result));
    }

    @Override
    @PostMapping("/{collectionId}")
    public ResponseEntity<PlayerCollectionResponse.Info> update(
            @PathVariable Long collectionId,
            @Valid @RequestBody PlayerCollectionRequest.Update request
    ) {
        CollectionResult.Info result = collectionLogFacade.update(collectionId, PlayerCollectionWebMapper.toUpdateCommand(request));
        return ResponseEntity.ok(PlayerCollectionWebMapper.toInfo(result));
    }

    @Override
    @GetMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<PlayerCollectionResponse.Info>> get(
            @PathVariable Long collectionId
    ) {
        CollectionResult.Info result = collectionLogFacade.getCollection(collectionId);
        return ApiResponses.ok(PlayerCollectionWebMapper.toInfo(result));
    }

    @Override
    @DeleteMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<PlayerCollectionResponse.Deleted>> delete(
            @PathVariable Long collectionId
    ) {
        CollectionResult.Deleted result = collectionLogFacade.delete(collectionId);
        return ApiResponses.deleted(PlayerCollectionWebMapper.toDeleted(result));
    }
}
