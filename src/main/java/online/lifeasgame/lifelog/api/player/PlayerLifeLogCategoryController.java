package online.lifeasgame.lifelog.api.player;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.application.LifeLogCategoryService;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/lifelog/categories")
public class PlayerLifeLogCategoryController {
    private final LifeLogCategoryService service;

    public record SystemSelection(@NotBlank String kind, @NotBlank String systemCode) {}
    public record PersonalCreate(@NotBlank String kind, @NotBlank String name) {}
    public record PersonalRename(@NotBlank String name) {}

    @GetMapping("/system")
    public ResponseEntity<ApiResponse<List<String>>> systemCodes(@RequestParam String kind) {
        return ApiResponses.ok(service.systemCodes(kind));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LifeLogCategoryService.Category>>> mine(@RequestParam String kind) {
        return ApiResponses.ok(service.mine(kind));
    }

    @PostMapping("/system")
    public ResponseEntity<ApiResponse<LifeLogCategoryService.Category>> addSystem(@Valid @RequestBody SystemSelection request) {
        return ApiResponses.ok(service.addSystem(request.kind(), request.systemCode()));
    }

    @DeleteMapping("/system/{kind}/{code}")
    public ResponseEntity<Void> hideSystem(@PathVariable String kind, @PathVariable String code) {
        service.hideSystem(kind, code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/personal")
    public ResponseEntity<ApiResponse<LifeLogCategoryService.Category>> createPersonal(@Valid @RequestBody PersonalCreate request) {
        return ApiResponses.ok(service.createPersonal(request.kind(), request.name()));
    }

    @PatchMapping("/personal/{id}")
    public ResponseEntity<ApiResponse<LifeLogCategoryService.Category>> renamePersonal(@PathVariable Long id, @Valid @RequestBody PersonalRename request) {
        return ApiResponses.ok(service.renamePersonal(id, request.name()));
    }

    @DeleteMapping("/personal/{id}")
    public ResponseEntity<Void> deletePersonal(@PathVariable Long id) {
        service.deletePersonal(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/records/{kind}/{recordId}/personal-category")
    public ResponseEntity<ApiResponse<Map<String, Long>>> assign(@PathVariable String kind, @PathVariable Long recordId, @RequestBody JsonNode request) {
        if (request == null || !request.isObject() || !request.has("categoryId")) {
            throw new DomainException(LifeLogError.INVALID_PERSONAL_CATEGORY);
        }
        JsonNode value = request.get("categoryId");
        if (!value.isNull() && (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 1)) {
            throw new DomainException(LifeLogError.INVALID_PERSONAL_CATEGORY);
        }
        return ApiResponses.ok(java.util.Collections.singletonMap("personalCategoryId",
                service.assign(kind, recordId, value.isNull() ? null : value.longValue())));
    }
}
