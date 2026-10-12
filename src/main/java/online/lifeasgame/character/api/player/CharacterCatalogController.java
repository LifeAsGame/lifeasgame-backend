package online.lifeasgame.character.api.player;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.CharacterCatalogService;
import online.lifeasgame.character.application.CharacterCatalogService.Category;
import online.lifeasgame.character.application.CharacterCatalogService.Item;
import online.lifeasgame.character.application.CharacterCatalogService.PageResult;
import online.lifeasgame.character.domain.PersonalCategory.Kind;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/catalog")
public class CharacterCatalogController {
    private final CharacterCatalogService service;

    @GetMapping("/{kind}/categories")
    public ResponseEntity<ApiResponse<List<Category>>> categories(@PathVariable Kind kind) {
        return ApiResponses.ok(service.categories(kind));
    }

    @GetMapping("/{kind}/items")
    public ResponseEntity<ApiResponse<PageResult<Item>>> search(@PathVariable Kind kind,
            @RequestParam(required = false) String q, @RequestParam(required = false) String majorCode,
            @RequestParam(required = false) String minorCode,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(service.search(kind, q, majorCode, minorCode, page, size));
    }

    @GetMapping("/{kind}/items/{catalogItemId}")
    public ResponseEntity<ApiResponse<Item>> get(@PathVariable Kind kind, @PathVariable Long catalogItemId) {
        return ApiResponses.ok(service.get(kind, catalogItemId));
    }
}
