package online.lifeasgame.person.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.person.api.request.PersonRequest;
import online.lifeasgame.person.api.response.PersonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Person API V1 (Player)")
public interface PersonApiSpecV1 {

    @Operation(summary = "Person 생성")
    ResponseEntity<ApiResponse<PersonResponse.Detail>> create(
            @Valid @RequestBody PersonRequest.Create request
    );

    @Operation(summary = "내 활성 Person 목록")
    ResponseEntity<ApiResponse<List<PersonResponse.Detail>>> list();

    @Operation(summary = "내 Person 상세")
    ResponseEntity<ApiResponse<PersonResponse.Detail>> detail(
            @PathVariable Long personId
    );

    @Operation(summary = "내 Person 전체 수정")
    ResponseEntity<ApiResponse<PersonResponse.Detail>> update(
            @PathVariable Long personId,
            @Valid @RequestBody PersonRequest.Update request
    );

    @Operation(summary = "내 Person archive")
    ResponseEntity<ApiResponse<Void>> archive(@PathVariable Long personId);
}
