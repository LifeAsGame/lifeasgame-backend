package online.lifeasgame.character.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.ActivatedContentQuery;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/activated-content")
public class ActivatedContentController {
    private final ActivatedContentQuery query;

    @GetMapping
    public ResponseEntity<ApiResponse<ActivatedContentQuery.Page>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size) {
        return ApiResponses.ok(query.list(page, size));
    }
}
