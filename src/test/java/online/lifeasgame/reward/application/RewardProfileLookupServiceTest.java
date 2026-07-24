package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.internal.RewardProfileLookupApi;
import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.RewardProfileStatus;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardProfile provider-owned lookup API")
class RewardProfileLookupServiceTest {

    @Mock
    private RewardProfileReader rewardProfileReader;

    private RewardProfileLookupService service;

    @BeforeEach
    void setUp() {
        service = new RewardProfileLookupService(rewardProfileReader);
    }

    @Nested
    @DisplayName("활성 Profile을 code로 조회할 때")
    class LookupActiveProfile {

        @Test
        @DisplayName("Entity 대신 code-only Reference DTO를 반환한다")
        void returnsCodeOnlyReference() {
            RewardProfile profile = RewardProfile.create(
                    "RP_EXP_10",
                    "EXP 10 Profile",
                    RewardProfileStatus.ACTIVE
            );
            given(rewardProfileReader.getActiveByCodeOrThrow("RP_EXP_10"))
                    .willReturn(profile);

            RewardProfileLookupApi.RewardProfileReference result =
                    service.getActiveByCode("RP_EXP_10");

            assertThat(result.code()).isEqualTo("RP_EXP_10");
            assertThat(result).isNotInstanceOf(RewardProfile.class);
            assertThat(result.getClass().getRecordComponents())
                    .extracting(component -> component.getName())
                    .containsExactly("code");
        }
    }

    @Test
    @DisplayName("missing Profile의 RewardError를 그대로 전달한다")
    void keepsMissingProfileError() {
        given(rewardProfileReader.getActiveByCodeOrThrow("UNKNOWN"))
                .willThrow(new DomainException(RewardError.REWARD_PROFILE_NOT_FOUND));

        assertRewardError(
                () -> service.getActiveByCode("UNKNOWN"),
                RewardError.REWARD_PROFILE_NOT_FOUND
        );
    }

    @Test
    @DisplayName("inactive Profile의 RewardError를 그대로 전달한다")
    void keepsInactiveProfileError() {
        given(rewardProfileReader.getActiveByCodeOrThrow("RP_INACTIVE"))
                .willThrow(new DomainException(RewardError.REWARD_PROFILE_INACTIVE));

        assertRewardError(
                () -> service.getActiveByCode("RP_INACTIVE"),
                RewardError.REWARD_PROFILE_INACTIVE
        );
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
