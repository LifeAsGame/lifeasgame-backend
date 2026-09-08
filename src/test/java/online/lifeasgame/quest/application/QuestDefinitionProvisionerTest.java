package online.lifeasgame.quest.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.blueprint.StaticQuestBlueprintCatalog;
import online.lifeasgame.quest.application.event.QuestDefinitionEventFactory;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.reward.application.internal.RewardProfileLookupApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("QuestDefinitionProvisioner 승인 catalog 경계")
class QuestDefinitionProvisionerTest {

    @Test
    @DisplayName("persisted legacy code는 승인 catalog 밖이어도 그대로 반환한다")
    void resolvesPersistedLegacyDefinition() {
        QuestReader reader = mock(QuestReader.class);
        QuestWriter writer = mock(QuestWriter.class);
        Quest persisted = mock(Quest.class);
        given(reader.findByCode(QuestCode.PLAYER_WELCOME))
                .willReturn(Optional.of(persisted));
        QuestDefinitionProvisioner provisioner = new QuestDefinitionProvisioner(
                new StaticQuestBlueprintCatalog(),
                reader,
                writer,
                mock(RewardProfileLookupApi.class),
                mock(DomainEventPublisher.class),
                mock(QuestDefinitionEventFactory.class),
                Clock.systemUTC()
        );

        assertThat(provisioner.resolve(QuestCode.PLAYER_WELCOME))
                .contains(persisted);

        verifyNoInteractions(writer);
    }

    @Test
    @DisplayName("persisted definition이 없는 legacy code는 새로 만들지 않는다")
    void rejectsMissingLegacyDefinition() {
        QuestReader reader = mock(QuestReader.class);
        QuestWriter writer = mock(QuestWriter.class);
        given(reader.findByCode(QuestCode.PLAYER_WELCOME))
                .willReturn(Optional.empty());
        QuestDefinitionProvisioner provisioner = new QuestDefinitionProvisioner(
                new StaticQuestBlueprintCatalog(),
                reader,
                writer,
                mock(RewardProfileLookupApi.class),
                mock(DomainEventPublisher.class),
                mock(QuestDefinitionEventFactory.class),
                Clock.systemUTC()
        );

        assertThatThrownBy(() -> provisioner.ensure(QuestCode.PLAYER_WELCOME))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(QuestError.QUEST_NOT_FOUND)
                );

        verifyNoInteractions(writer);
    }
}
