package online.lifeasgame.inventory.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.ItemQueryService;
import online.lifeasgame.inventory.application.ItemService;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.api.admin.mapper.AdminItemWebMapper;
import online.lifeasgame.inventory.api.admin.request.AdminItemRequest;
import online.lifeasgame.inventory.api.admin.response.AdminItemResponse;
import online.lifeasgame.inventory.api.admin.spec.AdminItemApiSpecV1;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/items")
public class AdminItemController implements AdminItemApiSpecV1 {

    private final ItemService itemService;
    private final ItemQueryService itemQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AdminItemResponse.Page<AdminItemResponse.Summary>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String rarity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );
        ItemResult.Page<ItemResult.Summary> result = itemQueryService.search(
                name,
                category,
                type,
                rarity,
                pageable
        );
        return ApiResponses.ok(AdminItemWebMapper.toSummaryPage(result));
    }

    @Override
    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<AdminItemResponse.Detail>> getItem(
            @PathVariable Long itemId
    ) {
        ItemResult.Detail result = itemQueryService.getItem(itemId);
        if (!itemId.equals(result.id())) {
            throw new IllegalStateException("Item query result id mismatch");
        }
        return ApiResponses.ok(AdminItemWebMapper.toDetail(result));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminItemResponse.Id>> create(
            @Valid @RequestBody AdminItemRequest.Create request
    ) {
        ItemResult.Id result = itemService.create(AdminItemWebMapper.toCreateCommand(request));
        return ApiResponses.created(
                URI.create("/admin/v1/items/" + result.id()),
                AdminItemWebMapper.toInfo(result)
        );
    }

    @Override
    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<AdminItemResponse.Id>> update(
            @PathVariable Long itemId,
            @Valid @RequestBody AdminItemRequest.Update request
    ) {
        ItemResult.Id result = itemService.update(AdminItemWebMapper.toUpdateCommand(itemId, request));
        return ApiResponses.ok(AdminItemWebMapper.toInfo(result));
    }

    @Override
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<AdminItemResponse.Deleted>> delete(@PathVariable Long itemId) {
        ItemResult.Deleted result = itemService.delete(itemId);
        return ApiResponses.ok(AdminItemWebMapper.toDeleted(result));
    }
}
