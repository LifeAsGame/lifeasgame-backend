package online.lifeasgame.character.infra;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerTitleQuery;
import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.TitleCategory;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlayerTitleRepositoryAdapter implements PlayerTitleRepository, PlayerTitleQuery {

    private final JpaPlayerTitleRepository jpaRepository;
    private final AwardReceiptAcquiredAtQuery acquiredAtQuery;

    public PlayerTitle save(PlayerTitle playerTitle) {
        return jpaRepository.saveAndFlush(playerTitle);
    }

    @Override
    public boolean existsByPlayerIdAndTitleId(Long playerId, Long titleId) {
        return jpaRepository.existsByPlayerIdAndTitleId(playerId, titleId);
    }

    @Override
    public long deleteByPlayerIdAndTitleId(Long playerId, Long titleId) {
        return jpaRepository.deleteByPlayerIdAndTitleId(playerId, titleId);
    }

    @Override
    public List<PlayerTitleView> findViewsByPlayerId(Long playerId) {
        var grantTimes = acquiredAtQuery.titles(playerId);
        return jpaRepository.findPlayerTitleViews(playerId).stream()
                .map(view -> withGrantTime(view, grantTimes.get(view.getTitleId())))
                .toList();
    }

    private static PlayerTitleView withGrantTime(PlayerTitleView view, Instant grantTime) {
        if (grantTime == null) return view;
        return new GrantTimeView(view, grantTime);
    }

    private record GrantTimeView(PlayerTitleView delegate, Instant acquiredAt)
            implements PlayerTitleView {
        public Long getTitleId() { return delegate.getTitleId(); }
        public String getCode() { return delegate.getCode(); }
        public String getName() { return delegate.getName(); }
        public TitleCategory getCategory() { return delegate.getCategory(); }
        public String getDescMd() { return delegate.getDescMd(); }
        public Instant getAcquiredAt() { return acquiredAt; }
    }
}
