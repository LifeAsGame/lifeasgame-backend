package online.lifeasgame.quest.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.quest.api.player.request.QuestRouteRequest;
import online.lifeasgame.quest.api.player.response.QuestRouteResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Player Quest Route API V1")
public interface QuestRouteSpecV1 {

    @Operation(summary = "Quest Route 목록")
    ResponseEntity<ApiResponse<QuestRouteResponse.Routes>> routes();

    @Operation(summary = "Quest Route 상세")
    ResponseEntity<ApiResponse<QuestRouteResponse.Route>> route(
            @PathVariable Long routeId
    );

    @Operation(summary = "Quest Route 선택")
    ResponseEntity<ApiResponse<QuestRouteResponse.Route>> select(
            @PathVariable Long routeId
    );

    @Operation(summary = "내가 선택한 Quest Route 목록")
    ResponseEntity<ApiResponse<QuestRouteResponse.Routes>> myRoutes();

    @Operation(summary = "내 Quest Route 진행 상세")
    ResponseEntity<ApiResponse<QuestRouteResponse.Route>> myRoute(
            @PathVariable Long routeId
    );

    @Operation(summary = "내 Quest Route Step 상세")
    ResponseEntity<ApiResponse<QuestRouteResponse.StepDetail>> myStep(
            @PathVariable Long routeId,
            @PathVariable Long stepId
    );

    @Operation(summary = "현재 Quest Route Step을 한 단계 진행")
    ResponseEntity<ApiResponse<QuestRouteResponse.Route>> advance(
            @PathVariable Long routeId,
            @Valid @RequestBody QuestRouteRequest.Advance request
    );
}
