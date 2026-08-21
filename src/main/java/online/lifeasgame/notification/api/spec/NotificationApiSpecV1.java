package online.lifeasgame.notification.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.notification.api.response.NotificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Notification API V1 (Player)")
public interface NotificationApiSpecV1 {

    @Operation(summary = "내 알림 inbox 조회")
    ResponseEntity<ApiResponse<NotificationResponse.Page>> inbox(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @Operation(summary = "내 unread 알림 수 조회")
    ResponseEntity<ApiResponse<NotificationResponse.UnreadCount>> unreadCount();

    @Operation(summary = "내 알림 하나를 읽음 처리")
    ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable Long notificationId
    );

    @Operation(summary = "내 unread 알림 전체 읽음 처리")
    ResponseEntity<ApiResponse<NotificationResponse.MarkedCount>> markAllRead();
}
