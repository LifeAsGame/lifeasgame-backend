package online.lifeasgame.inventory.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.player.request.InventoryRequest;
import online.lifeasgame.inventory.api.player.response.InventoryResponse;
import online.lifeasgame.inventory.api.player.response.InventoryResponse.Entries;
import online.lifeasgame.inventory.api.player.response.InventoryResponse.Slot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Inventory API V1")
public interface InventoryApiSpecV1 {

    @Operation(summary = "인벤토리 목록 조회", description = "인벤토리 슬롯 목록(간단)을 조회합니다.")
    ResponseEntity<ApiResponse<Entries>> list();

    @Operation(summary = "인벤토리 엔트리 단건 조회", description = "itemInstanceId(=InventoryEntry.id) 기준으로 엔트리 상세를 조회합니다.")
    ResponseEntity<ApiResponse<InventoryResponse.EntryDetail>> getEntry(
            @PathVariable Long itemInstanceId,
            @RequestParam(defaultValue = "true") boolean includeItem,
            @RequestParam(defaultValue = "true") boolean includeInstanceAttrs
    );

    @Operation(summary = "인벤토리 아이템 추가", description = "아이템을 인벤토리에 추가합니다.")
    ResponseEntity<ApiResponse<InventoryResponse.Slots>> add(@Valid @RequestBody InventoryRequest.Add request);

    @Operation(summary = "인벤토리 슬롯 이동", description = "from 슬롯의 엔트리를 to 슬롯으로 이동합니다.")
    ResponseEntity<ApiResponse<Void>> move(@Valid @RequestBody InventoryRequest.Move request);

    @Operation(summary = "인벤토리 병합", description = "두 슬롯의 스택을 병합합니다.")
    ResponseEntity<ApiResponse<Void>> merge(@Valid @RequestBody InventoryRequest.Merge request);

    @Operation(summary = "인벤토리 분할", description = "한 슬롯의 스택을 다른 슬롯으로 분할합니다.")
    ResponseEntity<ApiResponse<Slot>> split(@Valid @RequestBody InventoryRequest.Split request);

    @Operation(summary = "인벤토리 아이템 제거", description = "해당 슬롯에서 수량만큼 제거합니다.")
    ResponseEntity<ApiResponse<Void>> remove(@Valid @RequestBody InventoryRequest.Remove request);
}
