package online.lifeasgame.economy.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.application.query.WalletBalanceQuery;
import online.lifeasgame.economy.domain.WalletHold;
import org.springframework.stereotype.Repository;

import java.util.List;

import static online.lifeasgame.economy.domain.QWalletBalance.walletBalance;
import static online.lifeasgame.economy.domain.QWalletHold.walletHold;

@Repository
@RequiredArgsConstructor
public class WalletBalanceQueryAdapter implements WalletBalanceQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Amount> findAvailable(Long playerId) {
        return queryFactory.select(Projections.constructor(Amount.class,
                        walletBalance.currency, walletBalance.amount))
                .from(walletBalance)
                .where(walletBalance.wallet.ownerId.eq(playerId))
                .fetch();
    }

    @Override
    public List<Amount> findOpenHoldTotals(Long playerId) {
        // Elapsed TTL does not settle a hold; only committed command state does.
        return queryFactory.select(Projections.constructor(Amount.class,
                        walletHold.currency, walletHold.amount.sum()))
                .from(walletHold)
                .where(walletHold.wallet.ownerId.eq(playerId), walletHold.status.eq(WalletHold.Status.OPEN))
                .groupBy(walletHold.currency)
                .fetch();
    }
}
