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
    @DisplayName("공식 P0 Reward Profile 두 건의 code와 이름을 보존한다")
    void containsExactlyTwoOfficialProfiles() {
        assertThat(SeedLevel1RewardProfile.values())
                .containsExactly(
                        SeedLevel1RewardProfile.EXP_TINY_10,
                        SeedLevel1RewardProfile.EXP_AND_ITEM_FIRST_STEP_20
                );

        RewardProfileSeedDefinition tiny =
                SeedLevel1RewardProfile.EXP_TINY_10.definition();
        RewardProfileSeedDefinition combined =
                SeedLevel1RewardProfile.EXP_AND_ITEM_FIRST_STEP_20.definition();

        assertThat(tiny.code()).isEqualTo(RewardProfileContentCode.RP_EXP_TINY_10);
        assertThat(tiny.code().value()).isEqualTo("RP_EXP_TINY_10");
        assertThat(tiny.name()).isEqualTo("소량 EXP");
        assertThat(tiny.status()).isEqualTo(RewardProfileStatus.ACTIVE);
        assertThat(combined.code())
                .isEqualTo(RewardProfileContentCode.RP_EXP_AND_ITEM_FIRST_STEP_20);
        assertThat(combined.code().value())
                .isEqualTo("RP_EXP_AND_ITEM_FIRST_STEP_20");
        assertThat(combined.name()).isEqualTo("EXP 20 + First Step Fragment");
        assertThat(combined.status()).isEqualTo(RewardProfileStatus.ACTIVE);
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

        assertThat(RewardDefinitionContentCode.EXP_PLAYER.value())
                .isEqualTo("EXP_PLAYER");
        assertThat(RewardDefinitionContentCode.ITEM_DEFINITION.value())
                .isEqualTo("ITEM_DEFINITION");
        assertThat(RewardProfileContentCode.RP_EXP_TINY_10.value())
                .isEqualTo("RP_EXP_TINY_10");
    }

    @Test
    @DisplayName("TINY Profile은 EXP_PLAYER의 첫 line에 amount 10을 둔다")
    void keepsTinyExpLineContract() {
        var lines = SeedLevel1RewardProfile.EXP_TINY_10.definition().lines();

        assertThat(lines).containsExactly(
                new RewardProfileLineSeedDefinition(
                        RewardDefinitionContentCode.EXP_PLAYER,
                        1,
                        10L
                )
        );
    }

    @Test
    @DisplayName("line은 EXP 20과 Item x1 순서로 lineOrder 1, 2를 사용한다")
    void keepsOfficialLineOrder() {
        var lines = SeedLevel1RewardProfile.EXP_AND_ITEM_FIRST_STEP_20
                .definition()
                .lines();

        assertThat(lines)
                .extracting(RewardProfileLineSeedDefinition::definitionCode)
                .containsExactly(
                        RewardDefinitionContentCode.EXP_PLAYER,
                        RewardDefinitionContentCode.ITEM_DEFINITION
                );
        assertThat(lines)
                .extracting(RewardProfileLineSeedDefinition::sortOrder)
                .containsExactly(1, 2)
                .doesNotHaveDuplicates();
        assertThat(lines)
                .extracting(RewardProfileLineSeedDefinition::amountOverride)
                .containsExactly(20L, 1L);
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
                        RewardDefinitionContentCode.EXP_PLAYER,
                        1,
                        20L
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
                                RewardDefinitionContentCode.EXP_PLAYER,
                                1,
                                20L
                        ),
                        new RewardProfileLineSeedDefinition(
                                RewardDefinitionContentCode.ITEM_DEFINITION,
                                1,
                                1L
                        )
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
