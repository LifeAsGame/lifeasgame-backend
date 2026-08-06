package online.lifeasgame.role.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.role.api.mapper.RoleWebMapper;
import online.lifeasgame.role.api.request.RoleRequest;
import online.lifeasgame.role.api.response.RoleResponse;
import online.lifeasgame.role.api.spec.RoleApiSpecV1;
import online.lifeasgame.role.application.RoleFacade;
import online.lifeasgame.role.application.result.RoleResult;
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
@RequestMapping("/api/v1/roles")
public class RoleController implements RoleApiSpecV1 {

    private final RoleFacade facade;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse.Detail>> create(
            @Valid @RequestBody RoleRequest.Create request
    ) {
        RoleResult.Detail result = facade.create(RoleWebMapper.toCreateCommand(request));
        return ApiResponses.created(
                URI.create("/api/v1/roles/" + result.id()),
                RoleWebMapper.toDetail(result)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse.Detail>>> list() {
        return ApiResponses.ok(facade.list().stream()
                .map(RoleWebMapper::toDetail)
                .toList());
    }

    @Override
    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse.Detail>> detail(
            @PathVariable Long roleId
    ) {
        return ApiResponses.ok(RoleWebMapper.toDetail(facade.detail(roleId)));
    }

    @Override
    @PutMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse.Detail>> update(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleRequest.Update request
    ) {
        return ApiResponses.ok(RoleWebMapper.toDetail(
                facade.update(roleId, RoleWebMapper.toUpdateCommand(request))
        ));
    }

    @Override
    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable Long roleId) {
        facade.archive(roleId);
        return ApiResponses.noContent();
    }
}
