package online.lifeasgame.lifelog.quick.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.quick.domain.error.QuickRecordError;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class QuickRecordCommand {

    private QuickRecordCommand() {
    }

    public record Create(
            String type,
            String lifeLogSubtype,
            String reflectionScope,
            CollectionCommand.Create collection,
            ExerciseCommand.Create exercise,
            MediaLogCommand.Create media
    ) {
        public Create(
                String type,
                CollectionCommand.Create collection,
                ExerciseCommand.Create exercise,
                MediaLogCommand.Create media
        ) {
            this(type, null, null, collection, exercise, media);
        }

        public Create {
            collection = copy(collection);
            exercise = copy(exercise);
            media = copy(media);
        }

        public Selected selected() {
            LifeLogType selectedType = parseType(type);
            int payloadCount = (collection == null ? 0 : 1)
                    + (exercise == null ? 0 : 1)
                    + (media == null ? 0 : 1);
            if (payloadCount != 1
                    || selectedType == LifeLogType.COLLECTION
                    && collection == null
                    || selectedType == LifeLogType.EXERCISE
                    && exercise == null
                    || selectedType == LifeLogType.MEDIA
                    && media == null) {
                throw invalid();
            }
            LifeLogRecordMetadataCommand nestedMetadata =
                    switch (selectedType) {
                        case COLLECTION -> collection.lifeLogMetadata();
                        case EXERCISE -> exercise.lifeLogMetadata();
                        case MEDIA -> media.lifeLogMetadata();
                    };
            if (nestedMetadata != null && nestedMetadata.isPresent()) {
                throw invalid();
            }
            return new Selected(
                    selectedType,
                    new LifeLogRecordMetadataCommand(
                            lifeLogSubtype,
                            reflectionScope
                    ),
                    collection,
                    exercise,
                    media
            );
        }
    }

    public record Selected(
            LifeLogType type,
            LifeLogRecordMetadataCommand lifeLogMetadata,
            CollectionCommand.Create collection,
            ExerciseCommand.Create exercise,
            MediaLogCommand.Create media
    ) {
    }

    private static LifeLogType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalid();
        }
        try {
            return LifeLogType.valueOf(
                    raw.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new DomainException(
                    QuickRecordError.INVALID_REQUEST,
                    null,
                    exception
            );
        }
    }

    private static CollectionCommand.Create copy(
            CollectionCommand.Create value
    ) {
        if (value == null) {
            return null;
        }
        return new CollectionCommand.Create(
                value.category(),
                value.title(),
                value.originalTitle(),
                value.quantity(),
                value.conditionNote(),
                value.acquiredFrom(),
                copyTags(value.tags()),
                value.lifeLogMetadata()
        );
    }

    private static ExerciseCommand.Create copy(
            ExerciseCommand.Create value
    ) {
        if (value == null) {
            return null;
        }
        return new ExerciseCommand.Create(
                value.category(),
                value.durationMinutes(),
                value.distanceKm(),
                value.calories(),
                value.exercisedOn(),
                value.memo(),
                value.lifeLogMetadata()
        );
    }

    private static MediaLogCommand.Create copy(
            MediaLogCommand.Create value
    ) {
        if (value == null) {
            return null;
        }
        return new MediaLogCommand.Create(
                value.category(),
                value.title(),
                value.originalTitle(),
                value.currentEpisode(),
                value.totalEpisode(),
                value.status(),
                copyTags(value.tags()),
                value.lifeLogMetadata()
        );
    }

    private static Set<String> copyTags(Set<String> values) {
        if (values == null) {
            return null;
        }
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(values)
        );
    }

    private static DomainException invalid() {
        return new DomainException(QuickRecordError.INVALID_REQUEST);
    }
}
