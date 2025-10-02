package online.lifeasgame.inventory.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.ItemService;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.application.result.ItemResult.Summary;
import online.lifeasgame.inventory.api.player.mapper.ItemWebMapper;
import online.lifeasgame.inventory.api.player.response.ItemResponse;
import online.lifeasgame.inventory.api.player.spec.ItemApiSpecV1;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
public class ItemController implements ItemApiSpecV1 {

    private final ItemService itemService;

    @Override
    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemResponse.Detail>> getItem(@PathVariable Long itemId) {
        ItemResult.Detail detail = itemService.getItem(itemId);
        return ApiResponses.ok(ItemWebMapper.toDetail(detail));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<ItemResponse.Page<ItemResponse.Summary>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String rarity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        ItemResult.Page<Summary> summaryPage = itemService.search(name, category, type, rarity, pageable);
        return ApiResponses.ok(ItemWebMapper.toSummaryPage(summaryPage));
    }
}
