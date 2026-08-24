package online.lifeasgame.adminaudit.application;

import online.lifeasgame.adminaudit.application.query.AdminAuditEventQuery;
import online.lifeasgame.adminaudit.application.result.AdminAuditQueryResult;
import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditEvent;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Audit foundation")
class AdminAuditFoundationTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-24T03:04:05.123456Z");

    @Mock
    private AdminAuditEventQuery query;

    @Nested
    @DisplayName("action과 target type을 만들 때")
    class Codes {

        @Test
        @DisplayName("bounded uppercase code만 허용한다")
        void validatesCodes() {
            assertThat(new AdminAuditAction("USER_STATUS_CHANGE").value())
                    .isEqualTo("USER_STATUS_CHANGE");
            assertThat(new AdminAuditTargetType("USER").value())
                    .isEqualTo("USER");
            assertThatThrownBy(() -> new AdminAuditAction("user.change"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new AdminAuditTargetType("U"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> AdminAuditEvent.record(
                    304L,
                    new AdminAuditAction("USER_STATUS_CHANGE"),
                    new AdminAuditTargetType("USER"),
                    "42",
                    null,
                    AdminAuditResult.SUCCESS,
                    "unsafe correlation",
                    null,
                    OCCURRED_AT
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe format");
        }
    }

    @Nested
    @DisplayName("audit cursor를 처리할 때")
    class Cursor {

        @Test
        @DisplayName("occurredAt과 id를 손실 없이 round-trip한다")
        void roundTrips() {
            String encoded = AdminAuditCursor.encode(OCCURRED_AT, 304L);

            assertThat(AdminAuditCursor.decode(encoded))
                    .isEqualTo(new AdminAuditEventQuery.Cursor(
                            OCCURRED_AT,
                            304L
                    ));
            assertThatThrownBy(() -> AdminAuditCursor.decode("not-a-cursor"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid audit cursor");
        }
    }

    @Nested
    @DisplayName("operator reason을 기록할 때")
    class Reason {

        @Test
        @DisplayName("주변 공백을 제거한 bounded operational rationale를 저장한다")
        void normalizes() {
            AdminAuditEvent event = event(
                    "  Balance correction approved for support case CASE-1234  "
            );

            assertThat(event.getReason()).isEqualTo(
                    "Balance correction approved for support case CASE-1234"
            );
        }

        @Test
        @DisplayName("blank control character와 multiline payload를 거부한다")
        void rejectsUnsafeStructure() {
            assertThatThrownBy(() -> event("   "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> event("case\u0000payload"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> event("case-1\nraw body"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("audit event 목록을 조회할 때")
    class ListEvents {

        @Test
        @DisplayName("size+1 조회로 bounded page와 stable next cursor를 만든다")
        void createsBoundedPage() {
            AdminAuditQueryService service = new AdminAuditQueryService(query);
            when(query.find(any(), eq(3))).thenReturn(List.of(
                    row(3L),
                    row(2L),
                    row(1L)
            ));

            AdminAuditQueryResult.Page page = service.list(
                    304L,
                    "USER_STATUS_CHANGE",
                    "USER",
                    "42",
                    AdminAuditResult.SUCCESS,
                    "request-304",
                    OCCURRED_AT.minusSeconds(1),
                    OCCURRED_AT.plusSeconds(1),
                    null,
                    2
            );

            assertThat(page.items()).extracting(AdminAuditQueryResult.Item::id)
                    .containsExactly(3L, 2L);
            assertThat(AdminAuditCursor.decode(page.nextCursor()))
                    .isEqualTo(new AdminAuditEventQuery.Cursor(
                            OCCURRED_AT,
                            2L
                    ));
            ArgumentCaptor<AdminAuditEventQuery.Filter> filter =
                    ArgumentCaptor.forClass(AdminAuditEventQuery.Filter.class);
            verify(query).find(filter.capture(), eq(3));
            assertThat(filter.getValue().action().value())
                    .isEqualTo("USER_STATUS_CHANGE");
            assertThat(filter.getValue().targetType().value())
                    .isEqualTo("USER");
        }

        @Test
        @DisplayName("unbounded size와 역전된 time range를 거부한다")
        void rejectsInvalidBounds() {
            AdminAuditQueryService service = new AdminAuditQueryService(query);

            assertThatThrownBy(() -> service.list(
                    null, null, null, null, null, null,
                    null, null, null, 101
            )).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.list(
                    null, null, null, null, null, null,
                    OCCURRED_AT, OCCURRED_AT, null, 20
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private AdminAuditEventQuery.Row row(Long id) {
        return new AdminAuditEventQuery.Row(
                id,
                304L,
                "USER_STATUS_CHANGE",
                "USER",
                "42",
                "CASE-304",
                AdminAuditResult.SUCCESS,
                "request-304",
                null,
                OCCURRED_AT
        );
    }

    private AdminAuditEvent event(String reason) {
        return AdminAuditEvent.record(
                304L,
                new AdminAuditAction("USER_STATUS_CHANGE"),
                new AdminAuditTargetType("USER"),
                "42",
                reason,
                AdminAuditResult.SUCCESS,
                "request-304",
                null,
                OCCURRED_AT
        );
    }
}
