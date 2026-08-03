package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.RewardDefinition;
import online.lifeasgame.reward.domain.RewardType;
import online.lifeasgame.reward.domain.error.RewardError;
import online.lifeasgame.reward.domain.repository.RewardDefinitionRepository;
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
@DisplayName("RewardDefinitionReader")
class RewardDefinitionReaderTest {

    @Mock
    private RewardDefinitionRepository repository;

    private RewardDefinitionReader reader;

    @BeforeEach
    void setUp() {
        reader = new RewardDefinitionReader(repository);
    }

    @Nested
    @DisplayName("RewardDefinition을 code로 조회할 때")
    class GetByCode {

        @Test
        @DisplayName("존재하는 정의를 반환한다")
        void returnsDefinition() {
            RewardDefinition definition = RewardDefinition.create(
                    "RD_EXP_10", "EXP 10", RewardType.EXP, 10L, null, null, true
            );
            given(repository.findByCode("RD_EXP_10")).willReturn(Optional.of(definition));

            assertThat(reader.getByCodeOrThrow("RD_EXP_10")).isSameAs(definition);
        }

        @Test
        @DisplayName("존재하지 않으면 REWARD_DEFINITION_NOT_FOUND 예외가 발생한다")
        void throwsWhenDefinitionDoesNotExist() {
            given(repository.findByCode("UNKNOWN")).willReturn(Optional.empty());

            assertThatThrownBy(() -> reader.getByCodeOrThrow("UNKNOWN"))
                    .isInstanceOfSatisfying(DomainException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(RewardError.REWARD_DEFINITION_NOT_FOUND)
                    );
        }
    }
}
