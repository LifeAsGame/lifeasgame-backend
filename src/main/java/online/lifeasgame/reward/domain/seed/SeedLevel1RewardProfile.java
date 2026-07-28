package online.lifeasgame.reward.domain.seed;

import online.lifeasgame.reward.domain.RewardProfileStatus;

import java.util.Arrays;
import java.util.List;

public enum SeedLevel1RewardProfile {

    EXP_AND_ITEM_FIRST_STEP_20(
            new RewardProfileSeedDefinition(
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
                                    1,
                                    null
                            )
                    )
            )
    );

    private final RewardProfileSeedDefinition definition;

    SeedLevel1RewardProfile(RewardProfileSeedDefinition definition) {
        this.definition = definition;
    }

    public RewardProfileSeedDefinition definition() {
        return definition;
    }

    public static List<RewardProfileSeedDefinition> definitions() {
        return Arrays.stream(values())
                .map(SeedLevel1RewardProfile::definition)
                .toList();
    }
}
