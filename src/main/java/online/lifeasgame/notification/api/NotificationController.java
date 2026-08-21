package online.lifeasgame.notification.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.notification.api.mapper.NotificationWebMapper;
import online.lifeasgame.notification.api.response.NotificationResponse;
import online.lifeasgame.notification.api.spec.NotificationApiSpecV1;
import online.lifeasgame.notification.application.NotificationQueryService;
import online.lifeasgame.notification.application.NotificationReadMarker;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationApiSpecV1 {

    private final NotificationQueryService queryService;
    private final NotificationReadMarker readMarker;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationResponse.Page>> inbox(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponses.ok(NotificationWebMapper.toPage(
                queryService.inbox(cursor, size)
        ));
    }

    @Override
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationResponse.UnreadCount>>
    unreadCount() {
        return ApiResponses.ok(NotificationWebMapper.toUnreadCount(
                queryService.unreadCount()
        ));
    }

    @Override
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable Long notificationId
    ) {
        readMarker.markOne(notificationId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<NotificationResponse.MarkedCount>>
    markAllRead() {
        return ApiResponses.ok(NotificationWebMapper.toMarkedCount(
                readMarker.markAll()
        ));
    }
}
