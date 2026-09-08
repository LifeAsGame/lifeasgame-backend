package online.lifeasgame.reward.domain.seed;

import online.lifeasgame.reward.domain.RewardProfileStatus;

import java.util.Arrays;
import java.util.List;

public enum SeedLevel1RewardProfile {

    EXP_TINY_10(
            new RewardProfileSeedDefinition(
                    RewardProfileContentCode.RP_EXP_TINY_10,
                    "소량 EXP",
                    RewardProfileStatus.ACTIVE,
                    List.of(
                            new RewardProfileLineSeedDefinition(
                                    RewardDefinitionContentCode.EXP_PLAYER,
                                    1,
                                    10L
                            )
                    )
            )
    ),

    EXP_AND_ITEM_FIRST_STEP_20(
            new RewardProfileSeedDefinition(
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
                                    2,
                                    1L
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
