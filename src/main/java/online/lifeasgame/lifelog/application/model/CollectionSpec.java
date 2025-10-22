package online.lifeasgame.lifelog.application.model;

import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.domain.CollectionCategory;
import online.lifeasgame.lifelog.domain.CollectionTags;
import online.lifeasgame.lifelog.domain.Quantity;
import online.lifeasgame.lifelog.domain.Title;

import java.util.Set;

public final class CollectionSpec {
    private CollectionSpec() {
    }

    public record Create(
            Long playerId,
            CollectionCategory category,
            Title title,
            Quantity quantity,
            String conditionNote,
            String acquiredFrom,
            CollectionTags tags
    ) {
        public static Create from(Long playerId, CollectionCommand.Create command) {
            Title t = (
                    command.originalTitle() == null ||
                            command.originalTitle().isBlank()
            ) ? Title.of(command.title()) : Title.of(command.title(), command.originalTitle());

            return new Create(
                    playerId,
                    CollectionCategory.parse(command.category()),
                    t,
                    Quantity.of(command.quantity()),
                    command.conditionNote(),
                    command.acquiredFrom(),
                    CollectionTags.of(command.tags() == null ? Set.of() : command.tags())
            );
        }
    }
}
