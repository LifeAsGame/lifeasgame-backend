package online.lifeasgame.quest.application;

import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.quest.application.command.AdminQuestAcceptanceOverrideCommand;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Quest Acceptance override application contract")
class AdminQuestAcceptanceOverrideServiceTest {

    private static final long ACCEPTANCE_ID = 308L;

    @Mock
    private QuestService questService;

    @Mock
    private AdminAuditInternalApi adminAuditApi;

    @Nested
    @DisplayName("command metadata를 만들 때")
    class CommandValidation {

        @Test
        @DisplayName("negative delta와 unsafe metadata를 거부한다")
        void rejectsInvalidMetadata() {
            assertThatThrownBy(() -> progress(-1, "CASE-308", "key-308", null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> progress(1, "CASE\nprivate", "key-308", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("single-line");
            assertThatThrownBy(() -> progress(1, "CASE-308", "unsafe key", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe format");
            assertThatThrownBy(() -> status("CANCELED", "CASE-308", "key-308", "unsafe id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe format");
        }

        @ParameterizedTest
        @ValueSource(strings = {"\u200B", "\u00A0", " \u200B \u00A0 "})
        @DisplayName("invisible-only reason을 거부한다")
        void rejectsInvisibleReason(String reason) {
            assertThatThrownBy(() -> progress(1, reason, "key-308", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embedded Unicode format reason을 두 command 모두 거부한다")
        void rejectsEmbeddedFormatReason() {
            assertThatThrownBy(() -> progress(
                    1,
                    "CASE-308\u202Eprivate",
                    "progress-key-308",
                    null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("single-line");
            assertThatThrownBy(() -> status(
                    "CANCELED",
                    "CASE-308\u202Eprivate",
                    "status-key-308",
                    null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("single-line");
        }

        @Test
        @DisplayName("correlation header가 없으면 safe UUID를 생성한다")
        void generatesCorrelation() {
            assertThat(progress(0, " CASE-308 ", "key-308", null))
                    .satisfies(command -> {
                        assertThat(command.reason()).isEqualTo("CASE-308");
                        assertThat(command.correlationId())
                                .matches("[a-f0-9-]{36}");
                    });
        }
    }

    @Test
    @DisplayName("progress mutation 뒤 canonical success Audit을 append한다")
    void adjustsProgressThenAudits() {
        AdminQuestAcceptanceOverrideService service = service();
        QuestResult.Acceptance expected = mock(QuestResult.Acceptance.class);
        when(questService.adjustAcceptanceProgress(any(), any()))
                .thenReturn(expected);

        QuestResult.Acceptance result = service.adjustProgress(progress(
                2,
                "CASE-308-PROGRESS",
                "progress-308",
                "request-progress"
        ));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<QuestCommand.AdjustProgress> questCommand =
                ArgumentCaptor.forClass(QuestCommand.AdjustProgress.class);
        ArgumentCaptor<AdminAuditInternalApi.AppendCommand> auditCommand =
                ArgumentCaptor.forClass(AdminAuditInternalApi.AppendCommand.class);
        InOrder order = inOrder(questService, adminAuditApi);
        order.verify(questService).adjustAcceptanceProgress(
                org.mockito.ArgumentMatchers.eq(ACCEPTANCE_ID),
                questCommand.capture()
        );
        order.verify(adminAuditApi).append(auditCommand.capture());
        assertThat(questCommand.getValue().delta()).isEqualTo(2);
        assertAudit(
                auditCommand.getValue(),
                "QUEST_ACCEPTANCE_PROGRESS_ADJUST",
                "CASE-308-PROGRESS",
                "progress-308",
                "request-progress"
        );
    }

    @Test
    @DisplayName("status mutation 뒤 canonical success Audit을 append한다")
    void changesStatusThenAudits() {
        AdminQuestAcceptanceOverrideService service = service();
        QuestResult.Acceptance expected = mock(QuestResult.Acceptance.class);
        when(questService.changeAcceptanceStatus(any(), any()))
                .thenReturn(expected);

        QuestResult.Acceptance result = service.changeStatus(status(
                "CANCELED",
                "CASE-308-STATUS",
                "status-308",
                "request-status"
        ));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<QuestCommand.ChangeStatus> questCommand =
                ArgumentCaptor.forClass(QuestCommand.ChangeStatus.class);
        ArgumentCaptor<AdminAuditInternalApi.AppendCommand> auditCommand =
                ArgumentCaptor.forClass(AdminAuditInternalApi.AppendCommand.class);
        InOrder order = inOrder(questService, adminAuditApi);
        order.verify(questService).changeAcceptanceStatus(
                org.mockito.ArgumentMatchers.eq(ACCEPTANCE_ID),
                questCommand.capture()
        );
        order.verify(adminAuditApi).append(auditCommand.capture());
        assertThat(questCommand.getValue().status()).isEqualTo("CANCELED");
        assertAudit(
                auditCommand.getValue(),
                "QUEST_ACCEPTANCE_STATUS_CHANGE",
                "CASE-308-STATUS",
                "status-308",
                "request-status"
        );
    }

    private AdminQuestAcceptanceOverrideService service() {
        return new AdminQuestAcceptanceOverrideService(
                questService,
                adminAuditApi
        );
    }

    private AdminQuestAcceptanceOverrideCommand.AdjustProgress progress(
            int delta,
            String reason,
            String key,
            String correlationId
    ) {
        return new AdminQuestAcceptanceOverrideCommand.AdjustProgress(
                ACCEPTANCE_ID,
                delta,
                reason,
                key,
                correlationId
        );
    }

    private AdminQuestAcceptanceOverrideCommand.ChangeStatus status(
            String value,
            String reason,
            String key,
            String correlationId
    ) {
        return new AdminQuestAcceptanceOverrideCommand.ChangeStatus(
                ACCEPTANCE_ID,
                value,
                reason,
                key,
                correlationId
        );
    }

    private void assertAudit(
            AdminAuditInternalApi.AppendCommand audit,
            String action,
            String reason,
            String key,
            String correlationId
    ) {
        assertThat(audit.action().value()).isEqualTo(action);
        assertThat(audit.targetType().value()).isEqualTo("QUEST_ACCEPTANCE");
        assertThat(audit.targetId()).isEqualTo(Long.toString(ACCEPTANCE_ID));
        assertThat(audit.reason()).isEqualTo(reason);
        assertThat(audit.result()).isEqualTo(AdminAuditResult.SUCCESS);
        assertThat(audit.idempotencyKey()).isEqualTo(key);
        assertThat(audit.correlationId()).isEqualTo(correlationId);
    }
}
