package online.lifeasgame.adminaudit.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Admin Audit event reason safety")
class AdminAuditEventTest {

    @Test
    @DisplayName("embedded Unicode format reason을 거부한다")
    void rejectsEmbeddedFormatReason() {
        assertThatThrownBy(() -> AdminAuditEvent.record(
                308L,
                new AdminAuditAction("QUEST_ACCEPTANCE_PROGRESS_ADJUST"),
                new AdminAuditTargetType("QUEST_ACCEPTANCE"),
                "308",
                "CASE-308\u202Eprivate",
                AdminAuditResult.SUCCESS,
                "request-308",
                "key-308",
                Instant.parse("2026-08-25T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("single-line");
    }
}
