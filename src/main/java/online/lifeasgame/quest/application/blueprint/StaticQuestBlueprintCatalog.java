package online.lifeasgame.quest.application.blueprint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Getter
@Component
@RequiredArgsConstructor
public class StaticQuestBlueprintCatalog implements QuestBlueprintCatalog {

    private final Map<QuestCode, QuestBlueprint> blueprints = Map.copyOf(initialize());

    private static Map<QuestCode, QuestBlueprint> initialize() {
        EnumMap<QuestCode, QuestBlueprint> map = new EnumMap<>(QuestCode.class);

        map.put(
                QuestCode.PLAYER_WELCOME,
                new QuestBlueprint(
                        QuestCode.PLAYER_WELCOME,
                        QuestCategory.MAIN,
                        QuestTitle.of("튜토리얼: 첫 걸음"),
                        "플레이어 등록을 마치고 새로운 삶을 시작하세요.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(100, RewardStats.empty()),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_TRACK,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_TRACK,
                        QuestCategory.REPEAT,
                        QuestTitle.of("레벨업 달성기"),
                        "모험을 통해 꾸준히 성장하세요. 주간마다 레벨업 진척도를 계산합니다.",
                        QuestTarget.of(QuestTargetType.COUNT, 7),
                        QuestReward.of(250, new RewardStats(Map.of("strength", 1, "wisdom", 1))),
                        QuestRepeatRule.WEEKLY,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_10,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_10,
                        QuestCategory.RECOMMENDED,
                        QuestTitle.of("성장의 첫 고비"),
                        "플레이어 레벨 10에 도달하면 특별 보상을 지급합니다.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(400, new RewardStats(Map.of("agility", 2))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_20,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_20,
                        QuestCategory.RECOMMENDED,
                        QuestTitle.of("꾸준한 성장"),
                        "레벨 20에 도달하여 중급 모험가의 자격을 증명하세요.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(600, new RewardStats(Map.of("agility", 2, "vitality", 1))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_30,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_30,
                        QuestCategory.RECOMMENDED,
                        QuestTitle.of("능력치 재정비"),
                        "레벨 30 달성 시 새로운 빌드를 위한 능력치를 지원합니다.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(800, new RewardStats(Map.of("strength", 2, "wisdom", 1))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_40,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_40,
                        QuestCategory.MAIN,
                        QuestTitle.of("상급자의 벽"),
                        "레벨 40을 달성해 상급자의 반열에 오르세요.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(1100, new RewardStats(Map.of("vitality", 2, "agility", 1))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_50,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_50,
                        QuestCategory.MAIN,
                        QuestTitle.of("중견 모험가"),
                        "레벨 50을 달성한 모험가에게 명예로운 칭호를 부여합니다.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(1500, new RewardStats(Map.of("vitality", 3, "charisma", 2))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_60,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_60,
                        QuestCategory.MAIN,
                        QuestTitle.of("베테랑의 증표"),
                        "레벨 60에 오른 베테랑에게 추가 스탯을 부여합니다.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(1700, new RewardStats(Map.of("strength", 2, "dexterity", 2))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_70,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_70,
                        QuestCategory.RECOMMENDED,
                        QuestTitle.of("지혜로운 지도자"),
                        "레벨 70에 도달하면 파티 운영에 필요한 통찰력을 보상합니다.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(1900, new RewardStats(Map.of("wisdom", 2, "charisma", 1))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_80,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_80,
                        QuestCategory.MAIN,
                        QuestTitle.of("최정예 검증"),
                        "레벨 80의 최정예 모험가에게 종합 능력 보너스를 제공합니다.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(2200, new RewardStats(Map.of("vitality", 3, "charisma", 2))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_90,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_90,
                        QuestCategory.MAIN,
                        QuestTitle.of("전설로의 준비"),
                        "레벨 90에 도달한 모험가에게 전설을 향한 준비 보너스를 지급합니다.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(2600, new RewardStats(Map.of("strength", 3, "vitality", 3))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.PLAYER_LEVEL_MILESTONE_100,
                new QuestBlueprint(
                        QuestCode.PLAYER_LEVEL_MILESTONE_100,
                        QuestCategory.MAIN,
                        QuestTitle.of("전설의 탄생"),
                        "최종 레벨 100에 도달하여 전설로 거듭나세요.",
                        QuestTarget.of(QuestTargetType.COUNT, 1),
                        QuestReward.of(3200, new RewardStats(Map.of("all", 5))),
                        QuestRepeatRule.NONE,
                        null
                )
        );

        map.put(
                QuestCode.EXERCISE_MINUTES_300,
                new QuestBlueprint(
                        QuestCode.EXERCISE_MINUTES_300,
                        QuestCategory.REPEAT,
                        QuestTitle.of("주간 체력 훈련"),
                        "한 주 동안 300분 이상의 운동을 기록하세요.",
                        QuestTarget.of(QuestTargetType.MINUTES, 300),
                        QuestReward.of(200, new RewardStats(Map.of("stamina", 2))),
                        QuestRepeatRule.WEEKLY,
                        null
                )
        );

        map.put(
                QuestCode.COLLECTION_HUNTER_10,
                new QuestBlueprint(
                        QuestCode.COLLECTION_HUNTER_10,
                        QuestCategory.RECOMMENDED,
                        QuestTitle.of("수집의 즐거움"),
                        "새로운 수집품을 10개 이상 등록하세요.",
                        QuestTarget.of(QuestTargetType.COUNT, 10),
                        QuestReward.of(350, new RewardStats(Map.of("insight", 1, "luck", 1))),
                        QuestRepeatRule.MONTHLY,
                        null
                )
        );

        map.put(
                QuestCode.MEDIA_BINGE_5,
                new QuestBlueprint(
                        QuestCode.MEDIA_BINGE_5,
                        QuestCategory.REPEAT,
                        QuestTitle.of("정주행 마스터"),
                        "보고 있는 작품의 회차를 다섯 번 이상 진행하세요.",
                        QuestTarget.of(QuestTargetType.COUNT, 5),
                        QuestReward.of(220, new RewardStats(Map.of("creativity", 2))),
                        QuestRepeatRule.MONTHLY,
                        null
                )
        );

        map.put(
                QuestCode.INVENTORY_COLLECTOR_100,
                new QuestBlueprint(
                        QuestCode.INVENTORY_COLLECTOR_100,
                        QuestCategory.RECOMMENDED,
                        QuestTitle.of("아이템 정리의 달인"),
                        "새로운 아이템 100개를 획득하세요.",
                        QuestTarget.of(QuestTargetType.COUNT, 100),
                        QuestReward.of(500, new RewardStats(Map.of("dexterity", 2, "luck", 2))),
                        QuestRepeatRule.NONE,
                        Instant.now().plus(365, ChronoUnit.DAYS)
                )
        );

        return map;
    }

    @Override
    public Collection<QuestBlueprint> all() {
        return blueprints.values();
    }

    @Override
    public Optional<QuestBlueprint> find(QuestCode code) {
        return Optional.ofNullable(blueprints.get(code));
    }
}
