package online.lifeasgame.reward.domain.seed;

import online.lifeasgame.reward.domain.RewardProfileStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SeedLevel1RewardProfile catalog")
class SeedLevel1RewardProfileTest {

    @Test
    @DisplayName("P0 Reward Profile은 정확히 한 건이고 공식 문자열을 보존한다")
    void containsExactlyOneOfficialProfile() {
        assertThat(SeedLevel1RewardProfile.values())
                .containsExactly(SeedLevel1RewardProfile.EXP_AND_ITEM_FIRST_STEP_20);

        RewardProfileSeedDefinition definition =
                SeedLevel1RewardProfile.EXP_AND_ITEM_FIRST_STEP_20.definition();

        assertThat(definition.code())
                .isEqualTo(RewardProfileContentCode.RP_EXP_AND_ITEM_FIRST_STEP_20);
        assertThat(definition.code().value())
                .isEqualTo("RP_EXP_AND_ITEM_FIRST_STEP_20");
        assertThat(definition.name()).isEqualTo("EXP 20 + First Step Fragment");
        assertThat(definition.status()).isEqualTo(RewardProfileStatus.ACTIVE);
    }

    @Test
    @DisplayName("신규 Definition과 전체 Profile content code에는 중복이 없다")
    void hasNoDuplicateContentCodes() {
        assertThat(Arrays.stream(RewardDefinitionContentCode.values())
                .map(RewardDefinitionContentCode::value)
                .toList())
                .doesNotHaveDuplicates();

        assertThat(Arrays.stream(RewardProfileContentCode.values())
                .map(RewardProfileContentCode::value)
                .toList())
                .doesNotHaveDuplicates();

        assertThat(RewardDefinitionContentCode.RD_EXP_20.value())
                .isEqualTo("RD_EXP_20");
        assertThat(RewardDefinitionContentCode.RD_ITEM_FIRST_STEP_FRAGMENT_1.value())
                .isEqualTo("RD_ITEM_FIRST_STEP_FRAGMENT_1");
    }

    @Test
    @DisplayName("line은 EXP 20과 Item x1 순서로 sortOrder 0, 1을 사용한다")
    void keepsOfficialLineOrder() {
        var lines = SeedLevel1RewardProfile.EXP_AND_ITEM_FIRST_STEP_20
                .definition()
                .lines();

        assertThat(lines)
                .extracting(RewardProfileLineSeedDefinition::definitionCode)
                .containsExactly(
                        RewardDefinitionContentCode.RD_EXP_20,
                        RewardDefinitionContentCode.RD_ITEM_FIRST_STEP_FRAGMENT_1
                );
        assertThat(lines)
                .extracting(RewardProfileLineSeedDefinition::sortOrder)
                .containsExactly(0, 1)
                .doesNotHaveDuplicates();
        assertThat(lines)
                .extracting(RewardProfileLineSeedDefinition::amountOverride)
                .containsExactly(null, null);
    }

    @Test
    @DisplayName("catalog가 노출하는 profile과 line 목록은 변경할 수 없다")
    void exposesNoMutableLists() {
        assertThatThrownBy(() -> SeedLevel1RewardProfile.definitions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> SeedLevel1RewardProfile.EXP_AND_ITEM_FIRST_STEP_20
                .definition()
                .lines()
                .clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("profile seed는 입력 line 목록을 방어적으로 복사하고 sortOrder 중복을 거부한다")
    void copiesLinesAndRejectsDuplicateSortOrder() {
        var mutableLines = new ArrayList<>(List.of(
                new RewardProfileLineSeedDefinition(
                        RewardDefinitionContentCode.RD_EXP_20,
                        0,
                        null
                )
        ));
        var definition = new RewardProfileSeedDefinition(
                RewardProfileContentCode.RP_EXP_AND_ITEM_FIRST_STEP_20,
                "EXP 20 + First Step Fragment",
                RewardProfileStatus.ACTIVE,
                mutableLines
        );

        mutableLines.clear();

        assertThat(definition.lines()).hasSize(1);
        assertThatThrownBy(() -> new RewardProfileSeedDefinition(
                RewardProfileContentCode.RP_EXP_AND_ITEM_FIRST_STEP_20,
                "EXP 20 + First Step Fragment",
                RewardProfileStatus.ACTIVE,
                List.of(
                        new RewardProfileLineSeedDefinition(
                                RewardDefinitionContentCode.RD_EXP_20,
                                0,
                                null
                        ),
                        new RewardProfileLineSeedDefinition(
                                RewardDefinitionContentCode.RD_ITEM_FIRST_STEP_FRAGMENT_1,
                                0,
                                null
                        )
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
