package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminHobbyWebMapper;
import online.lifeasgame.character.api.admin.request.AdminHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminHobbyResponse;
import online.lifeasgame.character.api.admin.spec.AdminHobbyApiSpecV1;
import online.lifeasgame.character.application.HobbyService;
import online.lifeasgame.character.application.result.HobbyResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/hobbies")
public class AdminHobbyController implements AdminHobbyApiSpecV1 {

    private final HobbyService hobbyService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminHobbyResponse.Info>> create(
            @Valid @RequestBody AdminHobbyRequest.Create request
    ) {
        HobbyResult.Info result = hobbyService.create(AdminHobbyWebMapper.toCreateCommand(request));
        return ApiResponses.created(
                URI.create("/admin/v1/hobbies/"),
                AdminHobbyWebMapper.toInfo(result)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AdminHobbyResponse.Infos>> list(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<HobbyResult.Info> results = hobbyService.getHobbies(categories);
        return ApiResponses.ok(AdminHobbyWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{hobbyId}")
    public ResponseEntity<ApiResponse<AdminHobbyResponse.Info>> get(
            @PathVariable Long hobbyId
    ) {
        HobbyResult.Info result = hobbyService.getHobby(hobbyId);
        return ApiResponses.ok(AdminHobbyWebMapper.toInfo(result));
    }

    @Override
    @PatchMapping("/{hobbyId}")
    public ResponseEntity<ApiResponse<AdminHobbyResponse.Info>> update(
            @PathVariable Long hobbyId,
            @Valid @RequestBody AdminHobbyRequest.Update request
    ) {
        HobbyResult.Info result = hobbyService.update(hobbyId, AdminHobbyWebMapper.toUpdateCommand(request));
        return ApiResponses.ok(AdminHobbyWebMapper.toInfo(result));
    }
}
