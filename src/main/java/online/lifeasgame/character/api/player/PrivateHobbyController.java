package online.lifeasgame.character.api.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PrivateHobbyService;
import online.lifeasgame.character.application.PrivateHobbyService.Info;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.platform.web.validation.DatabaseCalendarDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/hobbies/private")
public class PrivateHobbyController {
    private final PrivateHobbyService service;

    public record Create(String name, String detail, Integer proficiency, String status,
                         @DatabaseCalendarDate LocalDate startedOn, Long personalCategoryId) {}

    public static final class Update {
        public String name;
        public String detail;
        public Integer proficiency;
        public String status;
        @DatabaseCalendarDate public LocalDate startedOn;
        public Long personalCategoryId;
        @JsonIgnore private boolean categoryProvided;

        @JsonSetter("personalCategoryId")
        public void setPersonalCategoryId(Long id) {
            personalCategoryId = id;
            categoryProvided = true;
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Info>>> list() {
        return ApiResponses.ok(service.list());
    }

    @GetMapping("/{ownedItemId}")
    public ResponseEntity<ApiResponse<Info>> get(@PathVariable Long ownedItemId) {
        return ApiResponses.ok(service.get(ownedItemId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Info>> create(@Valid @RequestBody Create request) {
        Info result = service.create(request.name(), request.detail(), request.proficiency(), request.status(),
                request.startedOn(), request.personalCategoryId());
        return ApiResponses.created(URI.create("/api/v1/players/hobbies/private/" + result.ownedItemId()), result);
    }

    @PatchMapping("/{ownedItemId}")
    public ResponseEntity<ApiResponse<Info>> update(@PathVariable Long ownedItemId, @Valid @RequestBody Update request) {
        return ApiResponses.ok(service.update(ownedItemId, request.name, request.detail, request.proficiency,
                request.status, request.startedOn, request.personalCategoryId, request.categoryProvided));
    }

    @DeleteMapping("/{ownedItemId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long ownedItemId) {
        service.delete(ownedItemId);
        return ApiResponses.noContent();
    }
}
