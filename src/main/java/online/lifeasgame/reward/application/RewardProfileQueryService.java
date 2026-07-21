package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.application.query.RewardProfileQueryRepository;
import online.lifeasgame.reward.application.result.RewardProfileResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardProfileQueryService {

    private final RewardProfileReader rewardProfileReader;
    private final RewardProfileQueryRepository queryRepository;

    public RewardProfileResult.Detail getProfileView(String code) {
        return RewardProfileResult.Detail.from(rewardProfileReader.getByCodeOrThrow(code));
    }

    public List<RewardProfileResult.Summary> listActiveProfiles() {
        return queryRepository.findActiveSummaries().stream()
                .map(RewardProfileResult.Summary::from)
                .toList();
    }
}
