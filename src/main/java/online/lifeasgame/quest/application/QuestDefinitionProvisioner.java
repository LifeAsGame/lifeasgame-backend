package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.event.QuestDefinitionEventFactory;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestBlueprint;
import online.lifeasgame.quest.domain.QuestBlueprintCatalog;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.reward.application.internal.RewardProfileLookupApi;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class QuestDefinitionProvisioner {

    private final QuestBlueprintCatalog questBlueprintCatalog;
    private final QuestReader questReader;
    private final QuestWriter questWriter;
    private final RewardProfileLookupApi rewardProfileLookupApi;
    private final DomainEventPublisher domainEventPublisher;
    private final QuestDefinitionEventFactory eventFactory;
    private final Clock clock;

    public Quest ensure(QuestCode code) {
        return questReader.findByCode(code)
                .orElseGet(() -> materialize(
                        questBlueprintCatalog.require(code)
                ));
    }

    private Quest materialize(QuestBlueprint blueprint) {
        if (blueprint.usesRewardProfile()) {
            rewardProfileLookupApi.getActiveByCode(
                    blueprint.rewardProfileCodeOrNull()
            );
        }
        Quest saved = questWriter.create(blueprint.instantiate());
        domainEventPublisher.publish(eventFactory.created(
                saved,
                clock.instant()
        ));
        return saved;
    }
}
