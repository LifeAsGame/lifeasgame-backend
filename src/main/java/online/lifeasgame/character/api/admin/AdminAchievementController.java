package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminAchievementWebMapper;
import online.lifeasgame.character.api.admin.request.AdminAchievementRequest;
import online.lifeasgame.character.api.admin.response.AdminAchievementResponse;
import online.lifeasgame.character.api.admin.spec.AdminAchievementApiSpecV1;
import online.lifeasgame.character.application.AchievementService;
import online.lifeasgame.character.application.result.AchievementResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/achievements")
public class AdminAchievementController implements AdminAchievementApiSpecV1 {

    private final AchievementService achievementService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminAchievementResponse.Info>> create(
            @Valid @RequestBody AdminAchievementRequest.Create request
    ) {
        AchievementResult.Info result = achievementService.create(AdminAchievementWebMapper.toCreateCommand(request));

        return ApiResponses.created(
                URI.create("/admin/v1/achievements/" + result.code()),
                AdminAchievementWebMapper.toInfo(result)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AdminAchievementResponse.Infos>> list(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<AchievementResult.Info> results = achievementService.getAchievements(categories);
        return ApiResponses.ok(AdminAchievementWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{achievementId}")
    public ResponseEntity<ApiResponse<AdminAchievementResponse.Info>> get(
            @PathVariable Long achievementId
    ) {
        AchievementResult.Info result = achievementService.getAchievement(achievementId);
        return ApiResponses.ok(AdminAchievementWebMapper.toInfo(result));
    }

    @Override
    @PatchMapping("/{achievementId}")
    public ResponseEntity<ApiResponse<AdminAchievementResponse.Info>> update(
            Long achievementId,
            AdminAchievementRequest.Update request
    ) {
        AchievementResult.Info result = achievementService.update(achievementId, AdminAchievementWebMapper.toUpdateCommand(request));
        return ApiResponses.ok(AdminAchievementWebMapper.toInfo(result));
    }
}
