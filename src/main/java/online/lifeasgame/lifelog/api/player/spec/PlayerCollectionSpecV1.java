package online.lifeasgame.lifelog.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.lifelog.api.player.request.PlayerCollectionRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerCollectionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "LifeLog Collection API V1 (Player)")
public interface PlayerCollectionSpecV1 {

    @Operation(summary = "최근 조회")
    ResponseEntity<List<PlayerCollectionResponse.Info>> recent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    );

    @Operation(summary = "검색")
    ResponseEntity<List<PlayerCollectionResponse.Info>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "컬렉션 등록")
    ResponseEntity<PlayerCollectionResponse.Created> create(
            @Valid @RequestBody PlayerCollectionRequest.Create request
    );

    @Operation(summary = "컬렉션 수정")
    ResponseEntity<PlayerCollectionResponse.Info> update(
            @PathVariable Long collectionId,
            @Valid @RequestBody PlayerCollectionRequest.Update request
    );
}
