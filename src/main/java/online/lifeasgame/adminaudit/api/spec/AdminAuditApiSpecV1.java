package online.lifeasgame.adminaudit.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import online.lifeasgame.adminaudit.api.response.AdminAuditResponse;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

@Tag(name = "Admin Audit API V1")
public interface AdminAuditApiSpecV1 {

    @Operation(summary = "Admin audit event 목록 조회")
    ResponseEntity<ApiResponse<AdminAuditResponse.Page>> list(
            @RequestParam(required = false) @Positive Long actorUserId,
            @RequestParam(required = false)
            @Pattern(regexp = "[A-Z][A-Z0-9_]{2,63}") String action,
            @RequestParam(required = false)
            @Pattern(regexp = "[A-Z][A-Z0-9_]{2,63}") String targetType,
            @RequestParam(required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            String targetId,
            @RequestParam(required = false) AdminAuditResult result,
            @RequestParam(required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")
            String correlationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) @Size(min = 1, max = 256)
            String cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    );
}
