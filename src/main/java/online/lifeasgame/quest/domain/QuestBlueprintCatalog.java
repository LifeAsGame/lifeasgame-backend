package online.lifeasgame.quest.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;

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
                new DomainException(QuestError.QUEST_NOT_FOUND));
    }
}
