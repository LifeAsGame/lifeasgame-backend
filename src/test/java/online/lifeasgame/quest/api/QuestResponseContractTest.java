package online.lifeasgame.quest.api;

import jakarta.validation.Validation;
import online.lifeasgame.quest.api.admin.mapper.AdminQuestWebMapper;
import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import online.lifeasgame.quest.api.player.mapper.QuestWebMapper;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Quest Acceptance 응답 계약")
class QuestResponseContractTest {

    @Nested
    @DisplayName("완료된 Acceptance를 응답으로 변환할 때")
    class MapCompletedAcceptance {

        @Test
        @DisplayName("Player와 Admin 응답은 COMPLETED와 새 상태 필드를 반환하고 DONE을 노출하지 않는다")
        void exposesCompletedContract() {
            Instant reachedAt = Instant.parse("2026-07-23T03:00:00Z");
            Instant completedAt = reachedAt.plusSeconds(60);
            Quest quest = quest();
            QuestAcceptance acceptance = QuestAcceptance.start(
                    quest.getId(),
                    1930L,
                    TimePeriod.forever()
            );
            ReflectionTestUtils.setField(acceptance, "id", 19300L);
            acceptance.setProgress(1, quest, reachedAt);
            acceptance.complete(completedAt);
            QuestResult.Acceptance result = QuestResult.Acceptance.from(acceptance, quest);

            QuestResponse.Acceptance playerResponse =
                    QuestWebMapper.toAcceptance(result);
            AdminQuestResponse.Acceptance adminResponse =
                    AdminQuestWebMapper.toAcceptance(result);

            assertThat(playerResponse.status()).isEqualTo(QuestStatus.COMPLETED.name());
            assertThat(playerResponse.status()).isNotEqualTo("DONE");
            assertThat(playerResponse.progressValue()).isEqualTo(1);
            assertThat(playerResponse.completionPolicy())
                    .isEqualTo(QuestCompletionPolicy.USER_CONFIRM.name());
            assertThat(playerResponse.goalReachedAt()).isEqualTo(reachedAt);
            assertThat(playerResponse.completedAt()).isEqualTo(completedAt);

            assertThat(adminResponse.status()).isEqualTo(QuestStatus.COMPLETED.name());
            assertThat(adminResponse.status()).isNotEqualTo("DONE");
            assertThat(adminResponse.progressValue()).isEqualTo(1);
            assertThat(adminResponse.completionPolicy())
                    .isEqualTo(QuestCompletionPolicy.USER_CONFIRM.name());
            assertThat(adminResponse.goalReachedAt()).isEqualTo(reachedAt);
            assertThat(adminResponse.completedAt()).isEqualTo(completedAt);
        }
    }

    @Nested
    @DisplayName("RewardProfile 기반 Definition을 응답으로 변환할 때")
    class MapProfileDefinition {

        @Test
        @DisplayName("Admin과 Player 응답은 version/code를 노출하고 inline reward를 null로 둔다")
        void exposesProfileReferenceWithoutFakeInlineReward() {
            Quest quest = profileQuest();

            AdminQuestResponse.Definition admin =
                    AdminQuestWebMapper.toDefinition(
                            QuestResult.Definition.from(quest)
                    );
            QuestResponse.PlayerQuest player =
                    QuestWebMapper.toPlayerQuest(
                            QuestResult.PlayerQuest.from(quest, null)
                    );

            assertThat(admin.definitionVersion()).isEqualTo(4);
            assertThat(admin.rewardProfileCode()).isEqualTo("RP_EXP_30");
            assertThat(admin.rewardExp()).isNull();
            assertThat(admin.rewardStats()).isNull();
            assertThat(admin.semanticCategory()).isEqualTo("GROWTH");
            assertThat(admin.progressSource()).isEqualTo("COUNT");
            assertThat(admin.repeatPolicy()).isEqualTo("ONCE");
            assertThat(admin.repeatRule()).isEqualTo("ONCE");
            assertThat(admin.roleTemplateCode()).isEqualTo("ROLE_WARRIOR");

            assertThat(player.definitionVersion()).isEqualTo(4);
            assertThat(player.rewardProfileCode()).isEqualTo("RP_EXP_30");
            assertThat(player.rewardExp()).isNull();
            assertThat(player.rewardStats()).isNull();
            assertThat(player.semanticCategory()).isEqualTo("GROWTH");
            assertThat(player.progressSource()).isEqualTo("COUNT");
            assertThat(player.repeatPolicy()).isEqualTo("ONCE");
            assertThat(player.repeatRule()).isEqualTo("ONCE");
            assertThat(player.roleTemplateCode()).isEqualTo("ROLE_WARRIOR");
        }

