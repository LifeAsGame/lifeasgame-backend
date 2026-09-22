package online.lifeasgame.economy.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.application.internal.RewardGoldCreditApi.Receipt;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RewardGoldCreditStore {
    private final JdbcTemplate jdbc;

    public void ensureWallet(Long playerId) {
        jdbc.update("""
                INSERT INTO wallets (owner_id, version, created_at, updated_at)
                VALUES (?, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE owner_id = owner_id
                """, playerId);
    }

    public Optional<Receipt> find(Long lineId) {
        return jdbc.query("""
                SELECT reward_line_id, player_id, account_id, amount
                FROM reward_gold_credits WHERE reward_line_id = ?
                """, (rs, row) -> new Receipt(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)),
                lineId).stream().findFirst();
    }

    public void save(Receipt receipt) {
        jdbc.update("""
                INSERT INTO reward_gold_credits (reward_line_id, player_id, account_id, amount)
                VALUES (?, ?, ?, ?)
                """, receipt.rewardLineId(), receipt.playerId(), receipt.accountId(), receipt.amount());
    }
}
