package online.lifeasgame.character.api.admin;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AchievementReconciliation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/achievement-activation")
public class AdminAchievementReconciliationController {
    private final AchievementReconciliation reconciliation;

    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<AchievementReconciliation.Batch>> reconcile(
            @RequestBody Request request) {
        return ApiResponses.ok(reconciliation.run(
                request.afterPlayerId(), request.batchSize(), request.apply()));
    }

    public record Request(long afterPlayerId, int batchSize, boolean apply) {}
}
