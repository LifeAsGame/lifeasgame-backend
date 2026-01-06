package online.lifeasgame.lifelog.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.response.PlayerCollectionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface PlayerCollectionSpecV1 {

    @Operation(summary = "단건 조회")
    ResponseEntity<ApiResponse<PlayerCollectionResponse.Info>> get(
            @PathVariable Long collectionId
    );


    @Operation(summary = "컬렉션 삭제")
    ResponseEntity<ApiResponse<PlayerCollectionResponse.Deleted>> delete(
            @PathVariable Long collectionId
    );
}
