package online.lifeasgame.quest.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.reward.application.internal.RewardProfileLookupApi;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Quest Definition Application 계약")
class QuestDefinitionServiceTest {

    @Mock
    private QuestBlueprintCatalog blueprintCatalog;

    @Mock
    private QuestReader questReader;

    @Mock
    private QuestWriter questWriter;

    @Mock
    private RewardProfileLookupApi rewardProfileLookupApi;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private QuestService service;

    @BeforeEach
    void setUp() {
        service = new QuestService(
                blueprintCatalog,
                questReader,
                questWriter,
                rewardProfileLookupApi,
                domainEventPublisher
        );
    }

    @Nested
    @DisplayName("Admin이 RewardProfile 참조를 수정할 때")
    class UpdateRewardProfile {

        @Test
        @DisplayName("active profile code만 저장하고 신규 응답의 inline reward는 null이다")
        void updatesWithActiveProfile() {
            Quest quest = legacyQuest();
            stubExisting(quest);
            given(rewardProfileLookupApi.getActiveByCode("RP_EXP_30"))
                    .willReturn(reference("RP_EXP_30"));

            QuestResult.Definition result = service.updateDefinition(
                    update(2, "  RP_EXP_30  ", null, null)
            );

            assertThat(result.definitionVersion()).isEqualTo(2);
            assertThat(result.rewardProfileCode()).isEqualTo("RP_EXP_30");
            assertThat(result.rewardExp()).isNull();
            assertThat(result.rewardStats()).isNull();
            verify(rewardProfileLookupApi).getActiveByCode("RP_EXP_30");
            verify(domainEventPublisher).publishAll(any());
        }

        @Test
        @DisplayName("missing profile 오류를 번역하지 않고 Quest mutation을 남기지 않는다")
        void keepsMissingProfileError() {
            Quest quest = legacyQuest();
            stubExisting(quest);
            given(rewardProfileLookupApi.getActiveByCode("UNKNOWN"))
                    .willThrow(new DomainException(RewardError.REWARD_PROFILE_NOT_FOUND));

            assertRewardError(
                    () -> service.updateDefinition(
                            update(2, "UNKNOWN", null, null)
                    ),
                    RewardError.REWARD_PROFILE_NOT_FOUND
            );

            assertUnchangedLegacy(quest);
            verifyNoInteractions(domainEventPublisher);
        }

        @Test
        @DisplayName("inactive profile 오류를 번역하지 않고 Quest mutation을 남기지 않는다")
        void keepsInactiveProfileError() {
            Quest quest = legacyQuest();
            stubExisting(quest);
            given(rewardProfileLookupApi.getActiveByCode("RP_INACTIVE"))
                    .willThrow(new DomainException(RewardError.REWARD_PROFILE_INACTIVE));

            assertRewardError(
                    () -> service.updateDefinition(
                            update(2, "RP_INACTIVE", null, null)
                    ),
                    RewardError.REWARD_PROFILE_INACTIVE
            );

            assertUnchangedLegacy(quest);
            verifyNoInteractions(domainEventPublisher);
        }

        @Test
        @DisplayName("profile code와 inline reward를 한 요청에서 변경하면 400 계약으로 거부한다")
        void rejectsMixedRewardContract() {
            Quest quest = legacyQuest();
            stubExisting(quest);

            assertQuestError(
                    () -> service.updateDefinition(
                            update(2, "RP_EXP_10", 10, Map.of())
                    ),
                    QuestError.QUEST_REWARD_CONTRACT_CONFLICT
            );

            assertUnchangedLegacy(quest);
            verifyNoInteractions(rewardProfileLookupApi, domainEventPublisher);
        }

        @Test
        @DisplayName("동일 version과 profile code는 lookup과 Event가 없는 no-op이다")
        void doesNotPublishNoOp() {
            Quest quest = profileQuest(2, "RP_EXP_10");
            stubExisting(quest);

            QuestResult.Definition result = service.updateDefinition(
                    update(2, " RP_EXP_10 ", null, null)
            );

            assertThat(result.definitionVersion()).isEqualTo(2);
            assertThat(result.rewardProfileCode()).isEqualTo("RP_EXP_10");
            verifyNoInteractions(rewardProfileLookupApi, domainEventPublisher);
        }
    }

