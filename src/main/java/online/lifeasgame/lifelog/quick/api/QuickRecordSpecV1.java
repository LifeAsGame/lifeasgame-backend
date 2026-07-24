package online.lifeasgame.lifelog.quick.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Quick Record API V1 (Player)")
public interface QuickRecordSpecV1 {

    @Operation(summary = "기존 LifeLog subtype을 빠르게 기록")
    ResponseEntity<ApiResponse<QuickRecordResponse.Recorded>> record(
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey,
            @Valid @RequestBody QuickRecordRequest.Create request
    );
}
