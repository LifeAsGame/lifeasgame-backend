package online.lifeasgame.quest.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public interface QuestBlueprintCatalog {
    Optional<QuestBlueprint> find(QuestCode code);

    default Collection<QuestBlueprint> all() {
        return Collections.emptyList();
    }

    default QuestBlueprint require(QuestCode code) {
        return find(code).orElseThrow(() ->
                new IllegalArgumentException("Quest blueprint not found for code " + code.name()));
    }
}
