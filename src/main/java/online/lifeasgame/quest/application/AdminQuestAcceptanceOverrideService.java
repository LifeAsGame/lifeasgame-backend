package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;
import online.lifeasgame.quest.application.command.AdminQuestAcceptanceOverrideCommand;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminQuestAcceptanceOverrideService {

    private static final AdminAuditAction PROGRESS_ACTION =
            new AdminAuditAction("QUEST_ACCEPTANCE_PROGRESS_ADJUST");
    private static final AdminAuditAction STATUS_ACTION =
            new AdminAuditAction("QUEST_ACCEPTANCE_STATUS_CHANGE");
    private static final AdminAuditTargetType AUDIT_TARGET =
            new AdminAuditTargetType("QUEST_ACCEPTANCE");

    private final QuestService questService;
    private final AdminAuditInternalApi adminAuditApi;

    @Transactional
    public QuestResult.Acceptance adjustProgress(
            AdminQuestAcceptanceOverrideCommand.AdjustProgress command
    ) {
        QuestResult.Acceptance result = questService.adjustAcceptanceProgress(
                command.acceptanceId(),
                new QuestCommand.AdjustProgress(command.delta())
        );
        appendAudit(
                PROGRESS_ACTION,
                command.acceptanceId(),
                command.reason(),
                command.correlationId(),
                command.idempotencyKey()
        );
        return result;
    }

    @Transactional
    public QuestResult.Acceptance changeStatus(
            AdminQuestAcceptanceOverrideCommand.ChangeStatus command
    ) {
        QuestResult.Acceptance result = questService.changeAcceptanceStatus(
                command.acceptanceId(),
                new QuestCommand.ChangeStatus(command.status())
        );
        appendAudit(
                STATUS_ACTION,
                command.acceptanceId(),
                command.reason(),
                command.correlationId(),
                command.idempotencyKey()
        );
        return result;
    }

    private void appendAudit(
            AdminAuditAction action,
            Long acceptanceId,
            String reason,
            String correlationId,
            String idempotencyKey
    ) {
        adminAuditApi.append(new AdminAuditInternalApi.AppendCommand(
                action,
                AUDIT_TARGET,
                acceptanceId.toString(),
                reason,
                AdminAuditResult.SUCCESS,
                correlationId,
                idempotencyKey
        ));
    }
}
