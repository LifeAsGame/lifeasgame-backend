package online.lifeasgame.quest.application;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.automation.QuestSignalProcessingAttempt;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.api.player.QuestRouteController;
import online.lifeasgame.quest.api.player.request.QuestRouteRequest;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.quest.application.query.QuestQuery;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.reward.application.event.QuestRewardReadyBridge;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestArchitectureAlignmentTest {

    @Test
    void simpleFacadeAndMisleadingOrchestrationNamesAreRemoved() {
        for (String className : Set.of(
                "online.lifeasgame.quest.application.QuestFacade",
                "online.lifeasgame.quest.application.QuestRouteFacade",
                "online.lifeasgame.quest.application.automation.QuestAutomationService",
                "online.lifeasgame.quest.application.saga.QuestRewardSaga"
        )) {
            assertThatThrownBy(() -> Class.forName(className))
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Test
    void selfIdentityLivesInApplicationUseCases() {
        for (Class<?> type : List.of(
                QuestService.class,
                QuestQueryService.class,
                QuestManualCheckService.class,
                QuestRouteSelectService.class,
                QuestRouteAdvanceService.class,
                QuestRouteQueryService.class
        )) {
            assertThat(fieldTypes(type)).contains(CurrentPlayerAccessor.class);
        }
        assertThat(fieldTypes(QuestRouteController.class))
                .doesNotContain(CurrentPlayerAccessor.class);
        assertThat(Arrays.stream(
                        QuestRouteRequest.Advance.class.getRecordComponents()
                ).map(component -> component.getName()))
                .containsExactly("expectedStepId");
        assertThat(QuestRouteController.class
                .getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/v1/quest-routes");
    }

    @Test
    void queryTypesAndReadOnlyServiceAreSeparatedFromCommands() {
        assertThat(Arrays.stream(QuestCommand.class.getDeclaredClasses())
                .map(Class::getSimpleName))
                .doesNotContain(
                        "Definition",
                        "Acceptances",
                        "Acceptance",
                        "PlayerQuests",
                        "PlayerQuest"
                );
        assertThat(Arrays.stream(QuestQuery.class.getDeclaredClasses())
                .map(Class::getSimpleName))
                .containsExactlyInAnyOrder(
                        "Definition",
                        "Acceptances",
                        "Acceptance",
                        "PlayerQuests",
                        "PlayerQuest"
                );
        Transactional transactional =
                QuestQueryService.class.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    @Test
    void writerIsPersistenceOnlyAndProvisionerOwnsDefinitionEvents() {
        assertThat(fieldTypes(QuestWriter.class))
                .doesNotContain(DomainEventPublisher.class);
        Transactional writerTransaction =
                QuestWriter.class.getAnnotation(Transactional.class);
        Transactional provisionerTransaction =
                QuestDefinitionProvisioner.class
                        .getAnnotation(Transactional.class);
        assertThat(writerTransaction.propagation())
                .isEqualTo(Propagation.MANDATORY);
        assertThat(provisionerTransaction.propagation())
                .isEqualTo(Propagation.MANDATORY);
    }

    @Test
    void rewardConsumesOnlyQuestOwnedTypedFact() {
        assertThat(Arrays.stream(
                        QuestRewardReadyBridge.class.getDeclaredMethods()
                ).map(Method::getParameterTypes)
                .flatMap(Arrays::stream))
                .contains(QuestRewardReadyFact.class)
                .doesNotContain(QuestEvent.class);
    }

    @Test
    void durableSignalAttemptKeepsRequiresNewBoundary() throws Exception {
        Method process = QuestSignalProcessingAttempt.class.getDeclaredMethod(
                "process",
                online.lifeasgame.quest.application.automation.QuestSignal.class,
                String.class
        );
        assertThat(process.getAnnotation(Transactional.class).propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    private static Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(field -> field.getType())
                .collect(java.util.stream.Collectors.toSet());
    }
}
