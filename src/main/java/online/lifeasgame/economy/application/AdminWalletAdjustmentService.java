package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;
import online.lifeasgame.economy.application.command.AdminWalletAdjustmentCommand;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminWalletAdjustmentService {

    private static final AdminAuditAction AUDIT_ACTION =
            new AdminAuditAction("WALLET_ADJUSTMENT");
    private static final AdminAuditTargetType AUDIT_TARGET =
            new AdminAuditTargetType("WALLET");

    private final TopUpService topUpService;
    private final AdminAuditInternalApi adminAuditApi;

    @Transactional
    public EconomyResult.WalletBalance adjust(
            AdminWalletAdjustmentCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        EconomyResult.WalletBalance balance = topUpService.adjust(
                new EconomyCommand.AdjustWallet(
                        command.playerId(),
                        command.amount(),
                        command.currency(),
                        command.debit(),
                        command.reason()
                )
        );
        adminAuditApi.append(new AdminAuditInternalApi.AppendCommand(
                AUDIT_ACTION,
                AUDIT_TARGET,
                command.playerId().toString(),
                command.reason(),
                AdminAuditResult.SUCCESS,
                command.correlationId(),
                command.idempotencyKey()
        ));
        return balance;
    }
}
