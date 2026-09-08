package online.lifeasgame.quest.application.blueprint;

import online.lifeasgame.quest.domain.QuestBlueprint;
import online.lifeasgame.quest.domain.QuestBlueprintCatalog;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.seed.SeedLevel1Quest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
public class StaticQuestBlueprintCatalog implements QuestBlueprintCatalog {

    private final Map<QuestCode, QuestBlueprint> blueprints =
            Collections.unmodifiableMap(initialize());

    private static Map<QuestCode, QuestBlueprint> initialize() {
        EnumMap<QuestCode, QuestBlueprint> map = new EnumMap<>(QuestCode.class);
        SeedLevel1Quest.definitions().forEach(definition ->
                map.put(
                        definition.questCode(),
                        SeedLevel1QuestBlueprintAdapter.toBlueprint(definition)
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
