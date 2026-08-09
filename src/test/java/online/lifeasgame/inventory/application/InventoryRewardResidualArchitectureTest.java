package online.lifeasgame.inventory.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.reward.application.*;
import online.lifeasgame.reward.application.event.QuestRewardReadyBridge;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryRewardResidualArchitectureTest {

    @Test
    void simpleFacadesAndQueryForwardersAreRemoved() {
        for (String className : Set.of(
                "online.lifeasgame.inventory.application.InventoryFacade",
                "online.lifeasgame.inventory.application.MailboxFacade",
                "online.lifeasgame.inventory.application.InventoryQueryReader",
                "online.lifeasgame.inventory.application.MailBoxQueryReader",
                "online.lifeasgame.inventory.application.InventoryWriter"
        )) {
            assertThatThrownBy(() -> Class.forName(className))
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Test
    void selfIdentityLivesInInventoryAndMailboxUseCases() {
        for (Class<?> type : List.of(
                InventoryService.class,
                InventoryQueryService.class,
                MailboxService.class,
                MailboxQueryService.class
        )) {
            assertThat(fieldTypes(type)).contains(CurrentPlayerAccessor.class);
        }
    }

    @Test
    void queryServicesAreReadOnlyAndItemCommandsContainNoReads() {
        for (Class<?> type : List.of(
                InventoryQueryService.class,
                MailboxQueryService.class,
                ItemQueryService.class
        )) {
            Transactional transactional =
                    type.getAnnotation(Transactional.class);
            assertThat(transactional).isNotNull();
            assertThat(transactional.readOnly()).isTrue();
        }

        assertThat(methodNames(ItemService.class))
                .containsExactlyInAnyOrder("create", "update", "delete");
        assertThat(methodNames(ItemQueryService.class))
                .containsExactlyInAnyOrder("getItem", "search");
    }

    @Test
    void rewardAttemptAndFailureTransactionsRemainIsolated()
            throws Exception {
        assertRequiresNew(RewardSettlementCreateAttempt.class, "create");
        assertRequiresNew(
                RewardSettlementExpProcessAttempt.class,
                "process"
        );
        assertRequiresNew(
                RewardSettlementItemProcessAttempt.class,
                "process"
        );
        assertRequiresNew(
                RewardSettlementLineFailureRecorder.class,
                "record"
        );
        assertRequiresNew(
                RewardSettlementLineRetryPreparationService.class,
                "prepare"
        );
    }

    @Test
    void rewardBridgeConsumesTheQuestOwnedTypedFact() {
        assertThat(Arrays.stream(
                        QuestRewardReadyBridge.class.getDeclaredMethods()
                ).flatMap(method -> Arrays.stream(method.getParameterTypes())))
                .contains(QuestRewardReadyFact.class);
    }

    private static Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(field -> field.getType())
                .collect(java.util.stream.Collectors.toSet());
    }

    private static Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .map(Method::getName)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static void assertRequiresNew(
            Class<?> type,
            String methodName
    ) {
        Method method = Arrays.stream(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        assertThat(method.getAnnotation(Transactional.class).propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }
}
