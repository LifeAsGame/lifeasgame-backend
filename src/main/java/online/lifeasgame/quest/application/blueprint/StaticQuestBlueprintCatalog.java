package online.lifeasgame.quest.application.blueprint;

import online.lifeasgame.quest.domain.QuestBlueprint;
import online.lifeasgame.quest.domain.QuestBlueprintCatalog;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.seed.SeedLevel1Quest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StaticQuestBlueprintCatalog implements QuestBlueprintCatalog {

    private final List<QuestBlueprint> orderedBlueprints;
    private final Map<QuestCode, QuestBlueprint> blueprintsByCode;

    public StaticQuestBlueprintCatalog() {
        orderedBlueprints = SeedLevel1Quest.definitions().stream()
                .map(SeedLevel1QuestBlueprintAdapter::toBlueprint)
                .toList();

        EnumMap<QuestCode, QuestBlueprint> byCode = new EnumMap<>(QuestCode.class);
        orderedBlueprints.forEach(blueprint -> byCode.put(blueprint.code(), blueprint));
        blueprintsByCode = Collections.unmodifiableMap(byCode);
    }

    @Override
    public Collection<QuestBlueprint> all() {
        return orderedBlueprints;
    }

    @Override
    public Optional<QuestBlueprint> find(QuestCode code) {
        return Optional.ofNullable(blueprintsByCode.get(code));
    }
}