    @Test
    @DisplayName("legacy inline reward partial update는 기존 동작을 유지한다")
    void updatesLegacyInlineReward() {
        Quest quest = legacyQuest();
        stubExisting(quest);

        QuestResult.Definition result = service.updateDefinition(
                update(null, null, 50, Map.of("strength", 1))
        );

        assertThat(result.definitionVersion()).isEqualTo(1);
        assertThat(result.rewardProfileCode()).isNull();
        assertThat(result.rewardExp()).isEqualTo(50);
        assertThat(result.rewardStats()).containsEntry("strength", 1);
        verifyNoInteractions(rewardProfileLookupApi);
        verify(domainEventPublisher).publishAll(any());
    }

    @Test
    @DisplayName("신규 계약 Blueprint materialization 전에 active profile을 조회한다")
    void validatesProfileBlueprintBeforeMaterialization() {
        QuestBlueprint blueprint = QuestBlueprint.profileBased(
                QuestCode.PLAYER_WELCOME,
                4,
                QuestCategory.MAIN,
                QuestTitle.of("Profile Blueprint"),
                "profile blueprint",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                RewardProfileRef.of("RP_EXP_30"),
                QuestRepeatRule.NONE,
                null,
                QuestCompletionPolicy.AUTO
        );
        given(questReader.findByCode(QuestCode.PLAYER_WELCOME))
                .willReturn(Optional.empty());
        given(blueprintCatalog.require(QuestCode.PLAYER_WELCOME))
                .willReturn(blueprint);
        given(rewardProfileLookupApi.getActiveByCode("RP_EXP_30"))
                .willReturn(reference("RP_EXP_30"));
        given(questWriter.create(any())).willAnswer(
                invocation -> invocation.getArgument(0)
        );

        Quest result = service.ensureQuest(QuestCode.PLAYER_WELCOME);

        assertThat(result.getDefinitionVersion()).isEqualTo(4);
        assertThat(result.rewardProfileCodeOrNull()).isEqualTo("RP_EXP_30");
        verify(rewardProfileLookupApi).getActiveByCode("RP_EXP_30");
        verify(questWriter).create(any());
    }

    private void stubExisting(Quest quest) {
        given(questReader.findByCode(QuestCode.PLAYER_WELCOME))
                .willReturn(Optional.of(quest));
    }

    private QuestCommand.UpdateDefinition update(
            Integer definitionVersion,
            String rewardProfileCode,
            Integer rewardExp,
            Map<String, Integer> rewardStats
    ) {
        return new QuestCommand.UpdateDefinition(
                QuestCode.PLAYER_WELCOME.name(),
                definitionVersion,
                null,
                null,
                rewardProfileCode,
                rewardExp,
                rewardStats,
                null,
                null
        );
    }

    private RewardProfileLookupApi.RewardProfileReference reference(String code) {
        return new RewardProfileLookupApi.RewardProfileReference(code);
    }

    private Quest legacyQuest() {
        return Quest.create(
                QuestCode.PLAYER_WELCOME.value(),
                QuestCategory.MAIN,
                QuestTitle.of("Legacy Definition"),
                "legacy",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.AUTO,
                null
        );
    }

    private Quest profileQuest(int version, String profileCode) {
        return Quest.createDefinition(
                QuestCode.PLAYER_WELCOME.value(),
                version,
                QuestCategory.MAIN,
                QuestTitle.of("Profile Definition"),
                "profile",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                RewardProfileRef.of(profileCode),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.AUTO,
                null
        );
    }

    private void assertUnchangedLegacy(Quest quest) {
        assertThat(quest.getDefinitionVersion()).isEqualTo(1);
        assertThat(quest.rewardProfileCodeOrNull()).isNull();
        assertThat(quest.getReward().exp()).isZero();
        assertThat(quest.pullEvents()).isEmpty();
    }

    private void assertQuestError(Runnable action, QuestError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
