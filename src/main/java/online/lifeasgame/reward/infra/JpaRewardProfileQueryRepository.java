package online.lifeasgame.reward.infra;

import online.lifeasgame.reward.application.query.RewardProfileSummaryView;
import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.RewardProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaRewardProfileQueryRepository extends JpaRepository<RewardProfile, Long> {

    @Query("""
            SELECT new online.lifeasgame.reward.application.query.RewardProfileSummaryView(
                profile.id,
                profile.code,
                profile.name,
                profile.status
            )
            FROM RewardProfile profile
            WHERE profile.status = :status
            ORDER BY profile.code
            """)
    List<RewardProfileSummaryView> findSummariesByStatus(@Param("status") RewardProfileStatus status);
}
