package online.lifeasgame.quest.application.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.quest.application.QuestDefinitionProvisioner;
import online.lifeasgame.quest.domain.QuestBlueprint;
import online.lifeasgame.quest.domain.QuestBlueprintCatalog;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "app.quest.definition-bootstrap",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class QuestDefinitionBootstrapper implements ApplicationRunner {

    private final QuestDefinitionProvisioner definitionProvisioner;
    private final QuestBlueprintCatalog questBlueprintCatalog;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        questBlueprintCatalog.all().forEach(this::ensureQuest);
    }

    private void ensureQuest(QuestBlueprint blueprint) {
        definitionProvisioner.ensure(blueprint.code());
        log.debug("Quest definition ensured for {}", blueprint.code().name());
    }
}
