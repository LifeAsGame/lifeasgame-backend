package online.lifeasgame.inventory.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.ItemService;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.api.admin.mapper.AdminItemWebMapper;
import online.lifeasgame.inventory.api.admin.request.AdminItemRequest;
import online.lifeasgame.inventory.api.admin.response.AdminItemResponse;
import online.lifeasgame.inventory.api.admin.spec.AdminItemApiSpecV1;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/items")
public class AdminItemController implements AdminItemApiSpecV1 {

    private final ItemService itemService;

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
