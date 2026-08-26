package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;
import online.lifeasgame.character.application.command.AdminPlayerHolderGrantCommand;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminPlayerHolderGrantService {

    private static final AdminAuditAction ACHIEVEMENT_ACTION =
            new AdminAuditAction("PLAYER_ACHIEVEMENT_GRANT");
    private static final AdminAuditAction TITLE_ACTION =
            new AdminAuditAction("PLAYER_TITLE_GRANT");
    private static final AdminAuditTargetType ACHIEVEMENT_TARGET =
            new AdminAuditTargetType("PLAYER_ACHIEVEMENT");
    private static final AdminAuditTargetType TITLE_TARGET =
            new AdminAuditTargetType("PLAYER_TITLE");

    private final PlayerAchievementService playerAchievementService;
    private final PlayerTitleService playerTitleService;
    private final AdminAuditInternalApi adminAuditApi;

    @Transactional
    public PlayerAchievementResult.Granted grantAchievement(
            AdminPlayerHolderGrantCommand.GrantAchievement command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        PlayerAchievementResult.Granted result =
                playerAchievementService.grantAchievement(
                        command.playerId(),
                        command.achievementId()
                );
        appendAudit(
                ACHIEVEMENT_ACTION,
                ACHIEVEMENT_TARGET,
                command.playerId(),
                command.achievementId(),
                command.reason(),
                command.correlationId(),
                command.idempotencyKey()
        );
        return result;
    }

    @Transactional
    public PlayerTitleResult.Created grantTitle(
            AdminPlayerHolderGrantCommand.GrantTitle command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        PlayerTitleResult.Created result = playerTitleService.createTitle(
                command.playerId(),
                command.titleId()
        );
        appendAudit(
                TITLE_ACTION,
                TITLE_TARGET,
                command.playerId(),
                command.titleId(),
                command.reason(),
                command.correlationId(),
                command.idempotencyKey()
        );
        return result;
    }

    private void appendAudit(
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            Long playerId,
            Long definitionId,
            String reason,
            String correlationId,
            String idempotencyKey
    ) {
        adminAuditApi.append(new AdminAuditInternalApi.AppendCommand(
                action,
                targetType,
                playerId + ":" + definitionId,
                reason,
                AdminAuditResult.SUCCESS,
                correlationId,
                idempotencyKey
        ));
    }
}
