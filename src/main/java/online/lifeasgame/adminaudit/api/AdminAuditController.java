package online.lifeasgame.adminaudit.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.api.response.AdminAuditResponse;
import online.lifeasgame.adminaudit.api.spec.AdminAuditApiSpecV1;
import online.lifeasgame.adminaudit.application.AdminAuditQueryService;
import online.lifeasgame.adminaudit.application.result.AdminAuditQueryResult;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/audit-events")
public class AdminAuditController implements AdminAuditApiSpecV1 {

    private final AdminAuditQueryService queryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AdminAuditResponse.Page>> list(
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
    ) {
        AdminAuditQueryResult.Page page = queryService.list(
                actorUserId,
                action,
                targetType,
                targetId,
                result,
                correlationId,
                from,
                to,
                cursor,
                size
        );
        return ApiResponses.ok(new AdminAuditResponse.Page(
                page.items().stream().map(AdminAuditController::toResponse).toList(),
                page.nextCursor()
        ));
    }

    private static AdminAuditResponse.Item toResponse(
            AdminAuditQueryResult.Item item
    ) {
        return new AdminAuditResponse.Item(
                item.id(),
                item.actorUserId(),
                item.action(),
                item.targetType(),
                item.targetId(),
                item.reason(),
                item.result(),
                item.correlationId(),
                item.idempotencyKey(),
                item.occurredAt()
        );
    }
}
