package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.RewardDefinition;
import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.RewardProfileStatus;
import online.lifeasgame.reward.domain.RewardType;
import online.lifeasgame.reward.domain.error.RewardError;
import online.lifeasgame.reward.domain.repository.RewardProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardProfileReader")
class RewardProfileReaderTest {

    @Mock
    private RewardProfileRepository repository;

    private RewardProfileReader reader;

    @BeforeEach
    void setUp() {
        reader = new RewardProfileReader(repository);
    }

    @Nested
    @DisplayName("활성 RewardProfile을 code로 조회할 때")
    class GetActiveProfileByCode {

        @Test
        @DisplayName("존재하는 활성 profile이면 line을 포함해 반환한다")
        void returnsActiveProfileWithLines() {
            RewardProfile profile = profile(RewardProfileStatus.ACTIVE);
            profile.addLine(expDefinition(), 0, null);
            given(repository.findByCode("RP_EXP_10")).willReturn(Optional.of(profile));

            RewardProfile result = reader.getActiveByCodeOrThrow("RP_EXP_10");

            assertThat(result).isSameAs(profile);
            assertThat(result.getLines()).hasSize(1);
        }

        @Test
        @DisplayName("비활성 profile이면 REWARD_PROFILE_INACTIVE 예외가 발생한다")
        void throwsWhenProfileIsInactive() {
            given(repository.findByCode("RP_EXP_10"))
                    .willReturn(Optional.of(profile(RewardProfileStatus.INACTIVE)));

            assertRewardError(
                    () -> reader.getActiveByCodeOrThrow("RP_EXP_10"),
                    RewardError.REWARD_PROFILE_INACTIVE
            );
        }
    }

    @Nested
    @DisplayName("존재하지 않는 RewardProfile을 조회할 때")
    class ProfileNotFound {

        @Test
        @DisplayName("REWARD_PROFILE_NOT_FOUND 예외가 발생한다")
        void throwsNotFound() {
            given(repository.findByCode("UNKNOWN")).willReturn(Optional.empty());

            assertRewardError(
                    () -> reader.getByCodeOrThrow("UNKNOWN"),
                    RewardError.REWARD_PROFILE_NOT_FOUND
            );
        }
    }

    private RewardProfile profile(RewardProfileStatus status) {
        return RewardProfile.create("RP_EXP_10", "EXP 10 Profile", status);
    }

    private RewardDefinition expDefinition() {
        return RewardDefinition.create(
                "RD_EXP_10", "EXP 10", RewardType.EXP, 10L, null, null, true
        );
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
