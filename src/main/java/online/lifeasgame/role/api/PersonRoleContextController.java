package online.lifeasgame.role.api;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.role.application.PersonRoleContextFinder;
import online.lifeasgame.role.application.query.PersonRoleContextQuery.Context;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/persons/{personId}/role-contexts")
public class PersonRoleContextController {
    private final PersonRoleContextFinder finder;

    @GetMapping
    public ResponseEntity<ApiResponse<ContextPage>> list(@PathVariable Long personId,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = finder.find(personId, includeArchived, keyword, page, size);
        return ApiResponses.ok(new ContextPage(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages()));
    }

    public record ContextPage(List<Context> contents, int page, int size, long totalElements, int totalPages) {}
}
