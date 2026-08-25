package online.lifeasgame.economy.application;

import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.economy.application.command.AdminWalletAdjustmentCommand;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Wallet adjustment application contract")
class AdminWalletAdjustmentServiceTest {

    @Mock
    private TopUpService topUpService;

    @Mock
    private AdminAuditInternalApi adminAuditApi;

    @Nested
    @DisplayName("command metadata를 만들 때")
    class CommandValidation {

        @Test
        @DisplayName("reason과 idempotency key를 필수 bounded 값으로 검증한다")
        void validatesRequiredMetadata() {
            assertThatThrownBy(() -> command(" ", "key-306", null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> command(
                    "support case\nprivate payload",
                    "key-306",
                    null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("single-line");
            assertThatThrownBy(() -> command(
                    "CASE-306",
                    "unsafe key",
                    null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe format");
            assertThatThrownBy(() -> command(
                    "CASE-306",
                    "key-306",
                    "unsafe correlation"
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe format");
        }

        @Test
        @DisplayName("correlation header가 없으면 safe server UUID를 생성한다")
        void generatesCorrelation() {
            AdminWalletAdjustmentCommand command = command(
                    " CASE-306 ",
                    "key-306",
                    null
            );

            assertThat(command.reason()).isEqualTo("CASE-306");
            assertThat(command.correlationId())
                    .matches("[a-f0-9-]{36}");
        }
    }

    @Test
    @DisplayName("Wallet adjustment 뒤 동일 transaction의 canonical Audit을 append한다")
    void adjustsThenAudits() {
        AdminWalletAdjustmentService service =
                new AdminWalletAdjustmentService(topUpService, adminAuditApi);
        AdminWalletAdjustmentCommand command = command(
                "CASE-306",
                "wallet-adjust-306",
                "request-306"
        );
        when(topUpService.adjust(any())).thenReturn(
                new EconomyResult.WalletBalance(130L, "GOLD")
        );

        EconomyResult.WalletBalance result = service.adjust(command);

        assertThat(result.amount()).isEqualTo(130L);
        ArgumentCaptor<EconomyCommand.AdjustWallet> walletCommand =
                ArgumentCaptor.forClass(EconomyCommand.AdjustWallet.class);
        ArgumentCaptor<AdminAuditInternalApi.AppendCommand> auditCommand =
                ArgumentCaptor.forClass(
                        AdminAuditInternalApi.AppendCommand.class
                );
        InOrder order = inOrder(topUpService, adminAuditApi);
        order.verify(topUpService).adjust(walletCommand.capture());
        order.verify(adminAuditApi).append(auditCommand.capture());
        assertThat(walletCommand.getValue().playerId()).isEqualTo(306L);
        assertThat(walletCommand.getValue().reason()).isEqualTo("CASE-306");
        assertThat(auditCommand.getValue()).satisfies(audit -> {
            assertThat(audit.action().value()).isEqualTo("WALLET_ADJUSTMENT");
            assertThat(audit.targetType().value()).isEqualTo("WALLET");
            assertThat(audit.targetId()).isEqualTo("306");
            assertThat(audit.reason()).isEqualTo("CASE-306");
            assertThat(audit.result()).isEqualTo(AdminAuditResult.SUCCESS);
            assertThat(audit.correlationId()).isEqualTo("request-306");
            assertThat(audit.idempotencyKey()).isEqualTo("wallet-adjust-306");
        });
        verify(adminAuditApi).append(any());
    }

    private AdminWalletAdjustmentCommand command(
            String reason,
            String idempotencyKey,
            String correlationId
    ) {
        return new AdminWalletAdjustmentCommand(
                306L,
                30L,
                "GOLD",
                false,
                reason,
                idempotencyKey,
                correlationId
        );
    }
}
