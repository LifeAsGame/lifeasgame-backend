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

    @Operation(summary = "내 알림 inbox 조회", description = "저장된 title/body와 각각의 copy ID·버전 및 copyLocale을 반환합니다. 조회 시 최신 template로 다시 렌더링하지 않습니다. 기존 알림의 metadata는 null이며 새 승인 버전을 소급 부여하지 않습니다. 신규 source는 QUEST_COMPLETED/QUEST_REWARD_READY만 허용되며 다른 type의 이력 조회와 구별됩니다.")
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
