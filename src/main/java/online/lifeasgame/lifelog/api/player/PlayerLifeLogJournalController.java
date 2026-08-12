package online.lifeasgame.lifelog.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.mapper.PlayerLifeLogJournalWebMapper;
import online.lifeasgame.lifelog.api.player.response.PlayerLifeLogJournalResponse;
import online.lifeasgame.lifelog.api.player.spec.PlayerLifeLogJournalSpecV1;
import online.lifeasgame.lifelog.application.LifeLogJournalQueryService;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lifelogs")
public class PlayerLifeLogJournalController
        implements PlayerLifeLogJournalSpecV1 {

    private final LifeLogJournalQueryService journalQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PlayerLifeLogJournalResponse.Page>> list(
            @RequestParam(required = false) Long primaryRoleId,
            @RequestParam(required = false) String subtype,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LifeLogJournalResult.Page result = journalQueryService.list(
                primaryRoleId,
                subtype,
                page,
                size
        );
        return ApiResponses.ok(PlayerLifeLogJournalWebMapper.toPage(result));
    }

    @Override
    @GetMapping("/{lifeLogId}")
    public ResponseEntity<ApiResponse<PlayerLifeLogJournalResponse.Detail>>
    detail(@PathVariable Long lifeLogId) {
        return ApiResponses.ok(PlayerLifeLogJournalWebMapper.toDetail(
                journalQueryService.detail(lifeLogId)
        ));
    }
}
