package online.lifeasgame.character.api.player;

import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.PlayerCertificationWebMapper;
import online.lifeasgame.character.api.player.mapper.PlayerHobbyWebMapper;
import online.lifeasgame.character.application.PersonalCategoryService;
import online.lifeasgame.character.application.PersonalCategoryService.Assignment;
import online.lifeasgame.character.application.PersonalCategoryService.Category;
import online.lifeasgame.character.domain.PersonalCategory.Kind;
import online.lifeasgame.character.domain.error.PersonalCategoryError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/categories")
public class PersonalCategoryController {
    private final PersonalCategoryService service;

    public record NameRequest(String name) {}

    @GetMapping("/{kind}/owned")
    public ResponseEntity<ApiResponse<List<Category>>> owned(@PathVariable Kind kind) {
        return ApiResponses.ok(service.owned(kind));
    }

    @GetMapping("/{kind}")
    public ResponseEntity<ApiResponse<List<Category>>> list(@PathVariable Kind kind) {
        return ApiResponses.ok(service.list(kind));
    }

    @PostMapping("/{kind}")
    public ResponseEntity<ApiResponse<Category>> create(@PathVariable Kind kind, @RequestBody NameRequest request) {
        Category created = service.create(kind, request.name());
        return ApiResponses.created(URI.create("/api/v1/players/categories/" + kind + "/" + created.id()), created);
    }

    @PatchMapping("/{kind}/{id}")
    public ResponseEntity<ApiResponse<Category>> rename(@PathVariable Kind kind, @PathVariable Long id,
                                                          @RequestBody NameRequest request) {
        return ApiResponses.ok(service.rename(kind, id, request.name()));
    }

    @DeleteMapping("/{kind}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Kind kind, @PathVariable Long id) {
        service.delete(kind, id);
        return ApiResponses.noContent();
    }

    @PatchMapping("/{kind}/items/{itemId}/personal-category")
    public ResponseEntity<ApiResponse<Assignment>> assign(@PathVariable Kind kind, @PathVariable Long itemId,
                                                            @RequestBody Map<String, Long> request) {
        if (!request.containsKey("personalCategoryId")) {
            throw new DomainException(PersonalCategoryError.INVALID_ASSIGNMENT);
        }
        return ApiResponses.ok(service.assign(kind, itemId, request.get("personalCategoryId")));
    }

    @GetMapping("/{kind}/{id}/items")
    public ResponseEntity<ApiResponse<Object>> personalItems(@PathVariable Kind kind, @PathVariable Long id) {
        return ApiResponses.ok(kind == Kind.CERTIFICATION
                ? PlayerCertificationWebMapper.toInfos(service.certificationItems(id, null))
                : PlayerHobbyWebMapper.toInfos(service.hobbyItems(id, null)));
    }

    @GetMapping("/{kind}/system/{code}/items")
    public ResponseEntity<ApiResponse<Object>> systemItems(@PathVariable Kind kind, @PathVariable String code) {
        return ApiResponses.ok(kind == Kind.CERTIFICATION
                ? PlayerCertificationWebMapper.toInfos(service.certificationItems(null, code))
                : PlayerHobbyWebMapper.toInfos(service.hobbyItems(null, code)));
    }
}
