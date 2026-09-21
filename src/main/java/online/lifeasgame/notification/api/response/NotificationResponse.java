package online.lifeasgame.notification.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public final class NotificationResponse {

    private NotificationResponse() {
    }

    public record Page(
            List<Info> notifications,
            boolean hasMore,
            Long nextCursor
    ) {
    }

    @Schema(name = "NotificationInfo")
    public record Info(
            Long id,
            @Schema(description = "저장된 source type. 미지원/과거 type도 이력 조회 가능하며 신규 발행 허용을 뜻하지 않음")
            String type,
            String title,
            String body,
            @Schema(description = "저장 시 적용한 title copy ID. legacy는 null", nullable = true,
                    example = "notification.ntf_quest_completed.title") String titleCopyId,
            @Schema(description = "title copy 버전. source event/Quest 정의 버전과 별개. legacy는 null", nullable = true, example = "1") Integer titleCopyVersion,
            @Schema(description = "저장 시 적용한 body copy ID. legacy는 null", nullable = true,
                    example = "notification.ntf_quest_completed.body") String bodyCopyId,
            @Schema(description = "body copy 버전. legacy는 null", nullable = true, example = "1") Integer bodyCopyVersion,
            @Schema(description = "저장된 title/body의 locale. legacy는 null", nullable = true, example = "ko-KR") String copyLocale,
            Instant occurredAt,
            boolean read
    ) {
    }

    public record UnreadCount(long unreadCount) {
    }

    public record MarkedCount(int markedCount) {
    }
}
