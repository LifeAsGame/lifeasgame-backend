package online.lifeasgame.role.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.role.api.mapper.RoleRelationWebMapper;
import online.lifeasgame.role.api.request.RoleRelationRequest;
import online.lifeasgame.role.api.response.RoleRelationResponse;
import online.lifeasgame.role.api.spec.RoleRelationApiSpecV1;
import online.lifeasgame.role.application.RoleRelationQueryService;
import online.lifeasgame.role.application.RoleRelationService;
import online.lifeasgame.role.application.result.RoleRelationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles/{roleId}/relations")
public class RoleRelationController implements RoleRelationApiSpecV1 {

    private final RoleRelationService relationService;
    private final RoleRelationQueryService relationQueryService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<RoleRelationResponse.Detail>> create(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleRelationRequest.Create request
    ) {
        RoleRelationResult.Detail result = relationService.create(
                roleId,
                RoleRelationWebMapper.toCreateCommand(request)
        );
        return ApiResponses.created(
                URI.create("/api/v1/roles/" + roleId + "/relations/" + result.id()),
                RoleRelationWebMapper.toDetail(result)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleRelationResponse.Detail>>> list(
            @PathVariable Long roleId
    ) {
        return ApiResponses.ok(relationQueryService.list(roleId).stream()
                .map(RoleRelationWebMapper::toDetail)
                .toList());
    }

    @Override
    @GetMapping("/{relationId}")
    public ResponseEntity<ApiResponse<RoleRelationResponse.Detail>> detail(
            @PathVariable Long roleId,
            @PathVariable Long relationId
    ) {
        return ApiResponses.ok(RoleRelationWebMapper.toDetail(
                relationQueryService.detail(roleId, relationId)
        ));
    }

    @Override
    @PutMapping("/{relationId}")
    public ResponseEntity<ApiResponse<RoleRelationResponse.Detail>> update(
            @PathVariable Long roleId,
            @PathVariable Long relationId,
            @Valid @RequestBody RoleRelationRequest.Update request
    ) {
        return ApiResponses.ok(RoleRelationWebMapper.toDetail(
                relationService.update(
                        roleId,
                        relationId,
                        RoleRelationWebMapper.toUpdateCommand(request)
                )
        ));
    }

    @Override
    @DeleteMapping("/{relationId}")
    public ResponseEntity<ApiResponse<Void>> archive(
            @PathVariable Long roleId,
            @PathVariable Long relationId
    ) {
        relationService.archive(roleId, relationId);
        return ApiResponses.noContent();
    }
}