        @Test
        @DisplayName("legacy 응답은 version 1과 기존 inline reward 값을 유지한다")
        void keepsLegacyInlineRewardResponse() {
            Quest quest = quest();

            AdminQuestResponse.Definition admin =
                    AdminQuestWebMapper.toDefinition(
                            QuestResult.Definition.from(quest)
                    );
            QuestResponse.PlayerQuest player =
                    QuestWebMapper.toPlayerQuest(
                            QuestResult.PlayerQuest.from(quest, null)
                    );

            assertThat(admin.definitionVersion()).isEqualTo(1);
            assertThat(admin.rewardProfileCode()).isNull();
            assertThat(admin.rewardExp()).isZero();
            assertThat(admin.rewardStats()).isEqualTo(Map.of());
            assertThat(admin.semanticCategory()).isNull();
            assertThat(admin.progressSource()).isNull();
            assertThat(admin.repeatPolicy()).isNull();
            assertThat(admin.roleTemplateCode()).isNull();
            assertThat(player.definitionVersion()).isEqualTo(1);
            assertThat(player.rewardProfileCode()).isNull();
            assertThat(player.rewardExp()).isZero();
            assertThat(player.rewardStats()).isEqualTo(Map.of());
            assertThat(player.semanticCategory()).isNull();
            assertThat(player.progressSource()).isNull();
            assertThat(player.repeatPolicy()).isNull();
            assertThat(player.roleTemplateCode()).isNull();
        }

        @Test
        @DisplayName("Admin Update의 blank Role code는 Bean Validation 400 대상이다")
        void rejectsBlankRoleTemplateCode() {
            AdminQuestRequest.Update request = new AdminQuestRequest.Update(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "GROWTH",
                    "COUNT",
                    "ONCE",
                    "   "
            );

            var validator = Validation.buildDefaultValidatorFactory()
                    .getValidator();

            assertThat(validator.validate(request))
                    .extracting(violation ->
                            violation.getPropertyPath().toString())
                    .containsExactly("roleTemplateCode");
        }

