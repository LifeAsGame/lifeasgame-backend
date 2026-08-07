package online.lifeasgame.role.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.role.api.request.RoleRelationRequest;
import online.lifeasgame.role.api.response.RoleRelationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Role Relation API V1 (Player)")
public interface RoleRelationApiSpecV1 {

    @Operation(summary = "Role에 Person 관계 생성")
    ResponseEntity<ApiResponse<RoleRelationResponse.Detail>> create(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleRelationRequest.Create request
    );

    @Operation(summary = "Role의 활성 관계 목록")
    ResponseEntity<ApiResponse<List<RoleRelationResponse.Detail>>> list(
            @PathVariable Long roleId
    );

    @Operation(summary = "Role 관계 상세")
    ResponseEntity<ApiResponse<RoleRelationResponse.Detail>> detail(
            @PathVariable Long roleId,
            @PathVariable Long relationId
    );

    @Operation(summary = "Role 관계 수정")
    ResponseEntity<ApiResponse<RoleRelationResponse.Detail>> update(
            @PathVariable Long roleId,
            @PathVariable Long relationId,
            @Valid @RequestBody RoleRelationRequest.Update request
    );

    @Operation(summary = "Role 관계 archive")
    ResponseEntity<ApiResponse<Void>> archive(
            @PathVariable Long roleId,
            @PathVariable Long relationId
    );
}
