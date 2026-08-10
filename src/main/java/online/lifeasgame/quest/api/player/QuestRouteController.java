package online.lifeasgame.quest.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.quest.api.player.mapper.QuestRouteWebMapper;
import online.lifeasgame.quest.api.player.request.QuestRouteRequest;
import online.lifeasgame.quest.api.player.response.QuestRouteResponse;
import online.lifeasgame.quest.api.player.spec.QuestRouteSpecV1;
import online.lifeasgame.quest.application.QuestRouteAdvanceService;
import online.lifeasgame.quest.application.QuestRouteQueryService;
import online.lifeasgame.quest.application.QuestRouteSelectService;
import online.lifeasgame.quest.application.result.QuestRouteResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/quest-routes")
public class QuestRouteController implements QuestRouteSpecV1 {

    private final QuestRouteSelectService selectService;
    private final QuestRouteAdvanceService advanceService;
    private final QuestRouteQueryService queryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<QuestRouteResponse.Routes>> routes() {
        return ApiResponses.ok(QuestRouteWebMapper.toRoutes(
                queryService.routes()
        ));
    }

    @Override
    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<QuestRouteResponse.Route>> route(
            @PathVariable Long routeId
    ) {
        return ApiResponses.ok(QuestRouteWebMapper.toRoute(
                queryService.route(routeId)
        ));
    }

    @Override
    @PostMapping("/{routeId}/select")
    public ResponseEntity<ApiResponse<QuestRouteResponse.Route>> select(
            @PathVariable Long routeId
    ) {
        QuestRouteResult.Route result = selectService.select(routeId);
        return ApiResponses.ok(QuestRouteWebMapper.toRoute(result));
    }

    @Override
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<QuestRouteResponse.Routes>> myRoutes() {
        return ApiResponses.ok(QuestRouteWebMapper.toRoutes(
                queryService.myRoutes()
        ));
    }

    @Override
    @GetMapping("/my/{routeId}")
    public ResponseEntity<ApiResponse<QuestRouteResponse.Route>> myRoute(
            @PathVariable Long routeId
    ) {
        return ApiResponses.ok(QuestRouteWebMapper.toRoute(
                queryService.myRoute(routeId)
        ));
    }

    @Override
    @GetMapping("/my/{routeId}/steps/{stepId}")
    public ResponseEntity<ApiResponse<QuestRouteResponse.StepDetail>> myStep(
            @PathVariable Long routeId,
            @PathVariable Long stepId
    ) {
        return ApiResponses.ok(QuestRouteWebMapper.toStepDetail(
                queryService.myStep(routeId, stepId)
        ));
    }

    @Override
    @PostMapping("/my/{routeId}/advance")
    public ResponseEntity<ApiResponse<QuestRouteResponse.Route>> advance(
            @PathVariable Long routeId,
            @Valid @RequestBody QuestRouteRequest.Advance request
    ) {
        QuestRouteResult.Route result = advanceService.advance(
                routeId,
                QuestRouteWebMapper.toExpectedStepId(request)
        );
        return ApiResponses.ok(QuestRouteWebMapper.toRoute(result));
    }
}
