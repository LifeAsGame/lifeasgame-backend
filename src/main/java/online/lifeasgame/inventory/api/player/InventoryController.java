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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory")
public class InventoryController implements InventoryApiSpecV1 {

    private final InventoryFacade inventoryFacade;

    @Override
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<InventoryResponse.Slots>> add(@Valid @RequestBody InventoryRequest.Add request) {
        InventoryResult.Slots result = inventoryFacade.add(InventoryWebMapper.toAddCommand(request));
        return ApiResponses.ok(InventoryWebMapper.toSlots(result));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<InventoryResponse.Entries>> list() {
        InventoryResult.Entries result = inventoryFacade.list();
        return ApiResponses.ok(InventoryWebMapper.toEntries(result));
    }

    @Override
    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> remove(@Valid @RequestBody InventoryRequest.Remove request) {
        inventoryFacade.remove(InventoryWebMapper.toRemoveCommand(request));
        return ApiResponses.noContent();
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
}
