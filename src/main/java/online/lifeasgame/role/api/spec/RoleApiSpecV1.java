package online.lifeasgame.role.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.role.api.request.RoleRequest;
import online.lifeasgame.role.api.response.RoleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Role API V1 (Player)")
public interface RoleApiSpecV1 {

    @Operation(summary = "Role 생성")
    ResponseEntity<ApiResponse<RoleResponse.Detail>> create(
            @Valid @RequestBody RoleRequest.Create request
    );

    @Operation(summary = "내 활성 Role 목록")
    ResponseEntity<ApiResponse<List<RoleResponse.Detail>>> list();

    @Operation(summary = "내 Role 상세")
    ResponseEntity<ApiResponse<RoleResponse.Detail>> detail(@PathVariable Long roleId);

    @Operation(summary = "내 Role 전체 수정")
    ResponseEntity<ApiResponse<RoleResponse.Detail>> update(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleRequest.Update request
    );

    @Operation(summary = "내 Role archive")
    ResponseEntity<ApiResponse<Void>> archive(@PathVariable Long roleId);
}
