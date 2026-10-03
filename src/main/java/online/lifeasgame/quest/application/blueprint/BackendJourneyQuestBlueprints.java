package online.lifeasgame.quest.application.blueprint;

import online.lifeasgame.quest.domain.*;
import java.util.List;

final class BackendJourneyQuestBlueprints {
    private BackendJourneyQuestBlueprints() {}
    static List<QuestBlueprint> all() {
        return List.of(
                QuestBlueprint.finalContract(
                        QuestCode.Q_DEV_DEFINE_BACKEND_GOAL, 1, QuestSemanticCategory.ROLE,
                        QuestTitle.of("백엔드 개발 목표 정하기"), "백엔드 개발 목표 정하기",
                        QuestTarget.of(QuestTargetType.COUNT, 1), QuestProgressSource.MANUAL_CHECK,
                        RewardProfileRef.of("RP_NONE"), QuestRepeatRule.ONCE,
                        QuestRoleTemplateRef.of("ROLE_BACKEND_DEVELOPER"), null, QuestCompletionPolicy.USER_CONFIRM),
                QuestBlueprint.finalContract(
                        QuestCode.Q_DEV_RECORD_JAVA_STUDY, 1, QuestSemanticCategory.ROLE,
                        QuestTitle.of("Java 학습 기록 남기기"), "Java 학습 기록 남기기",
                        QuestTarget.of(QuestTargetType.COUNT, 1), QuestProgressSource.RECORD_CREATED,
                        RewardProfileRef.of("RP_EXP_TINY_10"), QuestRepeatRule.ONCE,
                        QuestRoleTemplateRef.of("ROLE_BACKEND_DEVELOPER"), null, QuestCompletionPolicy.USER_CONFIRM),
                QuestBlueprint.finalContract(
                        QuestCode.Q_DEV_BUILD_SPRING_CRUD, 1, QuestSemanticCategory.ROLE,
                        QuestTitle.of("Spring CRUD 기능 완성하기"), "Spring CRUD 기능 완성하기",
                        QuestTarget.of(QuestTargetType.COUNT, 1), QuestProgressSource.RECORD_CREATED,
                        RewardProfileRef.of("RP_EXP_TINY_10"), QuestRepeatRule.ONCE,
                        QuestRoleTemplateRef.of("ROLE_BACKEND_DEVELOPER"), null, QuestCompletionPolicy.USER_CONFIRM),
                QuestBlueprint.finalContract(
                        QuestCode.Q_DEV_MODEL_DATABASE, 1, QuestSemanticCategory.ROLE,
                        QuestTitle.of("데이터 모델 작성하기"), "데이터 모델 작성하기",
                        QuestTarget.of(QuestTargetType.COUNT, 1), QuestProgressSource.RECORD_CREATED,
                        RewardProfileRef.of("RP_EXP_TINY_10"), QuestRepeatRule.ONCE,
                        QuestRoleTemplateRef.of("ROLE_BACKEND_DEVELOPER"), null, QuestCompletionPolicy.USER_CONFIRM),
                QuestBlueprint.finalContract(
                        QuestCode.Q_DEV_WRITE_DOMAIN_TEST, 1, QuestSemanticCategory.ROLE,
                        QuestTitle.of("도메인 테스트 작성하기"), "도메인 테스트 작성하기",
                        QuestTarget.of(QuestTargetType.COUNT, 1), QuestProgressSource.RECORD_CREATED,
                        RewardProfileRef.of("RP_EXP_TINY_10"), QuestRepeatRule.ONCE,
                        QuestRoleTemplateRef.of("ROLE_BACKEND_DEVELOPER"), null, QuestCompletionPolicy.USER_CONFIRM),
                QuestBlueprint.finalContract(
                        QuestCode.Q_DEV_DEPLOY_SERVICE, 1, QuestSemanticCategory.ROLE,
                        QuestTitle.of("서비스 배포하기"), "서비스 배포하기",
                        QuestTarget.of(QuestTargetType.COUNT, 1), QuestProgressSource.RECORD_CREATED,
                        RewardProfileRef.of("RP_EXP_AND_ITEM_FIRST_STEP_20"), QuestRepeatRule.ONCE,
                        QuestRoleTemplateRef.of("ROLE_BACKEND_DEVELOPER"), null, QuestCompletionPolicy.USER_CONFIRM),
                QuestBlueprint.finalContract(
                        QuestCode.Q_DEV_POLISH_README, 1, QuestSemanticCategory.ROLE,
                        QuestTitle.of("README 정리하기"), "README 정리하기",
                        QuestTarget.of(QuestTargetType.COUNT, 1), QuestProgressSource.RECORD_CREATED,
                        RewardProfileRef.of("RP_EXP_TINY_10"), QuestRepeatRule.ONCE,
                        QuestRoleTemplateRef.of("ROLE_BACKEND_DEVELOPER"), null, QuestCompletionPolicy.USER_CONFIRM)
        );
    }
}
