package online.lifeasgame.inventory.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.InventoryFacade;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.api.player.mapper.InventoryWebMapper;
import online.lifeasgame.inventory.api.player.request.InventoryRequest;
import online.lifeasgame.inventory.api.player.response.InventoryResponse;
import online.lifeasgame.inventory.api.player.spec.InventoryApiSpecV1;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory")
public class InventoryController implements InventoryApiSpecV1 {

    private final InventoryFacade inventoryFacade;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<InventoryResponse.Entries>> list() {
        InventoryResult.Entries result = inventoryFacade.list();
        return ApiResponses.ok(InventoryWebMapper.toEntries(result));
    }

    @Override
    @GetMapping("/{itemInstanceId}")
    public ResponseEntity<ApiResponse<InventoryResponse.EntryDetail>> getEntry(
            @PathVariable Long itemInstanceId,
            @RequestParam(defaultValue = "true") boolean includeItem,
            @RequestParam(defaultValue = "true") boolean includeInstanceAttrs
    ) {
        InventoryResult.Entry result = inventoryFacade.getEntry(itemInstanceId);
        return ApiResponses.ok(InventoryWebMapper.toEntryDetail(result));
    }
    @Override
    @PatchMapping("/move")
    public ResponseEntity<ApiResponse<Void>> move(@Valid @RequestBody InventoryRequest.Move request) {
        inventoryFacade.move(InventoryWebMapper.toMoveCommand(request));
        return ApiResponses.noContent();
    }

    @Override
    @PatchMapping("/merge")
    public ResponseEntity<ApiResponse<Void>> merge(@Valid @RequestBody InventoryRequest.Merge request) {
        inventoryFacade.merge(InventoryWebMapper.toMergeCommand(request));
        return ApiResponses.noContent();
    }

    @Override
    @PatchMapping("/split")
    public ResponseEntity<ApiResponse<InventoryResponse.Slot>> split(
            @Valid @RequestBody InventoryRequest.Split request
    ) {
        InventoryResult.Slot result = inventoryFacade.split(InventoryWebMapper.toSplitCommand(request));
        return ApiResponses.ok(InventoryWebMapper.toSlot(result));
    }

    @Override
    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> remove(@Valid @RequestBody InventoryRequest.Remove request) {
        inventoryFacade.remove(InventoryWebMapper.toRemoveCommand(request));
        return ApiResponses.noContent();
    }
}
