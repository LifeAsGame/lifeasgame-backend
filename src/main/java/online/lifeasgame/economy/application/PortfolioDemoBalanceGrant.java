package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.internal.PortfolioDemoBalanceApi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PortfolioDemoBalanceGrant implements PortfolioDemoBalanceApi {
    private final JdbcTemplate jdbc;
    private final TopUpService topUpService;

    @Override
    @Transactional
    public void grantStartingGold(String runId, Long playerId) {
        int written = jdbc.update("INSERT IGNORE INTO portfolio_demo_balance_grants (run_id,player_id) VALUES (?,?)",
                runId, playerId);
        if (written == 0) return;
        topUpService.adjust(new EconomyCommand.AdjustWallet(
                playerId, 100, "GOLD", false, "PORTFOLIO_DEMO_START"));
    }
}
