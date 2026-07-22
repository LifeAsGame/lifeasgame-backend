package online.lifeasgame.reward.application;

import online.lifeasgame.reward.application.result.RewardProfileResult;
import online.lifeasgame.reward.application.query.RewardProfileQueryRepository;
import online.lifeasgame.reward.application.query.RewardProfileSummaryView;
import online.lifeasgame.reward.domain.RewardDefinition;
import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.RewardProfileStatus;
import online.lifeasgame.reward.domain.RewardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardProfileQueryService")
class RewardProfileQueryServiceTest {

    @Mock
    private RewardProfileReader reader;

    @Mock
    private RewardProfileQueryRepository queryRepository;

    private RewardProfileQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new RewardProfileQueryService(reader, queryRepository);
    }

    @Nested
    @DisplayName("profile 상세를 조회할 때")
    class GetProfileView {

        @Test
        @DisplayName("정렬된 line과 보상 정의를 함께 반환한다")
        void returnsProfileWithOrderedLines() {
            RewardProfile profile = profile("RP_EXP", "EXP Profile");
            profile.addLine(expDefinition("RD_EXP_30", 30L), 20, null);
            profile.addLine(expDefinition("RD_EXP_10", 10L), 10, null);
            given(reader.getByCodeOrThrow("RP_EXP")).willReturn(profile);

            RewardProfileResult.Detail result = queryService.getProfileView("RP_EXP");

            assertThat(result.lines()).extracting(RewardProfileResult.Line::sortOrder)
                    .containsExactly(10, 20);
            assertThat(result.lines()).extracting(line -> line.rewardDefinition().code())
                    .containsExactly("RD_EXP_10", "RD_EXP_30");
        }
    }

    @Nested
    @DisplayName("활성 profile 목록을 조회할 때")
    class ListActiveProfiles {

        @Test
        @DisplayName("활성 profile 요약 목록을 반환한다")
        void returnsActiveProfileSummaries() {
            given(queryRepository.findActiveSummaries()).willReturn(List.of(
                    new RewardProfileSummaryView(
                            1L, "RP_EXP_10", "EXP 10 Profile", RewardProfileStatus.ACTIVE
                    ),
                    new RewardProfileSummaryView(
                            2L, "RP_EXP_30", "EXP 30 Profile", RewardProfileStatus.ACTIVE
                    )
            ));

            assertThat(queryService.listActiveProfiles())
                    .extracting(RewardProfileResult.Summary::code)
                    .containsExactly("RP_EXP_10", "RP_EXP_30");
        }
    }

    private RewardProfile profile(String code, String name) {
        return RewardProfile.create(code, name, RewardProfileStatus.ACTIVE);
    }

    private RewardDefinition expDefinition(String code, Long amount) {
        return RewardDefinition.create(code, code, RewardType.EXP, amount, null, true);
    }
}
