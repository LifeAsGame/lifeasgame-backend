package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.application.GuildNoteService;
import online.lifeasgame.social.application.GuildNoteStore;
import online.lifeasgame.social.application.result.GuildResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GuildNoteController {
    private final GuildNoteService notes;

    @GetMapping("/api/v1/guilds/{guildId}/members/{memberPlayerId}/my-note")
    public ResponseEntity<ApiResponse<GuildNoteStore.Note>> find(@PathVariable Long guildId,
            @PathVariable Long memberPlayerId) {
        return ApiResponses.ok(notes.find(guildId, memberPlayerId));
    }

    @PutMapping("/api/v1/guilds/{guildId}/members/{memberPlayerId}/my-note")
    public ResponseEntity<ApiResponse<GuildNoteStore.Note>> put(@PathVariable Long guildId,
            @PathVariable Long memberPlayerId, @Valid @RequestBody Write request) {
        return ApiResponses.ok(notes.put(guildId, memberPlayerId, request.personId(), request.text(), request.version()));
    }

    @DeleteMapping("/api/v1/guild-notes/{noteId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long noteId) {
        notes.delete(noteId);
        return ApiResponses.noContent();
    }

    @GetMapping("/api/v1/persons/{personId}/guild-notes")
    public ResponseEntity<ApiResponse<GuildResult.Page<GuildNoteStore.Note>>> page(
            @PathVariable Long personId, @RequestParam(required = false) Long guildId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "true") boolean includeHistory,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = notes.page(personId, guildId, keyword, includeHistory, page, size);
        return ApiResponses.ok(GuildResult.Page.of(result.getContent(), page, size, result.getTotalElements()));
    }

    public record Write(@NotNull @Positive Long personId, String text, Long version) {}
}
