package online.lifeasgame.role.api;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.role.application.RoleScheduleFinder;
import online.lifeasgame.role.application.query.RoleScheduleQuery.Source;
import online.lifeasgame.role.application.query.RoleScheduleQuery.Status;
import online.lifeasgame.role.application.query.RoleScheduleQuery.TimeMode;
import online.lifeasgame.role.application.result.RoleScheduleResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles/{roleId}/schedule")
public class RoleScheduleController {
    private final RoleScheduleFinder finder;

    @GetMapping
    public ResponseEntity<ApiResponse<RoleScheduleResult.Page>> list(
            @PathVariable Long roleId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "DATED") TimeMode time,
            @RequestParam(defaultValue = "ALL") Source source,
            @RequestParam(defaultValue = "PLANNED") Status status,
            @RequestParam(defaultValue = "false") boolean participating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(finder.find(roleId, from == null ? null : from.toInstant(),
                to == null ? null : to.toInstant(), time, source, status, participating, page, size));
    }
}