        @Test
        @DisplayName("Admin Update의 0 version은 Bean Validation 400 대상이다")
        void rejectsNonPositiveRequestVersion() {
            AdminQuestRequest.Update request = new AdminQuestRequest.Update(
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            var validator = Validation.buildDefaultValidatorFactory()
                    .getValidator();

            assertThat(validator.validate(request))
                    .extracting(violation ->
                            violation.getPropertyPath().toString())
                    .containsExactly("definitionVersion");
        }

        @Test
        @DisplayName("Admin과 Player Blueprint 응답에도 version/code 계약을 노출한다")
        void exposesBlueprintContract() {
            QuestBlueprint blueprint = QuestBlueprint.finalContract(
                    QuestCode.PLAYER_WELCOME,
                    5,
                    QuestCategory.MAIN,
                    QuestSemanticCategory.RECORD,
                    QuestTitle.of("Profile Blueprint"),
                    "profile blueprint",
                    QuestTarget.of(QuestTargetType.COUNT, 1),
                    QuestProgressSource.RECORD_CREATED,
                    RewardProfileRef.of("RP_EXP_10"),
                    QuestRepeatRule.DAILY,
                    null,
                    null,
                    QuestCompletionPolicy.AUTO
            );
            QuestResult.Blueprint result = QuestResult.Blueprint.from(blueprint);

            AdminQuestResponse.Blueprint admin =
                    AdminQuestWebMapper.toBlueprints(List.of(result))
                            .blueprints()
                            .getFirst();
            QuestResponse.Blueprint player = QuestWebMapper.toBlueprint(result);

            assertThat(admin.definitionVersion()).isEqualTo(5);
            assertThat(admin.rewardProfileCode()).isEqualTo("RP_EXP_10");
            assertThat(admin.rewardExp()).isNull();
            assertThat(admin.rewardStats()).isNull();
            assertThat(admin.semanticCategory()).isEqualTo("RECORD");
            assertThat(admin.progressSource()).isEqualTo("RECORD_CREATED");
            assertThat(admin.repeatPolicy()).isEqualTo("DAILY");
            assertThat(admin.roleTemplateCode()).isNull();
            assertThat(player.definitionVersion()).isEqualTo(5);
            assertThat(player.rewardProfileCode()).isEqualTo("RP_EXP_10");
            assertThat(player.rewardExp()).isNull();
            assertThat(player.rewardStats()).isNull();
            assertThat(player.semanticCategory()).isEqualTo("RECORD");
            assertThat(player.progressSource()).isEqualTo("RECORD_CREATED");
            assertThat(player.repeatPolicy()).isEqualTo("DAILY");
            assertThat(player.roleTemplateCode()).isNull();
        }

        @Test
        @DisplayName("final-contract의 nullable legacy category를 Admin/Player 응답에 그대로 노출한다")
        void exposesNullLegacyCategoryForFinalContract() {
            QuestBlueprint blueprint = QuestBlueprint.finalContract(
                    QuestCode.Q_RECORD_FIRST_TRACE,
                    1,
                    QuestSemanticCategory.RECORD,
                    QuestTitle.of("첫 흔적 남기기"),
                    "사용자가 직접 작성한 LifeLog 한 건을 남기면 완료됩니다.",
                    QuestTarget.of(QuestTargetType.COUNT, 1),
                    QuestProgressSource.RECORD_CREATED,
                    RewardProfileRef.of("RP_EXP_TINY_10"),
                    QuestRepeatRule.ONCE,
                    null,
                    null,
                    QuestCompletionPolicy.AUTO
            );
            Quest quest = blueprint.instantiate();

            AdminQuestResponse.Blueprint adminCatalog =
                    AdminQuestWebMapper.toBlueprints(List.of(
                                    QuestResult.Blueprint.from(blueprint)
                            ))
                            .blueprints()
                            .getFirst();
            AdminQuestResponse.Definition adminDefinition =
                    AdminQuestWebMapper.toDefinition(
                            QuestResult.Definition.from(quest)
                    );
            QuestResponse.Blueprint playerCatalog =
                    QuestWebMapper.toBlueprint(
                            QuestResult.Blueprint.from(blueprint)
                    );
            QuestResponse.PlayerQuest playerDetail =
                    QuestWebMapper.toPlayerQuest(
                            QuestResult.PlayerQuest.from(quest, null)
                    );

            assertThat(adminCatalog.category()).isNull();
            assertThat(adminDefinition.category()).isNull();
            assertThat(playerCatalog.category()).isNull();
            assertThat(playerDetail.category()).isNull();
            assertThat(adminDefinition.semanticCategory()).isEqualTo("RECORD");
            assertThat(playerDetail.progressSource())
                    .isEqualTo("RECORD_CREATED");
            assertThat(playerDetail.rewardProfileCode())
                    .isEqualTo("RP_EXP_TINY_10");
            assertThat(playerDetail.roleTemplateCode()).isNull();
        }
    }

    private Quest quest() {
        Quest quest = Quest.create(
                "quest:test:response-contract",
                QuestCategory.MAIN,
                QuestTitle.of("응답 계약 테스트"),
                "Quest 응답 계약 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.USER_CONFIRM,
                null
        );
        ReflectionTestUtils.setField(quest, "id", 193L);
        return quest;
    }

    private Quest profileQuest() {
        Quest quest = Quest.createDefinition(
                "quest:test:profile-response-contract",
                4,
                QuestCategory.MAIN,
                QuestSemanticCategory.GROWTH,
                QuestTitle.of("Profile 응답 계약 테스트"),
                "Quest Profile 응답 계약 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestProgressSource.COUNT,
                RewardProfileRef.of("RP_EXP_30"),
                QuestRepeatRule.ONCE,
                QuestRoleTemplateRef.of("ROLE_WARRIOR"),
                QuestCompletionPolicy.USER_CONFIRM,
                null
        );
        ReflectionTestUtils.setField(quest, "id", 204L);
        return quest;
    }
}
