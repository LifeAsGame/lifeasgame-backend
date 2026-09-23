package online.lifeasgame.reward.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AccountRewardEntitlementStore {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(Long accountId, String code, Long settlementId) {
        jdbc.update("""
                INSERT INTO account_reward_entitlements (account_id, entitlement_code, settlement_id)
                VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE account_id = account_id
                """, accountId, code, settlementId);
        Long owner = jdbc.queryForObject("""
                SELECT settlement_id FROM account_reward_entitlements
                WHERE account_id = ? AND entitlement_code = ? FOR UPDATE
                """, Long.class, accountId, code);
        return settlementId.equals(owner);
    }
}
