package online.lifeasgame.person.api;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLinkApi;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/persons")
public class PersonLinkController {
    private final PersonLinkApi links;
    private final CurrentPlayerAccessor currentPlayer;

    @DeleteMapping("/{personId}/linked-user")
    public ResponseEntity<ApiResponse<Void>> unlink(@PathVariable Long personId) {
        links.unlink(currentPlayer.currentPlayerIdOrThrow(), personId);
        return ApiResponses.noContent();
    }
}
