package online.lifeasgame.lifelog.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.api.player.mapper.PlayerCollectionWebMapper;
import online.lifeasgame.lifelog.api.player.request.PlayerCollectionRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerCollectionResponse;
import online.lifeasgame.lifelog.api.player.spec.PlayerCollectionSpecV1;
import online.lifeasgame.lifelog.application.CollectionLogFacade;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/collections")
public class PlayerCollectionController implements PlayerCollectionSpecV1 {

    private final CollectionLogFacade collectionLogFacade;

    @Override
    @PostMapping
    public ResponseEntity<PlayerCollectionResponse.Created> create(
            @Valid @RequestBody PlayerCollectionRequest.Create request
    ) {
        CollectionResult.Created result = collectionLogFacade.create(PlayerCollectionWebMapper.toCommand(request));
        return ResponseEntity.ok(PlayerCollectionWebMapper.toResponse(result));
    }

    @Override
    @PostMapping("/{collectionId}")
    public ResponseEntity<PlayerCollectionResponse.Info> update(
            @PathVariable Long collectionId,
            @Valid @RequestBody PlayerCollectionRequest.Update request
    ) {
        CollectionResult.Info info = collectionLogFacade.update(
                collectionId,
                PlayerCollectionWebMapper.toCommand(request)
        );
        return ResponseEntity.ok(PlayerCollectionWebMapper.toResponse(info));
    }

    @Override
    @GetMapping("/recent")
    public ResponseEntity<List<PlayerCollectionResponse.Info>> recent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        List<CollectionResult.Info> infos = collectionLogFacade.recent(limit);
        return ResponseEntity.ok(PlayerCollectionWebMapper.toResponseList(infos));
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<List<PlayerCollectionResponse.Info>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        List<CollectionResult.Info> infos = collectionLogFacade.search(
                PlayerCollectionWebMapper.toCommand(
                        category,
                        titleLike,
                        page,
                        size
                )
        );
        return ResponseEntity.ok(PlayerCollectionWebMapper.toResponseList(infos));
    }
}
