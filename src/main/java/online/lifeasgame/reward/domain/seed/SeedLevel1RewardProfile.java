package online.lifeasgame.reward.domain.seed;

import online.lifeasgame.reward.domain.RewardProfileStatus;

import java.util.Arrays;
import java.util.List;

public enum SeedLevel1RewardProfile {

    ADVENTURE_PREPARATION(new RewardProfileSeedDefinition(
            RewardProfileContentCode.RP_ADVENTURE_PREPARATION,
            "모험의 준비 보상", RewardProfileStatus.ACTIVE,
            List.of(new RewardProfileLineSeedDefinition(RewardDefinitionContentCode.RD_ADVENTURE_GOLD, 1, 100L),
                    new RewardProfileLineSeedDefinition(RewardDefinitionContentCode.RD_RECORD_CRYSTAL, 2, 1L)))),

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
