package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.application.PersonalRoleGroupFinder;
import online.lifeasgame.social.application.PersonalRoleGroupLinker;
import online.lifeasgame.social.application.PersonalRoleGroupResult.*;
import online.lifeasgame.social.application.result.GuildResult;
import online.lifeasgame.social.domain.PersonalGroupType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles/{roleId}")
public class PersonalRoleGroupController {
    private final PersonalRoleGroupFinder finder;
    private final PersonalRoleGroupLinker linker;

    @GetMapping("/group-links")
    public ResponseEntity<ApiResponse<GuildResult.Page<Link>>> links(@PathVariable Long roleId,
            @RequestParam(required = false) PersonalGroupType groupType, @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = finder.links(roleId, groupType, keyword, page, size);
        return ApiResponses.ok(GuildResult.Page.of(result.getContent(), page, size, result.getTotalElements()));
    }

    @GetMapping("/group-link-candidates")
    public ResponseEntity<ApiResponse<GuildResult.Page<Group>>> candidates(@PathVariable Long roleId,
            @RequestParam PersonalGroupType groupType, @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = finder.candidates(roleId, groupType, keyword, page, size);
        return ApiResponses.ok(GuildResult.Page.of(result.getContent(), page, size, result.getTotalElements()));
    }

    @PostMapping("/group-links")
    public ResponseEntity<ApiResponse<Identity>> link(@PathVariable Long roleId, @Valid @RequestBody Create request) {
        return ApiResponses.ok(linker.link(roleId, request.groupType(), request.groupId()));
    }

    @DeleteMapping("/group-links/{linkId}")
    public ResponseEntity<ApiResponse<Void>> unlink(@PathVariable Long roleId, @PathVariable Long linkId) {
        linker.unlink(roleId, linkId);
        return ApiResponses.noContent();
    }

    public record Create(@NotNull PersonalGroupType groupType, @NotNull @Positive Long groupId) {}
}
